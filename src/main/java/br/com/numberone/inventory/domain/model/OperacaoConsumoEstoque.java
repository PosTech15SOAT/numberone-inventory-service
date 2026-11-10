
package br.com.numberone.inventory.domain.model;

import java.util.Objects;
import java.util.UUID;

public class OperacaoConsumoEstoque {

    private final UUID operationId;
    private final UUID serviceOrderId;
    private final UUID serviceOrderItemId;
    private final UUID responsibleUserId;
    private final String requestHash;

    private StatusOperacaoConsumo status;
    private UUID compensationId;

    public OperacaoConsumoEstoque(
            UUID operationId,
            UUID serviceOrderId,
            UUID serviceOrderItemId,
            UUID responsibleUserId,
            String requestHash,
            StatusOperacaoConsumo status) {
        this(operationId, serviceOrderId, serviceOrderItemId, responsibleUserId, requestHash, status, null);
    }

    public OperacaoConsumoEstoque(UUID operationId, UUID serviceOrderId, UUID serviceOrderItemId,
                                  UUID responsibleUserId, String requestHash, StatusOperacaoConsumo status,
                                  UUID compensationId) {
        if (status == StatusOperacaoConsumo.CLOSED) {
            if (serviceOrderId != null || serviceOrderItemId != null || responsibleUserId != null || requestHash != null) {
                throw new IllegalArgumentException("Fechamento sem baixa nao possui pedido de consumo.");
            }
            this.operationId = Objects.requireNonNull(operationId);
            this.serviceOrderId = null;
            this.serviceOrderItemId = null;
            this.responsibleUserId = null;
            this.requestHash = null;
            this.status = status;
            this.compensationId = Objects.requireNonNull(compensationId);
            return;
        }
        if (status == StatusOperacaoConsumo.REVERSED) {
            Objects.requireNonNull(compensationId);
        } else if (compensationId != null) {
            throw new IllegalArgumentException("Compensacao exige estado terminal de compensacao.");
        }
        this.compensationId = compensationId;

        this.operationId = Objects.requireNonNull(
                operationId, "operationId obrigatorio");

        this.serviceOrderId = Objects.requireNonNull(
                serviceOrderId, "serviceOrderId obrigatorio");

        this.serviceOrderItemId = Objects.requireNonNull(
                serviceOrderItemId, "serviceOrderItemId obrigatorio");

        this.responsibleUserId = Objects.requireNonNull(
                responsibleUserId, "responsibleUserId obrigatorio");

        if (requestHash == null
                || !requestHash.matches("[0-9a-fA-F]{64}")) {
            throw new IllegalArgumentException(
                    "requestHash deve conter 64 caracteres hexadecimais.");
        }

        this.requestHash = requestHash.toLowerCase(
                java.util.Locale.ROOT);

        this.status = Objects.requireNonNull(
                status, "status obrigatorio");
    }

    public void marcarComoConsumida() {
        validarProcessamento();
        this.status = StatusOperacaoConsumo.CONSUMED;
    }

    public void marcarEstoqueInsuficiente() {
        validarProcessamento();
        this.status = StatusOperacaoConsumo.INSUFFICIENT_STOCK;
    }

    private void validarProcessamento() {
        if (status != StatusOperacaoConsumo.PROCESSING) {
            throw new IllegalStateException(
                    "A operacao ja possui um resultado definitivo.");
        }
    }

    public boolean possuiMesmoRequestHash(String outroHash) {
        return outroHash != null
                && requestHash != null && requestHash.equalsIgnoreCase(outroHash);
    }

    public static OperacaoConsumoEstoque fechar(UUID operationId, UUID compensationId) {
        return new OperacaoConsumoEstoque(operationId, null, null, null, null, StatusOperacaoConsumo.CLOSED, compensationId);
    }

    public void reverter(UUID compensationId) {
        if (status != StatusOperacaoConsumo.CONSUMED) {
            throw new IllegalStateException("Somente consumo confirmado pode ser revertido.");
        }
        this.compensationId = Objects.requireNonNull(compensationId);
        this.status = StatusOperacaoConsumo.REVERSED;
    }

    public UUID getCompensationId() { return compensationId; }

    public UUID getOperationId() {
        return operationId;
    }

    public UUID getServiceOrderId() {
        return serviceOrderId;
    }

    public UUID getServiceOrderItemId() {
        return serviceOrderItemId;
    }

    public UUID getResponsibleUserId() {
        return responsibleUserId;
    }

    public String getRequestHash() {
        return requestHash;
    }

    public StatusOperacaoConsumo getStatus() {
        return status;
    }
}
