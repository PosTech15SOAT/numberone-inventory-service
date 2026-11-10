
package br.com.numberone.inventory.adapter.out.persistence;

import br.com.numberone.inventory.domain.model.SolicitacaoConsumoEstoque;
import br.com.numberone.inventory.domain.port.out.OperacaoConsumoItemRepositoryPort;

import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Repository
public class OperacaoConsumoItemPersistenceAdapter
        implements OperacaoConsumoItemRepositoryPort {

    private final OperacaoConsumoItemJpaRepository repository;

    public OperacaoConsumoItemPersistenceAdapter(
            OperacaoConsumoItemJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public void salvarItens(
            UUID operationId,
            List<SolicitacaoConsumoEstoque.ItemConsumo> items) {

        Objects.requireNonNull(
                operationId,
                "operationId obrigatorio"
        );

        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException(
                    "A operacao deve conter pelo menos um item."
            );
        }

        List<OperacaoConsumoItemEntity> entities =
                items.stream()
                        .map(item -> new OperacaoConsumoItemEntity(
                                operationId,
                                item.inventoryItemId(),
                                item.quantity()
                        ))
                        .toList();

        repository.saveAll(entities);
    }

    @Override
    public List<SolicitacaoConsumoEstoque.ItemConsumo> buscarItens(
            UUID operationId) {

        Objects.requireNonNull(
                operationId,
                "operationId obrigatorio"
        );

        return repository.findByOperationId(operationId)
                .stream()
                .map(entity ->
                        new SolicitacaoConsumoEstoque.ItemConsumo(
                                entity.getInventoryItemId(),
                                entity.getQuantity()
                        )
                )
                .toList();
    }
}
