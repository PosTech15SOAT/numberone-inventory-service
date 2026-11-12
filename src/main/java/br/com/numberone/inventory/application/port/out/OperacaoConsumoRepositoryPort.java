
package br.com.numberone.inventory.application.port.out;

import br.com.numberone.inventory.domain.model.OperacaoConsumoEstoque;

import java.util.Optional;
import java.util.UUID;

public interface OperacaoConsumoRepositoryPort {
    void bloquearOperacao(UUID operationId);


    Optional<OperacaoConsumoEstoque> buscarPorOperationId(
            UUID operationId
    );

    OperacaoConsumoEstoque salvar(
            OperacaoConsumoEstoque operacao
    );
}
