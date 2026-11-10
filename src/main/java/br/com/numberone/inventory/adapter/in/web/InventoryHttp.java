package br.com.numberone.inventory.adapter.in.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import tools.jackson.databind.JsonNode;

import java.util.UUID;

final class InventoryHttp {
    static final String CORRELATION = "X-Correlation-Id";
    static final String ADMIN_SUBJECT = "inventory.adminSubject";
    static final String REQUEST_CORRELATION = "inventory.correlationId";

    private InventoryHttp() { }

    static UUID uuid(String raw) {
        if (raw == null || raw.length() != 36) {
            throw new IllegalArgumentException("UUID invalido.");
        }
        UUID id = UUID.fromString(raw);
        if (!id.toString().equalsIgnoreCase(raw) || id.equals(new UUID(0, 0))) {
            throw new IllegalArgumentException("UUID invalido.");
        }
        return id;
    }

    static int quantidade(JsonNode value) {
        if (value == null || !value.isIntegralNumber() || !value.canConvertToInt()) {
            throw new IllegalArgumentException("Quantidade deve ser um inteiro de 32 bits.");
        }
        return value.intValue();
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record Problem(String code, String message, UUID correlationId, UUID operationId) { }
}
