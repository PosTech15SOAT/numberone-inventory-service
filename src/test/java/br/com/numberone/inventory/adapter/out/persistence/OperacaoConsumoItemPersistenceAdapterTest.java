
package br.com.numberone.inventory.adapter.out.persistence;

import br.com.numberone.inventory.domain.model.SolicitacaoConsumoEstoque;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OperacaoConsumoItemPersistenceAdapterTest {

    private OperacaoConsumoItemJpaRepository repository;
    private OperacaoConsumoItemPersistenceAdapter adapter;

    private UUID operationId;
    private UUID inventoryItemId;

    @BeforeEach
    void configurar() {
        repository = mock(OperacaoConsumoItemJpaRepository.class);

        adapter = new OperacaoConsumoItemPersistenceAdapter(
                repository
        );

        operationId = UUID.randomUUID();
        inventoryItemId = UUID.randomUUID();
    }

    @Test
    void deveSalvarItensDaOperacao() {
        var items = List.of(
                new SolicitacaoConsumoEstoque.ItemConsumo(
                        inventoryItemId, 3
                )
        );

        adapter.salvarItens(operationId, items);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<OperacaoConsumoItemEntity>> captor =
                ArgumentCaptor.forClass(List.class);

        verify(repository).saveAll(captor.capture());

        var entities = captor.getValue();

        assertEquals(1, entities.size());
        assertEquals(operationId, entities.get(0).getOperationId());
        assertEquals(
                inventoryItemId,
                entities.get(0).getInventoryItemId()
        );
        assertEquals(3, entities.get(0).getQuantity());
    }

    @Test
    void deveBuscarItensDaOperacao() {
        var entity = new OperacaoConsumoItemEntity(
                operationId,
                inventoryItemId,
                5
        );

        when(repository.findByOperationId(operationId))
                .thenReturn(List.of(entity));

        var resultado = adapter.buscarItens(operationId);

        assertEquals(1, resultado.size());
        assertEquals(
                inventoryItemId,
                resultado.get(0).inventoryItemId()
        );
        assertEquals(5, resultado.get(0).quantity());
    }

    @Test
    void naoDeveSalvarListaVazia() {
        assertThrows(
                IllegalArgumentException.class,
                () -> adapter.salvarItens(
                        operationId,
                        List.of()
                )
        );

        verifyNoInteractions(repository);
    }

    @Test
    void naoDeveAceitarOperationIdNulo() {
        assertAll(
                () -> assertThrows(
                        NullPointerException.class,
                        () -> adapter.salvarItens(
                                null,
                                List.of(
                                        new SolicitacaoConsumoEstoque.ItemConsumo(
                                                inventoryItemId, 1
                                        )
                                )
                        )
                ),
                () -> assertThrows(
                        NullPointerException.class,
                        () -> adapter.buscarItens(null)
                )
        );

        verifyNoInteractions(repository);
    }
}
