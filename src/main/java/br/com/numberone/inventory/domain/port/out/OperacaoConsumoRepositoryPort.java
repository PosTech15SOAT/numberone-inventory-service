
package br.com.numberone.inventory.domain.port.out;

import br.com.numberone.inventory.domain.model.OperacaoConsumoEstoque;

import java.util.Optional;
import java.util.UUID;

public interface OperacaoConsumoRepositoryPort {

    Optional<OperacaoConsumoEstoque> buscarPorOperationId(
            UUID operationId
    );

    OperacaoConsumoEstoque salvar(
            OperacaoConsumoEstoque operacao
    );
}
