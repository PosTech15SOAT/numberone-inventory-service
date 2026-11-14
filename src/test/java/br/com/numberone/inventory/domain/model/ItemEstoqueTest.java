
package br.com.numberone.inventory.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ItemEstoqueTest {

    private ItemEstoque criarItem(int quantidade, int minimo) {
        return new ItemEstoque(
                UUID.randomUUID(),
                "FILTRO-001",
                "Filtro de oleo",
                "Filtro para motor",
                "PECA",
                "UNIDADE",
                quantidade,
                new BigDecimal("20.00"),
                new BigDecimal("35.00"),
                minimo,
                "Bosch",
                "Veiculos diversos",
                true
        );
    }

    @Test
    void deveAdicionarEstoque() {
        ItemEstoque item = criarItem(10, 5);

        item.adicionarEstoque(5);

        assertEquals(15, item.getQuantidadeEstoque());
    }

    @Test
    void deveBaixarEstoque() {
        ItemEstoque item = criarItem(10, 5);

        item.baixarEstoque(3);

        assertEquals(7, item.getQuantidadeEstoque());
    }

    @Test
    void naoDeveBaixarMaisQueSaldoFisico() {
        ItemEstoque item = criarItem(10, 5);

        assertThrows(
                IllegalArgumentException.class,
                () -> item.baixarEstoque(11)
        );
    }

    @Test
    void naoDevePermitirQuantidadeNegativa() {
        assertThrows(
                IllegalArgumentException.class,
                () -> criarItem(-1, 5)
        );
    }

    @Test
    void deveIdentificarEstoqueAbaixoDoMinimo() {
        ItemEstoque item = criarItem(3, 5);

        assertTrue(item.abaixoDoEstoqueMinimo());
    }

    @Test
    void naoDeveConsiderarEstoqueIgualAoMinimoComoAbaixo() {
        ItemEstoque item = criarItem(5, 5);

        assertFalse(item.abaixoDoEstoqueMinimo());
    }
}
