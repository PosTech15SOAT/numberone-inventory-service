
package br.com.numberone.inventory.adapter.out.persistence;

import br.com.numberone.inventory.domain.model.OperacaoConsumoEstoque;
import br.com.numberone.inventory.domain.model.StatusOperacaoConsumo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Entity
@Table(name = "operacao_consumo_estoque")
public class OperacaoConsumoEntity {

    @Id
    @Column(name = "operation_id", nullable = false)
    private UUID operationId;

    @Column(name = "ordem_servico_id", nullable = false)
    private UUID serviceOrderId;

    @Column(name = "item_ordem_servico_id", nullable = false)
    private UUID serviceOrderItemId;

    @Column(name = "usuario_responsavel_id", nullable = false)
    private UUID responsibleUserId;

    @Column(name = "request_hash", nullable = false, length = 64)
    private String requestHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private StatusOperacaoConsumo status;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected OperacaoConsumoEntity() {
    }

    public static OperacaoConsumoEntity fromDomain(
            OperacaoConsumoEstoque operacao) {

        OperacaoConsumoEntity entity =
                new OperacaoConsumoEntity();

        entity.operationId = operacao.getOperationId();
        entity.serviceOrderId = operacao.getServiceOrderId();
        entity.serviceOrderItemId =
                operacao.getServiceOrderItemId();
        entity.responsibleUserId =
                operacao.getResponsibleUserId();
        entity.requestHash = operacao.getRequestHash();
        entity.status = operacao.getStatus();

        OffsetDateTime agora =
                OffsetDateTime.now(ZoneOffset.UTC);

        entity.createdAt = agora;
        entity.updatedAt = agora;

        return entity;
    }

    public OperacaoConsumoEstoque toDomain() {
        return new OperacaoConsumoEstoque(
                operationId,
                serviceOrderId,
                serviceOrderItemId,
                responsibleUserId,
                requestHash,
                status
        );
    }


    public void atualizarStatus(
        StatusOperacaoConsumo novoStatus) {

       if (novoStatus == null) {
           throw new IllegalArgumentException(
                   "O status da operacao e obrigatorio."
           );
        }

        if (this.status != StatusOperacaoConsumo.PROCESSING) {
            throw new IllegalStateException(
                    "Nao e permitido alterar uma operacao finalizada."
            );
        }

        if (novoStatus == StatusOperacaoConsumo.PROCESSING) {
            throw new IllegalArgumentException(
                    "O novo status deve ser definitivo."
            );
        }

        this.status = novoStatus;
        this.updatedAt =
                OffsetDateTime.now(ZoneOffset.UTC);
    }

    public UUID getOperationId() {
        return operationId;
    }

    public StatusOperacaoConsumo getStatus() {
        return status;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
