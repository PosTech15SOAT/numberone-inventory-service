package br.com.numberone.inventory.adapter.out.persistence;

import br.com.numberone.inventory.domain.model.*;
import br.com.numberone.inventory.domain.port.out.CompensacaoRepositoryPort;
import jakarta.persistence.EntityManager;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.ObjectMapper;
import java.util.Optional;
import java.util.UUID;

@Repository
class CompensacaoPersistenceAdapter implements CompensacaoRepositoryPort {
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;
    private final EntityManager entities;

    CompensacaoPersistenceAdapter(JdbcTemplate jdbc, ObjectMapper json, EntityManager entities) {
        this.jdbc = jdbc; this.json = json; this.entities = entities;
    }

    public void bloquearCompensacao(UUID id) {
        jdbc.queryForList("SELECT pg_advisory_xact_lock(hashtextextended(?, 46842))", id.toString());
    }

    public Optional<Registro> buscar(UUID id) {
        return jdbc.query("SELECT operation_id, request_hash, result::text FROM compensacao_estoque WHERE compensation_id=?",
                (r, n) -> new Registro(r.getObject(1, UUID.class), r.getString(2),
                        json.readValue(r.getString(3), ResultadoConsumoEstoque.class)), id).stream().findFirst();
    }

    public void salvar(UUID operationId, SolicitacaoCompensacao pedido, String hash, ResultadoConsumoEstoque resultado) {
        // Flush the operation and inverse movements on this same transaction/connection before the audit insert.
        entities.flush();
        jdbc.update("""
                INSERT INTO compensacao_estoque(compensation_id, operation_id, request_hash, request, result)
                VALUES (?, ?, ?, CAST(? AS jsonb), CAST(? AS jsonb))
                """, pedido.compensationId(), operationId, hash, json.writeValueAsString(pedido), json.writeValueAsString(resultado));
    }
}
