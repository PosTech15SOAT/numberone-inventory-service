
package br.com.numberone.inventory.domain.model;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class OperacaoConsumoEstoqueTest {

    private static final String HASH =
            "a".repeat(64);

    private OperacaoConsumoEstoque criarOperacao(
            StatusOperacaoConsumo status) {

        return new OperacaoConsumoEstoque(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                HASH,
                status
        );
    }

    @Test
    void deveCriarOperacaoEmProcessamento() {
        var operacao = criarOperacao(
                StatusOperacaoConsumo.PROCESSING);

        assertEquals(
                StatusOperacaoConsumo.PROCESSING,
                operacao.getStatus());

        assertEquals(HASH, operacao.getRequestHash());
    }

    @Test
    void deveMarcarOperacaoComoConsumida() {
        var operacao = criarOperacao(
                StatusOperacaoConsumo.PROCESSING);

        operacao.marcarComoConsumida();

        assertEquals(
                StatusOperacaoConsumo.CONSUMED,
                operacao.getStatus());
    }

    @Test
    void deveMarcarEstoqueInsuficiente() {
        var operacao = criarOperacao(
                StatusOperacaoConsumo.PROCESSING);

        operacao.marcarEstoqueInsuficiente();

        assertEquals(
                StatusOperacaoConsumo.INSUFFICIENT_STOCK,
                operacao.getStatus());
    }

    @Test
    void naoDeveConsumirOperacaoJaConsumida() {
        var operacao = criarOperacao(
                StatusOperacaoConsumo.CONSUMED);

        assertThrows(
                IllegalStateException.class,
                operacao::marcarComoConsumida);
    }

    @Test
    void naoDeveAlterarResultadoDeEstoqueInsuficiente() {
        var operacao = criarOperacao(
                StatusOperacaoConsumo.INSUFFICIENT_STOCK);

        assertThrows(
                IllegalStateException.class,
                operacao::marcarComoConsumida);
    }

    @Test
    void naoDeveMarcarInsuficienciaAposConsumo() {
        var operacao = criarOperacao(
                StatusOperacaoConsumo.CONSUMED);

        assertThrows(
                IllegalStateException.class,
                operacao::marcarEstoqueInsuficiente);
    }

    @Test
    void deveReconhecerMesmoRequestHash() {
        var operacao = criarOperacao(
                StatusOperacaoConsumo.CONSUMED);

        assertTrue(operacao.possuiMesmoRequestHash(HASH));
        assertTrue(operacao.possuiMesmoRequestHash(
                HASH.toUpperCase()));
    }

    @Test
    void deveRejeitarRequestHashDiferente() {
        var operacao = criarOperacao(
                StatusOperacaoConsumo.CONSUMED);

        assertFalse(operacao.possuiMesmoRequestHash(
                "b".repeat(64)));

        assertFalse(operacao.possuiMesmoRequestHash(null));
    }

    @Test
    void naoDeveAceitarRequestHashInvalido() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new OperacaoConsumoEstoque(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "hash-invalido",
                        StatusOperacaoConsumo.PROCESSING
                ));
    }

    @Test
    void naoDeveAceitarIdentificadoresNulos() {
        assertAll(
                () -> assertThrows(
                        NullPointerException.class,
                        () -> new OperacaoConsumoEstoque(
                                null,
                                UUID.randomUUID(),
                                UUID.randomUUID(),
                                UUID.randomUUID(),
                                HASH,
                                StatusOperacaoConsumo.PROCESSING
                        )),
                () -> assertThrows(
                        NullPointerException.class,
                        () -> new OperacaoConsumoEstoque(
                                UUID.randomUUID(),
                                null,
                                UUID.randomUUID(),
                                UUID.randomUUID(),
                                HASH,
                                StatusOperacaoConsumo.PROCESSING
                        )),
                () -> assertThrows(
                        NullPointerException.class,
                        () -> new OperacaoConsumoEstoque(
                                UUID.randomUUID(),
                                UUID.randomUUID(),
                                null,
                                UUID.randomUUID(),
                                HASH,
                                StatusOperacaoConsumo.PROCESSING
                        )),
                () -> assertThrows(
                        NullPointerException.class,
                        () -> new OperacaoConsumoEstoque(
                                UUID.randomUUID(),
                                UUID.randomUUID(),
                                UUID.randomUUID(),
                                null,
                                HASH,
                                StatusOperacaoConsumo.PROCESSING
                        ))
        );
    }

    @Test
    void naoDeveAceitarStatusNulo() {
        assertThrows(
                NullPointerException.class,
                () -> new OperacaoConsumoEstoque(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        HASH,
                        null
                ));
    }
}
