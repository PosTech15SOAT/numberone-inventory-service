
package br.com.numberone.inventory.adapter.out.persistence;

import br.com.numberone.inventory.domain.model.OperacaoConsumoEstoque;
import br.com.numberone.inventory.domain.model.StatusOperacaoConsumo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class OperacaoConsumoPersistenceAdapterTest {

    private static final String HASH = "a".repeat(64);

    private OperacaoConsumoJpaRepository repository;
    private OperacaoConsumoPersistenceAdapter adapter;

    private UUID operationId;
    private UUID serviceOrderId;
    private UUID serviceOrderItemId;
    private UUID responsibleUserId;

    @BeforeEach
    void configurar() {
        repository = mock(OperacaoConsumoJpaRepository.class);
        adapter = new OperacaoConsumoPersistenceAdapter(repository);

        operationId = UUID.randomUUID();
        serviceOrderId = UUID.randomUUID();
        serviceOrderItemId = UUID.randomUUID();
        responsibleUserId = UUID.randomUUID();
    }

    private OperacaoConsumoEstoque criarOperacao(
            StatusOperacaoConsumo status) {

        return new OperacaoConsumoEstoque(
                operationId,
                serviceOrderId,
                serviceOrderItemId,
                responsibleUserId,
                HASH,
                status
        );
    }

    @Test
    void deveBuscarOperacaoExistente() {
        var entity = OperacaoConsumoEntity.fromDomain(
                criarOperacao(StatusOperacaoConsumo.CONSUMED));

        when(repository.findById(operationId))
                .thenReturn(Optional.of(entity));

        var resultado = adapter.buscarPorOperationId(operationId);

        assertTrue(resultado.isPresent());
        assertEquals(operationId, resultado.get().getOperationId());
        assertEquals(
                StatusOperacaoConsumo.CONSUMED,
                resultado.get().getStatus());
    }

    @Test
    void deveRetornarVazioQuandoOperacaoNaoExiste() {
        when(repository.findById(operationId))
                .thenReturn(Optional.empty());

        assertTrue(adapter.buscarPorOperationId(operationId).isEmpty());
    }

    @Test
    void deveSalvarNovaOperacaoEmProcessamento() {
        var operacao = criarOperacao(StatusOperacaoConsumo.PROCESSING);

        when(repository.findById(operationId))
                .thenReturn(Optional.empty());

        when(repository.save(any(OperacaoConsumoEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var resultado = adapter.salvar(operacao);

        assertEquals(
                StatusOperacaoConsumo.PROCESSING,
                resultado.getStatus());

        verify(repository).save(any(OperacaoConsumoEntity.class));
    }

    @Test
    void naoDeveCriarOperacaoJaFinalizada() {
        var operacao = criarOperacao(StatusOperacaoConsumo.CONSUMED);

        when(repository.findById(operationId))
                .thenReturn(Optional.empty());

        assertThrows(
                IllegalStateException.class,
                () -> adapter.salvar(operacao));

        verify(repository, never())
                .save(any(OperacaoConsumoEntity.class));
    }

    @Test
    void deveAtualizarStatusPreservandoDataDeCriacao() {
        var entity = OperacaoConsumoEntity.fromDomain(
                criarOperacao(StatusOperacaoConsumo.PROCESSING));

        OffsetDateTime dataCriacao = entity.getCreatedAt();

        when(repository.findById(operationId))
                .thenReturn(Optional.of(entity));

        when(repository.save(any(OperacaoConsumoEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var operacao = criarOperacao(StatusOperacaoConsumo.CONSUMED);

        var resultado = adapter.salvar(operacao);

        assertEquals(
                StatusOperacaoConsumo.CONSUMED,
                resultado.getStatus());

        assertEquals(dataCriacao, entity.getCreatedAt());

        assertFalse(
                entity.getUpdatedAt().isBefore(dataCriacao));
    }

    @Test
    void naoDeveAlterarOperacaoComHashDiferente() {
        var entity = OperacaoConsumoEntity.fromDomain(
                criarOperacao(StatusOperacaoConsumo.PROCESSING));

        when(repository.findById(operationId))
                .thenReturn(Optional.of(entity));

        var operacao = new OperacaoConsumoEstoque(
                operationId,
                serviceOrderId,
                serviceOrderItemId,
                responsibleUserId,
                "b".repeat(64),
                StatusOperacaoConsumo.CONSUMED
        );

        assertThrows(
                IllegalStateException.class,
                () -> adapter.salvar(operacao));

        verify(repository, never())
                .save(any(OperacaoConsumoEntity.class));
    }

    @Test
    void naoDeveAlterarOperacaoComOrdemServicoDiferente() {
        var entity = OperacaoConsumoEntity.fromDomain(
                criarOperacao(StatusOperacaoConsumo.PROCESSING));

        when(repository.findById(operationId))
                .thenReturn(Optional.of(entity));

        var operacao = new OperacaoConsumoEstoque(
                operationId,
                UUID.randomUUID(),
                serviceOrderItemId,
                responsibleUserId,
                HASH,
                StatusOperacaoConsumo.CONSUMED
        );

        assertThrows(
                IllegalStateException.class,
                () -> adapter.salvar(operacao));

        verify(repository, never())
                .save(any(OperacaoConsumoEntity.class));
    }

    @Test
    void naoDeveAlterarOperacaoFinalizada() {
        var entity = OperacaoConsumoEntity.fromDomain(
                criarOperacao(StatusOperacaoConsumo.CONSUMED));

        when(repository.findById(operationId))
                .thenReturn(Optional.of(entity));

        var operacao = criarOperacao(
                StatusOperacaoConsumo.INSUFFICIENT_STOCK);

        assertThrows(
                IllegalStateException.class,
                () -> adapter.salvar(operacao));

        verify(repository, never())
                .save(any(OperacaoConsumoEntity.class));
    }
}
