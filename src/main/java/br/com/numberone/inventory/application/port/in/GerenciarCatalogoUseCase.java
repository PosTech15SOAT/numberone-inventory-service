package br.com.numberone.inventory.application.port.in;

import br.com.numberone.inventory.domain.model.*;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
public interface GerenciarCatalogoUseCase {
    ItemEstoque cadastrar(ItemEstoque item, int saldoInicial, UUID autor);
    Optional<ItemEstoque> consultar(UUID itemId);
    List<ItemEstoque> listar();
    MovimentacaoEstoque entrada(UUID itemId, int quantidade, OrigemMovimentacao origem, UUID referencia, String observacao, UUID autor);
    List<MovimentacaoEstoque> consultarMovimentos(UUID itemId);
}
