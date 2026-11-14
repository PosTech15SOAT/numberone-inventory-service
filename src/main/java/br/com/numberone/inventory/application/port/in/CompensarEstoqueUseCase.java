package br.com.numberone.inventory.application.port.in;

import br.com.numberone.inventory.domain.model.*;
import java.util.UUID;
public interface CompensarEstoqueUseCase {
    ResultadoConsumoEstoque compensar(UUID operationId, SolicitacaoCompensacao pedido);
}
