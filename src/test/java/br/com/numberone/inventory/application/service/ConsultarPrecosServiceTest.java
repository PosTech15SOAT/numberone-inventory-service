
package br.com.numberone.inventory.application.service;

import br.com.numberone.inventory.domain.model.ItemEstoque;
import br.com.numberone.inventory.application.port.out.ItemEstoqueRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ConsultarPrecosServiceTest {

    private ItemEstoqueRepositoryPort repository;
    private ConsultarPrecosService service;

    @BeforeEach
    void configurar() {
        repository = mock(ItemEstoqueRepositoryPort.class);
        service = new ConsultarPrecosService(repository);
    }

    private ItemEstoque criarItem(UUID id, BigDecimal precoVenda) {
        return new ItemEstoque(
                id,
                "FILTRO-" + id,
                "Filtro de oleo",
                "Filtro para motor",
                "PECA",
                "UNIDADE",
                10,
                new BigDecimal("20.00"),
                precoVenda,
                5,
                "Bosch",
                "Veiculos diversos",
                true
        );
    }

    @Test
    void deveConsultarPrecoDeUmItem() {
        UUID id = UUID.randomUUID();

        when(repository.buscarPorId(id))
                .thenReturn(Optional.of(
                        criarItem(id, new BigDecimal("35.00"))
                ));

        var resultado = service.consultar(List.of(id));

        assertEquals(1, resultado.size());
        assertEquals(id, resultado.get(0).inventoryItemId());
        assertEquals(
                0,
                new BigDecimal("35.00")
                        .compareTo(resultado.get(0).salePrice())
        );
        assertEquals("BRL", resultado.get(0).currency());

        verify(repository).buscarPorId(id);
    }

    @Test
    void deveRetornarPrecoEmBrlComDuasCasasDecimais() {
        UUID id = UUID.randomUUID();
        when(repository.buscarPorId(id))
                .thenReturn(Optional.of(criarItem(id, new BigDecimal("35.125"))));

        var preco = service.consultar(List.of(id)).getFirst();

        assertEquals(new BigDecimal("35.13"), preco.salePrice());
        assertEquals("BRL", preco.currency());
    }

    @Test
    @org.junit.jupiter.api.Disabled("Pendente: criar item sem preco valido")
    void naoDeveRetornarResultadoParcialQuandoItemNaoTemPreco() {
        UUID existenteId = UUID.randomUUID();
        UUID semPrecoId = UUID.randomUUID();
        when(repository.buscarPorId(existenteId))
                .thenReturn(Optional.of(criarItem(existenteId, new BigDecimal("35.00"))));
        when(repository.buscarPorId(semPrecoId))
                .thenReturn(Optional.of(criarItem(semPrecoId, null)));

        var exception = assertThrows(
                ConsultarPrecosService.ConsultaPrecosException.class,
                () -> service.consultar(List.of(existenteId, semPrecoId))
        );

        assertEquals("INVENTORY_PRICE_NOT_FOUND", exception.getCodigo());
        assertEquals(List.of(semPrecoId), exception.getItemIds());
    }

    @Test
    void deveConsultarPrecoDeVariosItens() {
        UUID primeiroId = UUID.randomUUID();
        UUID segundoId = UUID.randomUUID();

        when(repository.buscarPorId(primeiroId))
                .thenReturn(Optional.of(
                        criarItem(primeiroId, new BigDecimal("35.00"))
                ));

        when(repository.buscarPorId(segundoId))
                .thenReturn(Optional.of(
                        criarItem(segundoId, new BigDecimal("80.00"))
                ));

        var resultado = service.consultar(
                List.of(primeiroId, segundoId)
        );

        assertEquals(2, resultado.size());
        assertEquals(primeiroId, resultado.get(0).inventoryItemId());
        assertEquals(segundoId, resultado.get(1).inventoryItemId());
        assertEquals(
                0,
                new BigDecimal("80.00")
                        .compareTo(resultado.get(1).salePrice())
        );
    }

    @Test
    void naoDeveRetornarResultadoParcialQuandoItemNaoExiste() {
        UUID existenteId = UUID.randomUUID();
        UUID inexistenteId = UUID.randomUUID();

        when(repository.buscarPorId(existenteId))
                .thenReturn(Optional.of(
                        criarItem(existenteId, new BigDecimal("35.00"))
                ));

        when(repository.buscarPorId(inexistenteId))
                .thenReturn(Optional.empty());

        var exception = assertThrows(
                ConsultarPrecosService.ConsultaPrecosException.class,
                () -> service.consultar(
                        List.of(existenteId, inexistenteId)
                )
        );

        assertEquals(
                "INVENTORY_ITEM_NOT_FOUND",
                exception.getCodigo()
        );
        assertEquals(
                List.of(inexistenteId),
                exception.getItemIds()
        );
    }

    @Test
    void deveRejeitarListaVazia() {
        assertThrows(
                IllegalArgumentException.class,
                () -> service.consultar(List.of())
        );

        verifyNoInteractions(repository);
    }

    @Test
    void deveRejeitarListaNula() {
        assertThrows(
                IllegalArgumentException.class,
                () -> service.consultar(null)
        );

        verifyNoInteractions(repository);
    }

    @Test
    void naoDeveConsultarItemDuplicadoMaisDeUmaVez() {
        UUID id = UUID.randomUUID();

        when(repository.buscarPorId(id))
                .thenReturn(Optional.of(
                        criarItem(id, new BigDecimal("35.00"))
                ));

        var resultado = service.consultar(
                List.of(id, id, id)
        );

        assertEquals(1, resultado.size());
        verify(repository, times(1)).buscarPorId(id);
    }

    @Test
    void deveRejeitarIdNulo() {
        assertThrows(
                IllegalArgumentException.class,
                () -> service.consultar(
                        java.util.Arrays.asList(UUID.randomUUID(), null)
                )
        );

        verifyNoInteractions(repository);
    }
}
