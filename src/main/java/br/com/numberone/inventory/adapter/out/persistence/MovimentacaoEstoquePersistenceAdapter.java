package br.com.numberone.inventory.adapter.out.persistence;

import br.com.numberone.inventory.domain.model.MovimentacaoEstoque;
import br.com.numberone.inventory.application.port.out.MovimentacaoEstoqueRepositoryPort;
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
                .stream().filter(m -> m.getOriginalMovementId() == null).map(MovimentacaoEstoqueEntity::getId).toList();
    }

    public List<UUID> buscarIdsInversos(UUID operationId) {
        return repository.findByOperationIdOrderByItemEstoqueId(operationId)
                .stream().filter(m -> m.getOriginalMovementId() != null).map(MovimentacaoEstoqueEntity::getId).toList();
    }

    public List<MovimentacaoEstoque> buscarBaixas(UUID operationId) {
        return repository.findByOperationIdOrderByItemEstoqueId(operationId)
                .stream().filter(m -> m.getOriginalMovementId() == null).map(MovimentacaoEstoqueEntity::toDomain).toList();
    }

    public MovimentacaoEstoque salvarInverso(MovimentacaoEstoque movimento, UUID operationId, UUID originalId, UUID compensationId) {
        var entity = MovimentacaoEstoqueEntity.fromDomain(movimento, operationId);
        entity.vincularInverso(originalId, compensationId);
        return repository.save(entity).toDomain();
    }

    @Override
    public List<MovimentacaoEstoque> buscarPorItem(UUID itemId) {
        return repository.findByItemEstoqueIdOrderByCreatedAtDescIdAsc(itemId)
                .stream().map(MovimentacaoEstoqueEntity::toDomain).toList();
    }
}
