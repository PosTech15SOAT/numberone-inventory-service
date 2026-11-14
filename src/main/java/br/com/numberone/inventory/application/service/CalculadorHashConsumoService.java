
package br.com.numberone.inventory.application.service;

import br.com.numberone.inventory.domain.model.SolicitacaoConsumoEstoque;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class CalculadorHashConsumoService {

    public String calcular(SolicitacaoConsumoEstoque solicitacao) {

        Objects.requireNonNull(
                solicitacao,
                "A solicitacao de consumo e obrigatoria."
        );

        String itensNormalizados = solicitacao.items()
                .stream()
                .sorted(Comparator.comparing(
                        item -> item.inventoryItemId().toString()
                ))
                .map(item ->
                        item.inventoryItemId() + ":" + item.quantity()
                )
                .collect(Collectors.joining("|"));

        String conteudo = String.join(
                "|",
                solicitacao.operationId().toString(),
                solicitacao.serviceOrderId().toString(),
                solicitacao.serviceOrderItemId().toString(),
                solicitacao.responsibleUserId().toString(),
                itensNormalizados
        );

        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(
                    conteudo.getBytes(StandardCharsets.UTF_8)
            );

            return HexFormat.of().formatHex(hash);

        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(
                    "Algoritmo SHA-256 indisponivel.",
                    e
            );
        }
    }
}
