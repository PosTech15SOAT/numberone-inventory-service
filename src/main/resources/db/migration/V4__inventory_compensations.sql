ALTER TABLE operacao_consumo_estoque
    ALTER COLUMN ordem_servico_id DROP NOT NULL,
    ALTER COLUMN item_ordem_servico_id DROP NOT NULL,
    ALTER COLUMN usuario_responsavel_id DROP NOT NULL,
    ALTER COLUMN request_hash DROP NOT NULL,
    DROP CONSTRAINT operacao_consumo_estoque_status_check,
    ADD COLUMN compensation_id UUID,
    ADD CONSTRAINT ck_operacao_estado CHECK (
        (status = 'CLOSED' AND ordem_servico_id IS NULL AND item_ordem_servico_id IS NULL
            AND usuario_responsavel_id IS NULL AND request_hash IS NULL AND compensation_id IS NOT NULL)
        OR
        (status IN ('PROCESSING', 'CONSUMED', 'INSUFFICIENT_STOCK', 'REVERSED')
            AND ordem_servico_id IS NOT NULL AND item_ordem_servico_id IS NOT NULL
            AND usuario_responsavel_id IS NOT NULL AND request_hash IS NOT NULL
            AND ((status = 'REVERSED') = (compensation_id IS NOT NULL)))
    );

CREATE TABLE compensacao_estoque (
    compensation_id UUID PRIMARY KEY,
    operation_id UUID NOT NULL REFERENCES operacao_consumo_estoque(operation_id),
    request_hash VARCHAR(64) NOT NULL CHECK (request_hash ~ '^[0-9a-f]{64}$'),
    request JSONB NOT NULL,
    result JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (operation_id, compensation_id)
);

ALTER TABLE operacao_consumo_estoque ADD CONSTRAINT fk_operacao_compensacao
    FOREIGN KEY (operation_id, compensation_id) REFERENCES compensacao_estoque(operation_id, compensation_id)
    DEFERRABLE INITIALLY DEFERRED;

ALTER TABLE movimentacao_estoque
    DROP CONSTRAINT uq_movimentacao_operacao_item,
    ADD COLUMN original_movement_id UUID UNIQUE REFERENCES movimentacao_estoque(id),
    ADD COLUMN compensation_id UUID,
    ADD CONSTRAINT fk_movimento_compensacao FOREIGN KEY (operation_id, compensation_id)
        REFERENCES compensacao_estoque(operation_id, compensation_id) DEFERRABLE INITIALLY DEFERRED,
    ADD CONSTRAINT ck_movimento_inverso CHECK (
        (original_movement_id IS NULL AND compensation_id IS NULL)
        OR (original_movement_id IS NOT NULL AND compensation_id IS NOT NULL AND operation_id IS NOT NULL
            AND tipo_movimentacao = 'ENTRADA' AND origem_movimentacao = 'ORDEM_SERVICO'
            AND quantidade_depois > quantidade_antes)
    );

CREATE UNIQUE INDEX uq_baixa_operacao_item ON movimentacao_estoque(operation_id, id_item_estoque)
    WHERE original_movement_id IS NULL;
CREATE UNIQUE INDEX uq_retorno_operacao_item ON movimentacao_estoque(operation_id, id_item_estoque)
    WHERE original_movement_id IS NOT NULL;
