package br.com.numberone.inventory.application.service;

import br.com.numberone.inventory.domain.model.*;
import br.com.numberone.inventory.domain.port.out.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.*;
import java.util.*;

@Service
public class CompensarEstoqueService {
    private final OperacaoConsumoRepositoryPort operacoes;
    private final CompensacaoRepositoryPort compensacoes;
    private final MovimentacaoEstoqueRepositoryPort movimentos;
    private final ItemEstoqueRepositoryPort itens;
    private final ObjectMapper json;

    public CompensarEstoqueService(OperacaoConsumoRepositoryPort operacoes, CompensacaoRepositoryPort compensacoes,
            MovimentacaoEstoqueRepositoryPort movimentos, ItemEstoqueRepositoryPort itens, ObjectMapper json) {
        this.operacoes = operacoes; this.compensacoes = compensacoes; this.movimentos = movimentos;
        this.itens = itens; this.json = json;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ResultadoConsumoEstoque compensar(UUID operationId, SolicitacaoCompensacao pedido) {
        UUID key = Objects.requireNonNull(pedido.compensationId());
        String hash = hash(pedido);
        // Global compensation identity first; operation lock is EXACTLY the one used by CT-05.
        compensacoes.bloquearCompensacao(key);
        operacoes.bloquearOperacao(operationId);
        var replay = compensacoes.buscar(key);
        if (replay.isPresent()) {
            if (!operationId.equals(replay.get().operationId()) || !hash.equals(replay.get().requestHash())) {
                throw conflito("IDEMPOTENCY_CONFLICT", operationId, key);
            }
            return replay.get().resultado();
        }
        var existente = operacoes.buscarPorOperationId(operationId);
        if (existente.isPresent() && Set.of(StatusOperacaoConsumo.CLOSED, StatusOperacaoConsumo.REVERSED)
                .contains(existente.get().getStatus())) {
            throw conflito("IDEMPOTENCY_CONFLICT", operationId, key);
        }
        // A confirmed refusal has no material effect. Preserve REJECTED and audit resolution without credit.
        if (existente.isPresent() && existente.get().getStatus() == StatusOperacaoConsumo.INSUFFICIENT_STOCK) {
            var resultado = ResultadoConsumoEstoque.from(existente.get());
            compensacoes.salvar(operationId, pedido, hash, resultado);
            return resultado;
        }
        validarEvidencia(operationId, pedido);
        if (existente.isEmpty()) {
            var fechada = OperacaoConsumoEstoque.fechar(operationId, key);
            operacoes.salvar(fechada);
            var resultado = ResultadoConsumoEstoque.from(fechada);
            compensacoes.salvar(operationId, pedido, hash, resultado);
            return resultado;
        }
        var operacao = existente.get();
        if (operacao.getStatus() != StatusOperacaoConsumo.CONSUMED) {
            throw new IllegalStateException("Consumo ainda sem resultado definitivo.");
        }
        var originais = movimentos.buscarBaixas(operationId);
        if (originais.isEmpty()) { throw new IllegalStateException("Consumo sem movimentos originais."); }
        List<UUID> inversos = new ArrayList<>();
        // Same stable item order as consumption; even an inactive item can be returned after verification.
        for (var original : originais) {
            var item = itens.buscarPorIdParaAtualizacao(original.itemEstoqueId()).orElseThrow();
            int antes = item.getQuantidadeEstoque();
            item.adicionarEstoque(Math.subtractExact(original.quantidadeAntes(), original.quantidadeDepois()));
            itens.salvar(item);
            var inverso = new MovimentacaoEstoque(UUID.randomUUID(), item.getId(), TipoMovimentacao.ENTRADA,
                    OrigemMovimentacao.ORDEM_SERVICO, original.referenciaOrigemId(), antes, item.getQuantidadeEstoque(),
                    "Compensacao " + key, pedido.evidence().verifiedBy(), LocalDateTime.now(ZoneOffset.UTC));
            inversos.add(movimentos.salvarInverso(inverso, operationId, original.id(), key).id());
        }
        operacao.reverter(key);
        operacoes.salvar(operacao);
        var resultado = new ResultadoConsumoEstoque(operationId, StatusOperacaoConsumo.REVERSED, inversos, key);
        compensacoes.salvar(operationId, pedido, hash, resultado);
        return resultado;
    }

    private void validarEvidencia(UUID operationId, SolicitacaoCompensacao p) {
        if (!Boolean.TRUE.equals(p.unusedConfirmed())) {
            throw conflito("MATERIAL_USE_NOT_RESOLVED", operationId, p.compensationId());
        }
        var e = p.evidence();
        if (p.reason() == null || !Set.of("CANCELLED_BEFORE_USE", "VERIFIED_RETURN").contains(p.reason())
                || e == null || e.verifiedBy() == null || e.verifiedBy().equals(new UUID(0, 0))
                || e.verifiedAt() == null || e.verifiedAt().isAfter(Instant.now().plusSeconds(30))
                || e.reference() == null || e.reference().isBlank()) {
            throw conflito("EVIDENCE_REQUIRED", operationId, p.compensationId());
        }
    }

    private String hash(SolicitacaoCompensacao pedido) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(json.writeValueAsString(pedido).getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) { throw new IllegalStateException(ex); }
    }

    private ConflitoCompensacaoException conflito(String code, UUID operationId, UUID key) {
        return new ConflitoCompensacaoException(code, operationId, key);
    }
}
