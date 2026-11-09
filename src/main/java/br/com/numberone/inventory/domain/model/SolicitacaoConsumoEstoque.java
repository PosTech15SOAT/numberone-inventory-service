
package br.com.numberone.inventory.domain.model;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public record SolicitacaoConsumoEstoque(
        UUID operationId,
        UUID serviceOrderId,
        UUID serviceOrderItemId,
        UUID responsibleUserId,
        List<ItemConsumo> items
) {

    public SolicitacaoConsumoEstoque {
        if (operationId == null
                || serviceOrderId == null
                || serviceOrderItemId == null
                || responsibleUserId == null) {
            throw new IllegalArgumentException(
                    "Os identificadores da operacao de consumo sao obrigatorios."
            );
        }

        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException(
                    "A operacao deve conter pelo menos um item."
            );
        }

        items = List.copyOf(items);

        Set<UUID> ids = new HashSet<>();

        for (ItemConsumo item : items) {
            if (!ids.add(item.inventoryItemId())) {
                throw new IllegalArgumentException(
                        "Nao sao permitidos itens de estoque duplicados."
                );
            }
        }
    }

    public record ItemConsumo(
            UUID inventoryItemId,
            int quantity
    ) {
        public ItemConsumo {
            if (inventoryItemId == null) {
                throw new IllegalArgumentException(
                        "O identificador do item de estoque e obrigatorio."
                );
            }

            if (quantity <= 0) {
                throw new IllegalArgumentException(
                        "A quantidade consumida deve ser maior que zero."
                );
            }
        }
    }
}
