
package br.com.numberone.inventory.application.service;

import br.com.numberone.inventory.application.port.in.ConsultarPrecosUseCase;
import br.com.numberone.inventory.application.port.in.ConsultarPrecosUseCase.PrecoItem;

import br.com.numberone.inventory.domain.model.ItemEstoque;
import br.com.numberone.inventory.application.port.out.ItemEstoqueRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class ConsultarPrecosService implements ConsultarPrecosUseCase {

    private final ItemEstoqueRepositoryPort itemEstoqueRepository;

    public ConsultarPrecosService(
            ItemEstoqueRepositoryPort itemEstoqueRepository) {
        this.itemEstoqueRepository = itemEstoqueRepository;
    }

    @Transactional(readOnly = true)
    public List<PrecoItem> consultar(List<UUID> inventoryItemIds) {
        if (inventoryItemIds == null || inventoryItemIds.isEmpty()
                || inventoryItemIds.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException(
                    "A lista de itens deve conter IDs válidos.");
        }

        List<PrecoItem> precos = new ArrayList<>();
        List<UUID> itensNaoEncontrados = new ArrayList<>();
        List<UUID> itensSemPreco = new ArrayList<>();

        for (UUID inventoryItemId : inventoryItemIds.stream().distinct().toList()) {
            ItemEstoque item = itemEstoqueRepository
                    .buscarPorId(inventoryItemId)
                    .orElse(null);

            if (item == null) {
                itensNaoEncontrados.add(inventoryItemId);
                continue;
            }

            BigDecimal precoVenda = item.getPrecoVenda();

            if (precoVenda == null
                    || precoVenda.compareTo(BigDecimal.ZERO) <= 0) {
                itensSemPreco.add(inventoryItemId);
                continue;
            }

            precos.add(new PrecoItem(
                    inventoryItemId,
                    precoVenda.setScale(2, RoundingMode.HALF_UP),
                    "BRL"
            ));
        }

        if (!itensNaoEncontrados.isEmpty() || !itensSemPreco.isEmpty()) {
            List<UUID> idsComProblema = new ArrayList<>();
            idsComProblema.addAll(itensNaoEncontrados);
            idsComProblema.addAll(itensSemPreco);

            String codigoErro = itensSemPreco.isEmpty()
                    ? "INVENTORY_ITEM_NOT_FOUND"
                    : "INVENTORY_PRICE_NOT_FOUND";

            throw new ConsultaPrecosException(codigoErro, idsComProblema);
        }

        return List.copyOf(precos);
    }

public static class ConsultaPrecosException
            extends RuntimeException {

        private final String codigo;
        private final List<UUID> itemIds;

        public ConsultaPrecosException(
                String codigo,
                List<UUID> itemIds) {
            super(codigo + ": " + itemIds);
            this.codigo = codigo;
            this.itemIds = List.copyOf(itemIds);
        }

        public String getCodigo() {
            return codigo;
        }

        public List<UUID> getItemIds() {
            return itemIds;
        }
    }
}
