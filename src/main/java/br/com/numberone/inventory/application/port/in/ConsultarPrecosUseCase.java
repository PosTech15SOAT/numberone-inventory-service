package br.com.numberone.inventory.application.port.in;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
public interface ConsultarPrecosUseCase {
    List<PrecoItem> consultar(List<UUID> inventoryItemIds);
    record PrecoItem(UUID inventoryItemId, BigDecimal salePrice, String currency) { }
}
