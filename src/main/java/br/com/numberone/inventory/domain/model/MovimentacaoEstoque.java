
package br.com.numberone.inventory.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;

public record MovimentacaoEstoque(
        UUID id,
        UUID itemEstoqueId,
        TipoMovimentacao tipoMovimentacao,
        OrigemMovimentacao origemMovimentacao,
        UUID referenciaOrigemId,
        int quantidadeAntes,
        int quantidadeDepois,
        String observacao,
        UUID usuarioResponsavelId,
        LocalDateTime createdAt
) {
    public MovimentacaoEstoque {
        if (itemEstoqueId == null) {
            throw new IllegalArgumentException("Item obrigatorio");
        }

        if (tipoMovimentacao == null || origemMovimentacao == null) {
            throw new IllegalArgumentException(
                    "Tipo e origem da movimentacao obrigatorios"
            );
        }

        if (usuarioResponsavelId == null) {
            throw new IllegalArgumentException("Usuario obrigatorio");
        }

        if (quantidadeAntes < 0 || quantidadeDepois < 0) {
            throw new IllegalArgumentException(
                    "Quantidades nao podem ser negativas"
            );
        }

        if (tipoMovimentacao == TipoMovimentacao.ENTRADA
                && quantidadeDepois <= quantidadeAntes) {
            throw new IllegalArgumentException(
                    "Entrada deve aumentar o estoque"
            );
        }

        if (tipoMovimentacao == TipoMovimentacao.BAIXA
                && quantidadeDepois >= quantidadeAntes) {
            throw new IllegalArgumentException(
                    "Baixa deve diminuir o estoque"
            );
        }
    }
}
