
package br.com.numberone.inventory.domain.model;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record ResultadoConsumoEstoque(
        UUID operationId,
        StatusOperacaoConsumo status,
        List<UUID> movements,
        UUID compensationId
) {

    public ResultadoConsumoEstoque {
        Objects.requireNonNull(
                operationId,
                "operationId obrigatorio"
        );

        Objects.requireNonNull(
                status,
                "status obrigatorio"
        );
        movements = List.copyOf(movements);
    }

    public ResultadoConsumoEstoque(UUID operationId, StatusOperacaoConsumo status) {
        this(operationId, status, List.of());
    }

    public ResultadoConsumoEstoque(UUID operationId, StatusOperacaoConsumo status, List<UUID> movements) {
        this(operationId, status, movements, null);
    }

    public static ResultadoConsumoEstoque from(
            OperacaoConsumoEstoque operacao) {

        Objects.requireNonNull(
                operacao,
                "operacao obrigatoria"
        );

        return new ResultadoConsumoEstoque(
                operacao.getOperationId(),
                operacao.getStatus(), List.of(), operacao.getCompensationId()
        );
    }
}
