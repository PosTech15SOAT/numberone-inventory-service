package br.com.numberone.inventory.application.port.in;

import br.com.numberone.inventory.domain.model.*;
import java.util.Optional;
import java.util.UUID;
public interface ConsumirEstoqueUseCase {
    ResultadoConsumoEstoque consumir(SolicitacaoConsumoEstoque solicitacao);
    Optional<ResultadoConsumoEstoque> consultar(UUID operationId);
    ResultadoConsumoEstoque consultarOperacaoExistente(SolicitacaoConsumoEstoque solicitacao);
}
