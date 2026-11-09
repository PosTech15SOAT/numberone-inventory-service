
package br.com.numberone.inventory.application.service;

import br.com.numberone.inventory.domain.model.OperacaoConsumoEstoque;
import br.com.numberone.inventory.domain.model.ResultadoConsumoEstoque;
import br.com.numberone.inventory.domain.model.SolicitacaoConsumoEstoque;
import br.com.numberone.inventory.domain.model.StatusOperacaoConsumo;
import br.com.numberone.inventory.domain.port.out.OperacaoConsumoRepositoryPort;

import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class ConsumirEstoqueService {

    private final OperacaoConsumoRepositoryPort operacaoRepository;
    private final CalculadorHashConsumoService calculadorHash;

    public ConsumirEstoqueService(
            OperacaoConsumoRepositoryPort operacaoRepository,
            CalculadorHashConsumoService calculadorHash) {

        this.operacaoRepository = operacaoRepository;
        this.calculadorHash = calculadorHash;
    }

    public ResultadoConsumoEstoque consultarOperacaoExistente(
            SolicitacaoConsumoEstoque solicitacao) {

        Objects.requireNonNull(
                solicitacao,
                "A solicitacao de consumo e obrigatoria."
        );

        String requestHash = calculadorHash.calcular(solicitacao);

        OperacaoConsumoEstoque operacao =
                operacaoRepository
                        .buscarPorOperationId(solicitacao.operationId())
                        .orElse(null);

        if (operacao == null) {
            return null;
        }

        if (!operacao.possuiMesmoRequestHash(requestHash)) {
            throw new ConflitoIdempotenciaException(
                    solicitacao.operationId()
            );
        }

        if (operacao.getStatus() == StatusOperacaoConsumo.PROCESSING) {
            throw new IllegalStateException(
                    "A operacao ainda esta em processamento."
            );
        }

        return ResultadoConsumoEstoque.from(operacao);
    }
}
