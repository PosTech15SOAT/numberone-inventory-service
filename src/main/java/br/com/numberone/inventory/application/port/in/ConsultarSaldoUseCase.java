package br.com.numberone.inventory.application.port.in;

import java.util.UUID;
public interface ConsultarSaldoUseCase {
    int consultar(UUID itemEstoqueId);
}
