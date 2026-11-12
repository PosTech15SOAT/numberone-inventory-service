package br.com.numberone.inventory.application.service;

import java.util.UUID;

public class ConflitoCompensacaoException extends RuntimeException {
    private final UUID operationId;
    private final UUID compensationId;

    public ConflitoCompensacaoException(String code, UUID operationId, UUID compensationId) {
        super(code);
        this.operationId = operationId;
        this.compensationId = compensationId;
    }

    public UUID getOperationId() { return operationId; }
    public UUID getCompensationId() { return compensationId; }
}
