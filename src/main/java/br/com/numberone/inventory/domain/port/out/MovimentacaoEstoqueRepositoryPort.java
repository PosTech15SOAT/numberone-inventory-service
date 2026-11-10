package br.com.numberone.inventory.domain.port.out;

import br.com.numberone.inventory.domain.model.MovimentacaoEstoque;

import java.util.List;
import java.util.UUID;

public interface MovimentacaoEstoqueRepositoryPort {
    default MovimentacaoEstoque salvar(MovimentacaoEstoque movimento) {
        return salvar(movimento, null);
    }

    MovimentacaoEstoque salvar(MovimentacaoEstoque movimento, UUID operationId);

    List<UUID> buscarIdsPorOperacao(UUID operationId);

    List<MovimentacaoEstoque> buscarPorItem(UUID itemId);

    List<MovimentacaoEstoque> buscarBaixas(UUID operationId);
    List<UUID> buscarIdsInversos(UUID operationId);
    MovimentacaoEstoque salvarInverso(MovimentacaoEstoque movimento, UUID operationId, UUID originalId, UUID compensationId);
}
