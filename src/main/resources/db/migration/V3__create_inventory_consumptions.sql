CREATE TABLE operacao_consumo_estoque (
    operation_id UUID PRIMARY KEY,
    ordem_servico_id UUID NOT NULL,
    item_ordem_servico_id UUID NOT NULL,
    usuario_responsavel_id UUID NOT NULL,
    request_hash VARCHAR(64) NOT NULL CHECK (request_hash ~ '^[0-9a-f]{64}$'),
    status VARCHAR(30) NOT NULL CHECK (status IN ('PROCESSING', 'CONSUMED', 'INSUFFICIENT_STOCK')),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE operacao_consumo_item (
    id UUID PRIMARY KEY,
    operation_id UUID NOT NULL REFERENCES operacao_consumo_estoque(operation_id),
    item_estoque_id UUID NOT NULL REFERENCES item_estoque(id),
    quantidade INTEGER NOT NULL CHECK (quantidade > 0),
    CONSTRAINT uq_operacao_consumo_item UNIQUE (operation_id, item_estoque_id)
);

ALTER TABLE movimentacao_estoque
    ADD COLUMN operation_id UUID REFERENCES operacao_consumo_estoque(operation_id),
    ADD CONSTRAINT uq_movimentacao_operacao_item UNIQUE (operation_id, id_item_estoque);
