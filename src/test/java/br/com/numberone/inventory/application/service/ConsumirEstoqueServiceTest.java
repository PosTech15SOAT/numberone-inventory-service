
package br.com.numberone.inventory.application.service;

import br.com.numberone.inventory.domain.model.OperacaoConsumoEstoque;
import br.com.numberone.inventory.domain.model.ResultadoConsumoEstoque;
import br.com.numberone.inventory.domain.model.SolicitacaoConsumoEstoque;
import br.com.numberone.inventory.domain.model.StatusOperacaoConsumo;
import br.com.numberone.inventory.application.port.out.ItemEstoqueRepositoryPort;
import br.com.numberone.inventory.application.port.out.MovimentacaoEstoqueRepositoryPort;
import br.com.numberone.inventory.application.port.out.OperacaoConsumoItemRepositoryPort;
import br.com.numberone.inventory.application.port.out.OperacaoConsumoRepositoryPort;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ConsumirEstoqueServiceTest {

    private OperacaoConsumoRepositoryPort repository;
    private CalculadorHashConsumoService calculadorHash;
    private ConsumirEstoqueService service;

    private UUID operationId;
    private UUID serviceOrderId;
    private UUID serviceOrderItemId;
    private UUID responsibleUserId;
    private UUID inventoryItemId;

    @BeforeEach
    void configurar() {
        repository = mock(OperacaoConsumoRepositoryPort.class);
        calculadorHash = new CalculadorHashConsumoService();

        service = new ConsumirEstoqueService(
                repository,
                calculadorHash,
                mock(ItemEstoqueRepositoryPort.class),
                mock(OperacaoConsumoItemRepositoryPort.class),
                mock(MovimentacaoEstoqueRepositoryPort.class)
        );

        operationId = UUID.randomUUID();
        serviceOrderId = UUID.randomUUID();
        serviceOrderItemId = UUID.randomUUID();
        responsibleUserId = UUID.randomUUID();
        inventoryItemId = UUID.randomUUID();
    }

    private SolicitacaoConsumoEstoque criarSolicitacao(int quantidade) {
        return new SolicitacaoConsumoEstoque(
                operationId,
                serviceOrderId,
                serviceOrderItemId,
                responsibleUserId,
                List.of(
                        new SolicitacaoConsumoEstoque.ItemConsumo(
                                inventoryItemId,
                                quantidade
                        )
                )
        );
    }

    private OperacaoConsumoEstoque criarOperacao(
            SolicitacaoConsumoEstoque solicitacao,
            StatusOperacaoConsumo status) {

        return new OperacaoConsumoEstoque(
                solicitacao.operationId(),
                solicitacao.serviceOrderId(),
                solicitacao.serviceOrderItemId(),
                solicitacao.responsibleUserId(),
                calculadorHash.calcular(solicitacao),
                status
        );
    }

    @Test
    void deveRetornarNullQuandoOperacaoNaoExiste() {
        var solicitacao = criarSolicitacao(2);

        when(repository.buscarPorOperationId(operationId))
                .thenReturn(Optional.empty());

        ResultadoConsumoEstoque resultado =
                service.consultarOperacaoExistente(solicitacao);

        assertNull(resultado);
    }

    @Test
    void deveRetornarResultadoDeOperacaoConsumida() {
        var solicitacao = criarSolicitacao(2);
        var operacao = criarOperacao(
                solicitacao,
                StatusOperacaoConsumo.CONSUMED
        );

        when(repository.buscarPorOperationId(operationId))
                .thenReturn(Optional.of(operacao));

        var resultado = service.consultarOperacaoExistente(solicitacao);

        assertNotNull(resultado);
        assertEquals(operationId, resultado.operationId());
        assertEquals(
                StatusOperacaoConsumo.CONSUMED,
                resultado.status()
        );
    }

    @Test
    void deveRetornarResultadoDeEstoqueInsuficiente() {
        var solicitacao = criarSolicitacao(10);
        var operacao = criarOperacao(
                solicitacao,
                StatusOperacaoConsumo.INSUFFICIENT_STOCK
        );

        when(repository.buscarPorOperationId(operationId))
                .thenReturn(Optional.of(operacao));

        var resultado = service.consultarOperacaoExistente(solicitacao);

        assertNotNull(resultado);
        assertEquals(
                StatusOperacaoConsumo.INSUFFICIENT_STOCK,
                resultado.status()
        );
    }

    @Test
    void deveRejeitarMesmoOperationIdComDadosDiferentes() {
        var solicitacaoOriginal = criarSolicitacao(2);
        var solicitacaoAlterada = criarSolicitacao(5);

        var operacao = criarOperacao(
                solicitacaoOriginal,
                StatusOperacaoConsumo.CONSUMED
        );

        when(repository.buscarPorOperationId(operationId))
                .thenReturn(Optional.of(operacao));

        var excecao = assertThrows(
                ConflitoIdempotenciaException.class,
                () -> service.consultarOperacaoExistente(
                        solicitacaoAlterada
                )
        );

        assertEquals(operationId, excecao.getOperationId());
    }

    @Test
    void naoDeveRetornarResultadoDefinitivoDuranteProcessamento() {
        var solicitacao = criarSolicitacao(2);
        var operacao = criarOperacao(
                solicitacao,
                StatusOperacaoConsumo.PROCESSING
        );

        when(repository.buscarPorOperationId(operationId))
                .thenReturn(Optional.of(operacao));

        assertThrows(
                IllegalStateException.class,
                () -> service.consultarOperacaoExistente(solicitacao)
        );
    }
}
