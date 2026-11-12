package br.com.numberone.inventory.application.port.out;

import br.com.numberone.inventory.domain.model.*;
import java.util.Optional;
import java.util.UUID;

public interface CompensacaoRepositoryPort {
    record Registro(UUID operationId, String requestHash, ResultadoConsumoEstoque resultado) { }
    void bloquearCompensacao(UUID compensationId);
    Optional<Registro> buscar(UUID compensationId);
    void salvar(UUID operationId, SolicitacaoCompensacao pedido, String hash, ResultadoConsumoEstoque resultado);
}
