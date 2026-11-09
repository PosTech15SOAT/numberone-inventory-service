
package br.com.numberone.inventory.application.service;

import br.com.numberone.inventory.domain.model.ItemEstoque;
import br.com.numberone.inventory.domain.port.out.ItemEstoqueRepositoryPort;

import java.util.UUID;

public class ConsultarSaldoDisponivelService {

    private final ItemEstoqueRepositoryPort itemRepository;

    public ConsultarSaldoDisponivelService(
            ItemEstoqueRepositoryPort itemRepository) {

        this.itemRepository = itemRepository;
    }

    public int consultar(UUID itemEstoqueId) {

        if (itemEstoqueId == null) {
            throw new IllegalArgumentException(
                    "Identificador do item obrigatorio"
            );
        }

        ItemEstoque item = itemRepository.buscarPorId(itemEstoqueId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Item de estoque nao encontrado"
                ));

        return item.getQuantidadeEstoque();
    }
}
