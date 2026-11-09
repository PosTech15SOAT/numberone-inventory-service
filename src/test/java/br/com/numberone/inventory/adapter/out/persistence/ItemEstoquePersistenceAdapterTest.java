
package br.com.numberone.inventory.adapter.out.persistence;

import br.com.numberone.inventory.domain.model.ItemEstoque;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ItemEstoquePersistenceAdapterTest {

    private ItemEstoqueJpaRepository repository;
    private ItemEstoquePersistenceAdapter adapter;

    @BeforeEach
    void configurar() {
        repository = mock(ItemEstoqueJpaRepository.class);
        adapter = new ItemEstoquePersistenceAdapter(repository);
    }

    @Test
    void deveBuscarItemPorId() {

        UUID id = UUID.randomUUID();

        ItemEstoque item = criarItem(id);

        ItemEstoqueEntity entity =
                ItemEstoqueEntity.fromDomain(item);

        when(repository.findById(id))
                .thenReturn(Optional.of(entity));

        Optional<ItemEstoque> resultado =
                adapter.buscarPorId(id);

        assertTrue(resultado.isPresent());
        assertEquals(id, resultado.get().getId());
        assertEquals("OLEO-001", resultado.get().getCodigo());
        assertEquals(10, resultado.get().getQuantidadeEstoque());

        verify(repository).findById(id);
    }

    @Test
    void deveRetornarVazioQuandoItemNaoExiste() {

        UUID id = UUID.randomUUID();

        when(repository.findById(id))
                .thenReturn(Optional.empty());

        Optional<ItemEstoque> resultado =
                adapter.buscarPorId(id);

        assertTrue(resultado.isEmpty());

        verify(repository).findById(id);
    }


@Test
void deveBuscarItemPorCodigo() {

    UUID id = UUID.randomUUID();
    ItemEstoque item = criarItem(id);

    when(repository.findByCodigo("OLEO-001"))
            .thenReturn(Optional.of(
                    ItemEstoqueEntity.fromDomain(item)
            ));

    Optional<ItemEstoque> resultado =
            adapter.buscarPorCodigo("OLEO-001");

    assertTrue(resultado.isPresent());
    assertEquals(id, resultado.get().getId());
    assertEquals("OLEO-001", resultado.get().getCodigo());

    verify(repository).findByCodigo("OLEO-001");
}

@Test
void deveBuscarItemPorIdParaAtualizacao() {

    UUID id = UUID.randomUUID();
    ItemEstoque item = criarItem(id);

    when(repository.buscarPorIdParaAtualizacao(id))
            .thenReturn(Optional.of(
                    ItemEstoqueEntity.fromDomain(item)
            ));

    Optional<ItemEstoque> resultado =
            adapter.buscarPorIdParaAtualizacao(id);

    assertTrue(resultado.isPresent());
    assertEquals(id, resultado.get().getId());

    verify(repository).buscarPorIdParaAtualizacao(id);
}

    @Test
    void deveSalvarItemEstoque() {

        UUID id = UUID.randomUUID();
        ItemEstoque item = criarItem(id);

        when(repository.save(any(ItemEstoqueEntity.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0)
                );

        ItemEstoque resultado = adapter.salvar(item);

        assertNotNull(resultado);
        assertEquals(id, resultado.getId());
        assertEquals("OLEO-001", resultado.getCodigo());
        assertEquals(10, resultado.getQuantidadeEstoque());

        verify(repository).save(any(ItemEstoqueEntity.class));
    }


    private ItemEstoque criarItem(UUID id) {
        return new ItemEstoque(
                id,
                "OLEO-001",
                "Óleo de motor",
                "Óleo sintético",
                "PECA",
                "UNIDADE",
                10,
                new BigDecimal("30.00"),
                new BigDecimal("50.00"),
                2,
                "Marca Teste",
                "Veículos diversos",
                true
        );
    }
}
