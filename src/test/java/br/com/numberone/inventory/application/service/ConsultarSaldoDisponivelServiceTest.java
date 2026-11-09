
package br.com.numberone.inventory.application.service;

import br.com.numberone.inventory.domain.model.ItemEstoque;
import br.com.numberone.inventory.domain.port.out.ItemEstoqueRepositoryPort;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ConsultarSaldoDisponivelServiceTest {

    private ItemEstoqueRepositoryPort itemRepository;
    private ConsultarSaldoDisponivelService service;

    private final UUID itemId = UUID.randomUUID();

    @BeforeEach
    void configurar() {
        itemRepository = mock(ItemEstoqueRepositoryPort.class);

        service = new ConsultarSaldoDisponivelService(
                itemRepository
        );
    }

    private ItemEstoque criarItem(int quantidade) {
        return new ItemEstoque(
                itemId,
                "FILTRO-001",
                "Filtro de oleo",
                "Filtro para motor",
                "PECA",
                "UNIDADE",
                quantidade,
                new BigDecimal("20.00"),
                new BigDecimal("35.00"),
                5,
                "Bosch",
                "Veiculos diversos",
                true
        );
    }

    @Test
    void deveRetornarQuantidadeFisicaEmEstoque() {
        when(itemRepository.buscarPorId(itemId))
                .thenReturn(Optional.of(criarItem(10)));

        int saldo = service.consultar(itemId);

        assertEquals(10, saldo);
        verify(itemRepository).buscarPorId(itemId);
    }

    @Test
    void deveRetornarZeroQuandoEstoqueEstiverEsgotado() {
        when(itemRepository.buscarPorId(itemId))
                .thenReturn(Optional.of(criarItem(0)));

        assertEquals(0, service.consultar(itemId));
    }

    @Test
    void deveRetornarQuantidadeMesmoAbaixoDoEstoqueMinimo() {
        when(itemRepository.buscarPorId(itemId))
                .thenReturn(Optional.of(criarItem(3)));

        assertEquals(3, service.consultar(itemId));
    }

    @Test
    void deveRetornarQuantidadeIgualAoEstoqueMinimo() {
        when(itemRepository.buscarPorId(itemId))
                .thenReturn(Optional.of(criarItem(5)));

        assertEquals(5, service.consultar(itemId));
    }

    @Test
    void naoDeveConsultarItemInexistente() {
        when(itemRepository.buscarPorId(itemId))
                .thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> service.consultar(itemId)
        );

        verify(itemRepository).buscarPorId(itemId);
    }

    @Test
    void naoDeveAceitarIdentificadorNulo() {
        assertThrows(
                IllegalArgumentException.class,
                () -> service.consultar(null)
        );

        verifyNoInteractions(itemRepository);
    }
}
