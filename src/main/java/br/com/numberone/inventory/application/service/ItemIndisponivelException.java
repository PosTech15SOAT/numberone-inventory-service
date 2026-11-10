package br.com.numberone.inventory.application.service;

import java.util.UUID;

public class ItemIndisponivelException extends RuntimeException {
    public ItemIndisponivelException(UUID itemId) {
        super("Item de estoque ausente ou indisponivel: " + itemId);
    }
}
