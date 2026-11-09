
package br.com.numberone.inventory.domain.port.out;

import br.com.numberone.inventory.domain.model.ItemEstoque;

import java.util.Optional;
import java.util.UUID;

public interface ItemEstoqueRepositoryPort {

    Optional<ItemEstoque> buscarPorId(UUID id);

    Optional<ItemEstoque> buscarPorCodigo(String codigo);

    Optional<ItemEstoque> buscarPorIdParaAtualizacao(UUID id);

    ItemEstoque salvar(ItemEstoque item);
}
