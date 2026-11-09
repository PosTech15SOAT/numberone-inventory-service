
package br.com.numberone.inventory.domain.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class MovimentacaoEstoqueTest {

    private MovimentacaoEstoque criarMovimentacao(
            TipoMovimentacao tipo,
            int antes,
            int depois) {

        return new MovimentacaoEstoque(
                UUID.randomUUID(),
                UUID.randomUUID(),
                tipo,
                OrigemMovimentacao.MANUAL,
                null,
                antes,
                depois,
                "Movimentacao de teste",
                UUID.randomUUID(),
                LocalDateTime.now()
        );
    }

    @Test
    void deveRegistrarEntradaDeEstoque() {
        MovimentacaoEstoque movimentacao =
                criarMovimentacao(TipoMovimentacao.ENTRADA, 10, 15);

        assertEquals(10, movimentacao.quantidadeAntes());
        assertEquals(15, movimentacao.quantidadeDepois());
    }

    @Test
    void deveRegistrarBaixaDeEstoque() {
        MovimentacaoEstoque movimentacao =
                criarMovimentacao(TipoMovimentacao.BAIXA, 10, 7);

        assertEquals(7, movimentacao.quantidadeDepois());
    }

    @Test
    void naoDevePermitirEntradaSemAumento() {
        assertThrows(
                IllegalArgumentException.class,
                () -> criarMovimentacao(TipoMovimentacao.ENTRADA, 10, 10)
        );
    }

    @Test
    void naoDevePermitirBaixaSemReducao() {
        assertThrows(
                IllegalArgumentException.class,
                () -> criarMovimentacao(TipoMovimentacao.BAIXA, 10, 12)
        );
    }

    @Test
    void naoDevePermitirSaldoNegativo() {
        assertThrows(
                IllegalArgumentException.class,
                () -> criarMovimentacao(TipoMovimentacao.AJUSTE, 10, -1)
        );
    }

    @Test
    void devePermitirAjusteDeEstoque() {
        MovimentacaoEstoque movimentacao =
                criarMovimentacao(TipoMovimentacao.AJUSTE, 10, 8);

        assertEquals(TipoMovimentacao.AJUSTE, movimentacao.tipoMovimentacao());
        assertEquals(8, movimentacao.quantidadeDepois());
    }
}
