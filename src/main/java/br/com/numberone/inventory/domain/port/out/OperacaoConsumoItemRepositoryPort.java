
package br.com.numberone.inventory.domain.port.out;

import br.com.numberone.inventory.domain.model.SolicitacaoConsumoEstoque;

import java.util.List;
import java.util.UUID;

public interface OperacaoConsumoItemRepositoryPort {

    void salvarItens(
            UUID operationId,
            List<SolicitacaoConsumoEstoque.ItemConsumo> items
    );

    List<SolicitacaoConsumoEstoque.ItemConsumo> buscarItens(
            UUID operationId
    );
}
