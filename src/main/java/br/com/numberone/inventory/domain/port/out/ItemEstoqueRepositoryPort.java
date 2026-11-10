
package br.com.numberone.inventory.domain.port.out;

import br.com.numberone.inventory.domain.model.ItemEstoque;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ItemEstoqueRepositoryPort {

    Optional<ItemEstoque> buscarPorId(UUID id);

    Optional<ItemEstoque> buscarPorCodigo(String codigo);

    Optional<ItemEstoque> buscarPorIdParaAtualizacao(UUID id);

    List<ItemEstoque> listarAtivos();
    ItemEstoque salvar(ItemEstoque item);
}
