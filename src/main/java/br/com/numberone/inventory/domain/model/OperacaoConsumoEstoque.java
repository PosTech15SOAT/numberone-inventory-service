
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

    public OperacaoConsumoEstoque(
            UUID operationId,
            UUID serviceOrderId,
            UUID serviceOrderItemId,
            UUID responsibleUserId,
            String requestHash,
            StatusOperacaoConsumo status) {

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
                && requestHash.equalsIgnoreCase(outroHash);
    }

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
