package br.com.numberone.inventory.domain.model;

import java.time.Instant;
import java.util.UUID;

public record SolicitacaoCompensacao(UUID compensationId, String reason, Boolean unusedConfirmed, Evidence evidence) {
    public record Evidence(UUID verifiedBy, Instant verifiedAt, String reference) { }
}
