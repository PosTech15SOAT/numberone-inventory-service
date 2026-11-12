package br.com.numberone.inventory.application.service;

import br.com.numberone.inventory.application.port.in.ConsumirEstoqueUseCase;

import br.com.numberone.inventory.domain.model.*;
import br.com.numberone.inventory.application.port.out.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
public class ConsumirEstoqueService implements ConsumirEstoqueUseCase {
    private final OperacaoConsumoRepositoryPort operacaoRepository;
    private final CalculadorHashConsumoService calculadorHash;
    private final ItemEstoqueRepositoryPort itemRepository;
    private final OperacaoConsumoItemRepositoryPort operacaoItemRepository;
    private final MovimentacaoEstoqueRepositoryPort movimentoRepository;

    public ConsumirEstoqueService(OperacaoConsumoRepositoryPort operacaoRepository,
                                 CalculadorHashConsumoService calculadorHash,
                                 ItemEstoqueRepositoryPort itemRepository,
                                 OperacaoConsumoItemRepositoryPort operacaoItemRepository,
                                 MovimentacaoEstoqueRepositoryPort movimentoRepository) {
        this.operacaoRepository = operacaoRepository;
        this.calculadorHash = calculadorHash;
        this.itemRepository = itemRepository;
        this.operacaoItemRepository = operacaoItemRepository;
        this.movimentoRepository = movimentoRepository;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ResultadoConsumoEstoque consumir(SolicitacaoConsumoEstoque solicitacao) {
        UUID operationId = solicitacao.operationId();
        String requestHash = calculadorHash.calcular(solicitacao);

        operacaoRepository.bloquearOperacao(operationId);
        var existente = operacaoExistente(operationId, requestHash);
        if (existente.isPresent()) {
            return resultado(existente.get());
        }

        List<ItemBloqueado> lote = new ArrayList<>();
        for (var linha : solicitacao.items()) {
            ItemEstoque item = itemRepository.buscarPorIdParaAtualizacao(linha.inventoryItemId())
                    .filter(ItemEstoque::isAtivo)
                    .orElseThrow(() -> new ItemIndisponivelException(linha.inventoryItemId()));
            lote.add(new ItemBloqueado(item, linha.quantity()));
        }

        var operacao = new OperacaoConsumoEstoque(operationId, solicitacao.serviceOrderId(),
                solicitacao.serviceOrderItemId(), solicitacao.responsibleUserId(), requestHash,
                StatusOperacaoConsumo.PROCESSING);
        operacaoRepository.salvar(operacao);
        operacaoItemRepository.salvarItens(operationId, solicitacao.items());

        if (lote.stream().anyMatch(linha -> linha.item().getQuantidadeEstoque() < linha.quantidade())) {
            operacao.marcarEstoqueInsuficiente();
            operacaoRepository.salvar(operacao);
            return ResultadoConsumoEstoque.from(operacao);
        }

        var agora = LocalDateTime.now(ZoneOffset.UTC);
        List<UUID> movimentos = new ArrayList<>();
        for (var linha : lote) {
            var item = linha.item();
            int antes = item.getQuantidadeEstoque();
            item.baixarEstoque(linha.quantidade());
            itemRepository.salvar(item);
            var movimento = new MovimentacaoEstoque(UUID.randomUUID(), item.getId(), TipoMovimentacao.BAIXA,
                    OrigemMovimentacao.ORDEM_SERVICO, solicitacao.serviceOrderItemId(), antes,
                    item.getQuantidadeEstoque(), null, solicitacao.responsibleUserId(), agora);
            movimentos.add(movimentoRepository.salvar(movimento, operationId).id());
        }
        operacao.marcarComoConsumida();
        operacaoRepository.salvar(operacao);
        return new ResultadoConsumoEstoque(operationId, operacao.getStatus(), movimentos);
    }

    @Transactional(readOnly = true)
    public Optional<ResultadoConsumoEstoque> consultar(UUID operationId) {
        return operacaoRepository.buscarPorOperationId(operationId).map(this::resultado);
    }

    @Transactional(readOnly = true)
    public ResultadoConsumoEstoque consultarOperacaoExistente(SolicitacaoConsumoEstoque solicitacao) {
        Objects.requireNonNull(solicitacao, "A solicitacao de consumo e obrigatoria.");
        return operacaoExistente(solicitacao.operationId(), calculadorHash.calcular(solicitacao))
                .map(operacao -> {
                    if (operacao.getStatus() == StatusOperacaoConsumo.PROCESSING) {
                        throw new IllegalStateException("A operacao ainda esta em processamento.");
                    }
                    return resultado(operacao);
                }).orElse(null);
    }

    private Optional<OperacaoConsumoEstoque> operacaoExistente(UUID id, String hash) {
        return operacaoRepository.buscarPorOperationId(id).map(operacao -> {
            if (operacao.getStatus() == StatusOperacaoConsumo.CLOSED || operacao.getStatus() == StatusOperacaoConsumo.REVERSED) {
                throw new ConflitoCompensacaoException("ATTEMPT_" + operacao.getStatus(), id, operacao.getCompensationId());
            }
            if (!operacao.possuiMesmoRequestHash(hash)) {
                throw new ConflitoIdempotenciaException(id);
            }
            return operacao;
        });
    }

    private ResultadoConsumoEstoque resultado(OperacaoConsumoEstoque operacao) {
        if (operacao.getStatus() == StatusOperacaoConsumo.CONSUMED || operacao.getStatus() == StatusOperacaoConsumo.REVERSED) {
            return new ResultadoConsumoEstoque(operacao.getOperationId(), operacao.getStatus(),
                    operacao.getStatus() == StatusOperacaoConsumo.REVERSED
                            ? movimentoRepository.buscarIdsInversos(operacao.getOperationId())
                            : movimentoRepository.buscarIdsPorOperacao(operacao.getOperationId()), operacao.getCompensationId());
        }
        return ResultadoConsumoEstoque.from(operacao);
    }

    private record ItemBloqueado(ItemEstoque item, int quantidade) { }
}
