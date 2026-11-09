
package br.com.numberone.inventory.domain.model;

import java.util.Objects;
import java.util.UUID;

public record ResultadoConsumoEstoque(
        UUID operationId,
        StatusOperacaoConsumo status
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
    }

    public static ResultadoConsumoEstoque from(
            OperacaoConsumoEstoque operacao) {

        Objects.requireNonNull(
                operacao,
                "operacao obrigatoria"
        );

        return new ResultadoConsumoEstoque(
                operacao.getOperationId(),
                operacao.getStatus()
        );
    }
}
