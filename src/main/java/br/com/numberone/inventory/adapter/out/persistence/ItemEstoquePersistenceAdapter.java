
package br.com.numberone.inventory.adapter.out.persistence;

import br.com.numberone.inventory.domain.model.ItemEstoque;
import br.com.numberone.inventory.domain.port.out.ItemEstoqueRepositoryPort;

import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class ItemEstoquePersistenceAdapter
        implements ItemEstoqueRepositoryPort {

    private final ItemEstoqueJpaRepository repository;

    public ItemEstoquePersistenceAdapter(
            ItemEstoqueJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<ItemEstoque> buscarPorId(UUID id) {
        return repository.findById(id)
                .map(ItemEstoqueEntity::toDomain);
    }

    @Override
    public Optional<ItemEstoque> buscarPorCodigo(String codigo) {
        return repository.findByCodigo(codigo)
                .map(ItemEstoqueEntity::toDomain);
    }

    @Override
    public Optional<ItemEstoque> buscarPorIdParaAtualizacao(UUID id) {
        return repository.buscarPorIdParaAtualizacao(id)
                .map(ItemEstoqueEntity::toDomain);
    }

    @Override
    public ItemEstoque salvar(ItemEstoque item) {
        ItemEstoqueEntity entity = ItemEstoqueEntity.fromDomain(item);

        ItemEstoqueEntity salva = repository.save(entity);

        return salva.toDomain();
    }
}
