package br.com.numberone.inventory.application.service;

import br.com.numberone.inventory.application.port.in.GerenciarCatalogoUseCase;

import br.com.numberone.inventory.domain.model.ItemEstoque;
import br.com.numberone.inventory.domain.model.MovimentacaoEstoque;
import br.com.numberone.inventory.domain.model.OrigemMovimentacao;
import br.com.numberone.inventory.domain.model.TipoMovimentacao;
import br.com.numberone.inventory.application.port.out.ItemEstoqueRepositoryPort;
import br.com.numberone.inventory.application.port.out.MovimentacaoEstoqueRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class CatalogoEstoqueService implements GerenciarCatalogoUseCase {
    private final ItemEstoqueRepositoryPort itens;
    private final MovimentacaoEstoqueRepositoryPort movimentos;

    public CatalogoEstoqueService(ItemEstoqueRepositoryPort itens, MovimentacaoEstoqueRepositoryPort movimentos) {
        this.itens = itens;
        this.movimentos = movimentos;
    }

    @Transactional
    public ItemEstoque cadastrar(ItemEstoque item, int saldoInicial, UUID autor) {
        if (saldoInicial < 0) {
            throw new IllegalArgumentException("Saldo inicial nao pode ser negativo.");
        }
        var cadastrado = itens.salvar(item);
        if (saldoInicial > 0) {
            registrarEntrada(cadastrado, saldoInicial, OrigemMovimentacao.MANUAL, null, null, autor);
        }
        return cadastrado;
    }

    @Transactional(readOnly = true)
    public Optional<ItemEstoque> consultar(UUID itemId) {
        return itens.buscarPorId(itemId);
    }

    @Transactional(readOnly = true)
    public List<ItemEstoque> listar() {
        return itens.listarAtivos();
    }

    @Transactional
    public MovimentacaoEstoque entrada(UUID itemId, int quantidade, OrigemMovimentacao origem,
                                       UUID referencia, String observacao, UUID autor) {
        var item = itens.buscarPorIdParaAtualizacao(itemId)
                .orElseThrow(() -> new ItemIndisponivelException(itemId));
        return registrarEntrada(item, quantidade, origem, referencia, observacao, autor);
    }

    private MovimentacaoEstoque registrarEntrada(ItemEstoque item, int quantidade, OrigemMovimentacao origem,
                                                  UUID referencia, String observacao, UUID autor) {
        int antes = item.getQuantidadeEstoque();
        item.adicionarEstoque(quantidade);
        itens.salvar(item);
        return movimentos.salvar(new MovimentacaoEstoque(UUID.randomUUID(), item.getId(), TipoMovimentacao.ENTRADA,
                origem, referencia, antes, item.getQuantidadeEstoque(), observacao, autor, LocalDateTime.now(ZoneOffset.UTC)));
    }

    @Transactional(readOnly = true)
    public List<MovimentacaoEstoque> consultarMovimentos(UUID itemId) {
        if (itens.buscarPorId(itemId).isEmpty()) {
            throw new ItemIndisponivelException(itemId);
        }
        return movimentos.buscarPorItem(itemId);
    }
}
