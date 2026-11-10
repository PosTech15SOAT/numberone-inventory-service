
package br.com.numberone.inventory.application.service;

import java.util.UUID;

public class ConflitoIdempotenciaException
        extends RuntimeException {

    private final UUID operationId;

    public ConflitoIdempotenciaException(UUID operationId) {
        super(
                "O operationId " + operationId
                + " ja foi utilizado com dados diferentes."
        );

        this.operationId = operationId;
    }

    public UUID getOperationId() {
        return operationId;
    }
}
