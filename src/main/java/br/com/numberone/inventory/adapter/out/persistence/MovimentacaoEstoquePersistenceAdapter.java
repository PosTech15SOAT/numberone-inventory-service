package br.com.numberone.inventory.adapter.out.persistence;

import br.com.numberone.inventory.domain.model.MovimentacaoEstoque;
import br.com.numberone.inventory.domain.port.out.MovimentacaoEstoqueRepositoryPort;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
class MovimentacaoEstoquePersistenceAdapter implements MovimentacaoEstoqueRepositoryPort {
    private final MovimentacaoEstoqueJpaRepository repository;

    MovimentacaoEstoquePersistenceAdapter(MovimentacaoEstoqueJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public MovimentacaoEstoque salvar(MovimentacaoEstoque movimento, UUID operationId) {
        return repository.save(MovimentacaoEstoqueEntity.fromDomain(movimento, operationId)).toDomain();
    }

    @Override
    public List<UUID> buscarIdsPorOperacao(UUID operationId) {
        return repository.findByOperationIdOrderByItemEstoqueId(operationId)
                .stream().map(MovimentacaoEstoqueEntity::getId).toList();
    }

    @Override
    public List<MovimentacaoEstoque> buscarPorItem(UUID itemId) {
        return repository.findByItemEstoqueIdOrderByCreatedAtDescIdAsc(itemId)
                .stream().map(MovimentacaoEstoqueEntity::toDomain).toList();
    }
}
