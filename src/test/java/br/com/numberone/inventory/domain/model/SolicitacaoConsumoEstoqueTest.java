
package br.com.numberone.inventory.domain.model;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SolicitacaoConsumoEstoqueTest {

    private final UUID operationId = UUID.randomUUID();
    private final UUID serviceOrderId = UUID.randomUUID();
    private final UUID serviceOrderItemId = UUID.randomUUID();
    private final UUID responsibleUserId = UUID.randomUUID();
    private final UUID inventoryItemId = UUID.randomUUID();

    private SolicitacaoConsumoEstoque criarSolicitacao(
            List<SolicitacaoConsumoEstoque.ItemConsumo> items) {

        return new SolicitacaoConsumoEstoque(
                operationId,
                serviceOrderId,
                serviceOrderItemId,
                responsibleUserId,
                items
        );
    }

    @Test
    void deveCriarSolicitacaoValida() {
        var solicitacao = criarSolicitacao(List.of(
                new SolicitacaoConsumoEstoque.ItemConsumo(
                        inventoryItemId, 2
                )
        ));

        assertEquals(operationId, solicitacao.operationId());
        assertEquals(serviceOrderId, solicitacao.serviceOrderId());
        assertEquals(serviceOrderItemId, solicitacao.serviceOrderItemId());
        assertEquals(responsibleUserId, solicitacao.responsibleUserId());
        assertEquals(1, solicitacao.items().size());
        assertEquals(2, solicitacao.items().get(0).quantity());
    }

    @Test
    void deveAceitarMultiplosItensDiferentes() {
        var solicitacao = criarSolicitacao(List.of(
                new SolicitacaoConsumoEstoque.ItemConsumo(
                        inventoryItemId, 2
                ),
                new SolicitacaoConsumoEstoque.ItemConsumo(
                        UUID.randomUUID(), 3
                )
        ));

        assertEquals(2, solicitacao.items().size());
    }

    @Test
    void naoDeveAceitarListaVazia() {
        assertThrows(IllegalArgumentException.class,
                () -> criarSolicitacao(List.of()));
    }

    @Test
    void naoDeveAceitarListaNula() {
        assertThrows(IllegalArgumentException.class,
                () -> criarSolicitacao(null));
    }

    @Test
    void naoDeveAceitarItemNulo() {
        assertThrows(NullPointerException.class,
                () -> criarSolicitacao(
                        java.util.Arrays.asList(
                                (SolicitacaoConsumoEstoque.ItemConsumo) null
                        )
                ));
    }

    @Test
    void naoDeveAceitarItensDuplicados() {
        assertThrows(IllegalArgumentException.class,
                () -> criarSolicitacao(List.of(
                        new SolicitacaoConsumoEstoque.ItemConsumo(
                                inventoryItemId, 2
                        ),
                        new SolicitacaoConsumoEstoque.ItemConsumo(
                                inventoryItemId, 3
                        )
                )));
    }

    @Test
    void naoDeveAceitarQuantidadeZero() {
        assertThrows(IllegalArgumentException.class,
                () -> new SolicitacaoConsumoEstoque.ItemConsumo(
                        inventoryItemId, 0
                ));
    }

    @Test
    void naoDeveAceitarQuantidadeNegativa() {
        assertThrows(IllegalArgumentException.class,
                () -> new SolicitacaoConsumoEstoque.ItemConsumo(
                        inventoryItemId, -1
                ));
    }

    @Test
    void naoDeveAceitarIdentificadorDeItemNulo() {
        assertThrows(IllegalArgumentException.class,
                () -> new SolicitacaoConsumoEstoque.ItemConsumo(
                        null, 1
                ));
    }

    @Test
    void naoDeveAceitarIdentificadoresObrigatoriosNulos() {
        var item = new SolicitacaoConsumoEstoque.ItemConsumo(
                inventoryItemId, 1
        );

        assertAll(
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new SolicitacaoConsumoEstoque(
                                null, serviceOrderId,
                                serviceOrderItemId, responsibleUserId,
                                List.of(item)
                        )),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new SolicitacaoConsumoEstoque(
                                operationId, null,
                                serviceOrderItemId, responsibleUserId,
                                List.of(item)
                        )),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new SolicitacaoConsumoEstoque(
                                operationId, serviceOrderId,
                                null, responsibleUserId,
                                List.of(item)
                        )),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new SolicitacaoConsumoEstoque(
                                operationId, serviceOrderId,
                                serviceOrderItemId, null,
                                List.of(item)
                        ))
        );
    }

    @Test
    void naoDevePermitirAlterarListaDeItens() {
        var itensOriginais = new ArrayList<
                SolicitacaoConsumoEstoque.ItemConsumo>();

        itensOriginais.add(
                new SolicitacaoConsumoEstoque.ItemConsumo(
                        inventoryItemId, 2
                )
        );

        var solicitacao = criarSolicitacao(itensOriginais);

        itensOriginais.clear();

        assertEquals(1, solicitacao.items().size());

        assertThrows(UnsupportedOperationException.class,
                () -> solicitacao.items().add(
                        new SolicitacaoConsumoEstoque.ItemConsumo(
                                UUID.randomUUID(), 1
                        )
                ));
    }
}
