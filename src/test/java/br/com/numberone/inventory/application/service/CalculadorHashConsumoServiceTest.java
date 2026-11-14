
package br.com.numberone.inventory.application.service;

import br.com.numberone.inventory.domain.model.SolicitacaoConsumoEstoque;
import br.com.numberone.inventory.domain.model.SolicitacaoConsumoEstoque.ItemConsumo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CalculadorHashConsumoServiceTest {

    private CalculadorHashConsumoService service;

    private UUID operationId;
    private UUID serviceOrderId;
    private UUID serviceOrderItemId;
    private UUID responsibleUserId;
    private UUID primeiroItemId;
    private UUID segundoItemId;

    @BeforeEach
    void configurar() {
        service = new CalculadorHashConsumoService();

        operationId = UUID.randomUUID();
        serviceOrderId = UUID.randomUUID();
        serviceOrderItemId = UUID.randomUUID();
        responsibleUserId = UUID.randomUUID();
        primeiroItemId = UUID.randomUUID();
        segundoItemId = UUID.randomUUID();
    }

    private SolicitacaoConsumoEstoque criarSolicitacao(
            UUID operationId,
            UUID serviceOrderId,
            UUID serviceOrderItemId,
            UUID responsibleUserId,
            List<ItemConsumo> items) {

        return new SolicitacaoConsumoEstoque(
                operationId,
                serviceOrderId,
                serviceOrderItemId,
                responsibleUserId,
                items
        );
    }

    private SolicitacaoConsumoEstoque solicitacaoPadrao() {
        return criarSolicitacao(
                operationId,
                serviceOrderId,
                serviceOrderItemId,
                responsibleUserId,
                List.of(
                        new ItemConsumo(primeiroItemId, 2),
                        new ItemConsumo(segundoItemId, 4)
                )
        );
    }

    @Test
    void deveGerarHashSha256Valido() {
        String hash = service.calcular(solicitacaoPadrao());

        assertTrue(hash.matches("[0-9a-f]{64}"));
    }

    @Test
    void deveGerarMesmoHashParaMesmaSolicitacao() {
        var solicitacao = solicitacaoPadrao();

        assertEquals(
                service.calcular(solicitacao),
                service.calcular(solicitacao)
        );
    }

    @Test
    void ordemDosItensNaoDeveAlterarHash() {
        var primeira = solicitacaoPadrao();

        var segunda = criarSolicitacao(
                operationId,
                serviceOrderId,
                serviceOrderItemId,
                responsibleUserId,
                List.of(
                        new ItemConsumo(segundoItemId, 4),
                        new ItemConsumo(primeiroItemId, 2)
                )
        );

        assertEquals(
                service.calcular(primeira),
                service.calcular(segunda)
        );
    }

    @Test
    void alterarQuantidadeDeveAlterarHash() {
        var primeira = solicitacaoPadrao();

        var segunda = criarSolicitacao(
                operationId,
                serviceOrderId,
                serviceOrderItemId,
                responsibleUserId,
                List.of(
                        new ItemConsumo(primeiroItemId, 3),
                        new ItemConsumo(segundoItemId, 4)
                )
        );

        assertNotEquals(
                service.calcular(primeira),
                service.calcular(segunda)
        );
    }

    @Test
    void alterarOperationIdDeveAlterarHash() {
        var primeira = solicitacaoPadrao();

        var segunda = criarSolicitacao(
                UUID.randomUUID(),
                serviceOrderId,
                serviceOrderItemId,
                responsibleUserId,
                primeira.items()
        );

        assertNotEquals(
                service.calcular(primeira),
                service.calcular(segunda)
        );
    }

    @Test
    void alterarIdentificadoresDeveAlterarHash() {
        var primeira = solicitacaoPadrao();

        var outraOrdem = criarSolicitacao(
                operationId,
                UUID.randomUUID(),
                serviceOrderItemId,
                responsibleUserId,
                primeira.items()
        );

        var outroItemOrdem = criarSolicitacao(
                operationId,
                serviceOrderId,
                UUID.randomUUID(),
                responsibleUserId,
                primeira.items()
        );

        var outroResponsavel = criarSolicitacao(
                operationId,
                serviceOrderId,
                serviceOrderItemId,
                UUID.randomUUID(),
                primeira.items()
        );

        String hashOriginal = service.calcular(primeira);

        assertAll(
                () -> assertNotEquals(
                        hashOriginal,
                        service.calcular(outraOrdem)
                ),
                () -> assertNotEquals(
                        hashOriginal,
                        service.calcular(outroItemOrdem)
                ),
                () -> assertNotEquals(
                        hashOriginal,
                        service.calcular(outroResponsavel)
                )
        );
    }
}
