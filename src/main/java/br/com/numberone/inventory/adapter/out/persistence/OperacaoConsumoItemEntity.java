
package br.com.numberone.inventory.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.util.UUID;

@Entity
@Table(
    name = "operacao_consumo_item",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_operacao_consumo_item",
        columnNames = {"operation_id", "item_estoque_id"}
    )
)
public class OperacaoConsumoItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "operation_id", nullable = false)
    private UUID operationId;

    @Column(name = "item_estoque_id", nullable = false)
    private UUID inventoryItemId;

    @Column(name = "quantidade", nullable = false)
    private int quantity;

    protected OperacaoConsumoItemEntity() {
    }

    public OperacaoConsumoItemEntity(
            UUID operationId,
            UUID inventoryItemId,
            int quantity) {

        if (operationId == null || inventoryItemId == null) {
            throw new IllegalArgumentException(
                    "Os identificadores da operacao e do item sao obrigatorios."
            );
        }

        if (quantity <= 0) {
            throw new IllegalArgumentException(
                    "A quantidade deve ser maior que zero."
            );
        }

        this.operationId = operationId;
        this.inventoryItemId = inventoryItemId;
        this.quantity = quantity;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOperationId() {
        return operationId;
    }

    public UUID getInventoryItemId() {
        return inventoryItemId;
    }

    public int getQuantity() {
        return quantity;
    }
}
