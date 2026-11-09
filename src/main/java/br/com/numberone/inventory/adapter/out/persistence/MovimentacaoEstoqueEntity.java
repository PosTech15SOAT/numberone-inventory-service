
package br.com.numberone.inventory.adapter.out.persistence;

import br.com.numberone.inventory.domain.model.MovimentacaoEstoque;
import br.com.numberone.inventory.domain.model.OrigemMovimentacao;
import br.com.numberone.inventory.domain.model.TipoMovimentacao;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "movimentacao_estoque")
public class MovimentacaoEstoqueEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "id_item_estoque", nullable = false)
    private UUID itemEstoqueId;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_movimentacao", nullable = false)
    private TipoMovimentacao tipoMovimentacao;

    @Enumerated(EnumType.STRING)
    @Column(name = "origem_movimentacao", nullable = false)
    private OrigemMovimentacao origemMovimentacao;

    @Column(name = "referencia_origem_id")
    private UUID referenciaOrigemId;

    @Column(name = "quantidade_antes", nullable = false)
    private int quantidadeAntes;

    @Column(name = "quantidade_depois", nullable = false)
    private int quantidadeDepois;

    @Column(name = "observacao", length = 500)
    private String observacao;

    @Column(name = "usuario_responsavel_id", nullable = false)
    private UUID usuarioResponsavelId;

    @Column(name = "operation_id")
    private UUID operationId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected MovimentacaoEstoqueEntity() {
    }

    public static MovimentacaoEstoqueEntity fromDomain(
            MovimentacaoEstoque movimentacao,
            UUID operationId) {

        if (movimentacao == null) {
            throw new IllegalArgumentException(
                    "A movimentacao e obrigatoria."
            );
        }

        MovimentacaoEstoqueEntity entity =
                new MovimentacaoEstoqueEntity();

        entity.id = movimentacao.id();
        entity.itemEstoqueId = movimentacao.itemEstoqueId();
        entity.tipoMovimentacao = movimentacao.tipoMovimentacao();
        entity.origemMovimentacao = movimentacao.origemMovimentacao();
        entity.referenciaOrigemId = movimentacao.referenciaOrigemId();
        entity.quantidadeAntes = movimentacao.quantidadeAntes();
        entity.quantidadeDepois = movimentacao.quantidadeDepois();
        entity.observacao = movimentacao.observacao();
        entity.usuarioResponsavelId = movimentacao.usuarioResponsavelId();
        entity.operationId = operationId;
        entity.createdAt = movimentacao.createdAt();

        return entity;
    }

    public MovimentacaoEstoque toDomain() {
        return new MovimentacaoEstoque(
                id,
                itemEstoqueId,
                tipoMovimentacao,
                origemMovimentacao,
                referenciaOrigemId,
                quantidadeAntes,
                quantidadeDepois,
                observacao,
                usuarioResponsavelId,
                createdAt
        );
    }

    public UUID getId() {
        return id;
    }

    public UUID getOperationId() {
        return operationId;
    }
}
