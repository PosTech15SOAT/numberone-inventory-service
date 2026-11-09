
package br.com.numberone.inventory.domain.port.out;

import br.com.numberone.inventory.domain.model.MovimentacaoEstoque;

public interface MovimentacaoEstoqueRepositoryPort {

    MovimentacaoEstoque salvar(
            MovimentacaoEstoque movimentacao
    );
}
