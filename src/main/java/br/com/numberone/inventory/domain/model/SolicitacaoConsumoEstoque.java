package br.com.numberone.inventory.domain.model;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

public record SolicitacaoConsumoEstoque(
        UUID operationId,
        UUID serviceOrderId,
        UUID serviceOrderItemId,
        UUID responsibleUserId,
        List<ItemConsumo> items
) {
    private static final UUID ZERO = new UUID(0, 0);

    public SolicitacaoConsumoEstoque {
        validarId(operationId);
        validarId(serviceOrderId);
        validarId(serviceOrderItemId);
        validarId(responsibleUserId);

        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("A operacao deve conter pelo menos um item.");
        }

        // A mesma ordem é usada para o hash e para a aquisição dos locks de saldo.
        Map<String, ItemConsumo> normalizados = new TreeMap<>();
        for (ItemConsumo item : items) {
            if (item == null) {
                throw new IllegalArgumentException("Item de consumo obrigatorio.");
            }
            normalizados.merge(item.inventoryItemId().toString(), item, (a, b) ->
                    new ItemConsumo(a.inventoryItemId(), somarQuantidades(a.quantity(), b.quantity())));
        }
        items = List.copyOf(normalizados.values());
    }

    private static int somarQuantidades(int a, int b) {
        if (a > Integer.MAX_VALUE - b) {
            throw new IllegalArgumentException("Quantidade total excede o limite por item.");
        }
        return a + b;
    }

    private static void validarId(UUID id) {
        if (id == null || ZERO.equals(id)) {
            throw new IllegalArgumentException("Identificador de consumo invalido.");
        }
    }

    public record ItemConsumo(UUID inventoryItemId, int quantity) {
        public ItemConsumo {
            validarId(inventoryItemId);
            if (quantity <= 0) {
                throw new IllegalArgumentException("A quantidade consumida deve ser maior que zero.");
            }
        }
    }
}
