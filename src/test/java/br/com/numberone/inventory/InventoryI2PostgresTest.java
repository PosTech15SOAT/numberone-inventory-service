package br.com.numberone.inventory;

import br.com.numberone.inventory.application.service.*;
import br.com.numberone.inventory.domain.model.*;
import br.com.numberone.inventory.domain.port.out.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;

/** Requires a disposable, real PostgreSQL database. No mocked persistence or inventory participant. */
@EnabledIfEnvironmentVariable(named = "I2_POSTGRES_TEST", matches = "true")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "app.security.os-token=i2-os", "app.security.billing-token=i2-billing"})
class InventoryI2PostgresTest {
    @Autowired ConsumirEstoqueService consume;
    @Autowired CompensarEstoqueService reverse;
    @Autowired OperacaoConsumoRepositoryPort operations;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager manager;
    @Autowired ObjectMapper json;
    @Value("${local.server.port}") int port;
    private final HttpClient http = HttpClient.newHttpClient();
    private final UUID correlation = UUID.randomUUID();

    private UUID material(int balance) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO item_estoque(id,codigo,nome,tipo_item,unidade_medida,quantidade_estoque,custo_unitario,preco_venda)
                VALUES (?,?,'Peca I2','PECA','UNIDADE',?,10,20)
                """, id, "I2-" + id, balance);
        return id;
    }
    private SolicitacaoConsumoEstoque consumption(UUID material) {
        return new SolicitacaoConsumoEstoque(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                List.of(new SolicitacaoConsumoEstoque.ItemConsumo(material, 1)));
    }
    private SolicitacaoCompensacao reversal() {
        return new SolicitacaoCompensacao(UUID.randomUUID(), "VERIFIED_RETURN", true,
                new SolicitacaoCompensacao.Evidence(UUID.randomUUID(), Instant.now(), "Conferencia sintetica I2"));
    }
    private int balance(UUID material) {
        return jdbc.queryForObject("SELECT quantidade_estoque FROM item_estoque WHERE id=?", Integer.class, material);
    }
    private int count(String table, UUID id) {
        return jdbc.queryForObject("SELECT count(*) FROM " + table + " WHERE operation_id=?", Integer.class, id);
    }
    private JsonNode call(int status, String method, String path, Object body, UUID key) throws Exception {
        var req = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/internal/inventory/" + path))
                .timeout(Duration.ofSeconds(15)).header("Authorization", "Bearer i2-os")
                .header("X-Correlation-Id", correlation.toString()).header("Content-Type", "application/json");
        if (key != null) { req.header("Idempotency-Key", key.toString()); }
        var res = http.send(req.method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body))).build(), HttpResponse.BodyHandlers.ofString());
        assertThat(res.statusCode()).as(res.body()).isEqualTo(status);
        return json.readTree(res.body());
    }
    private JsonNode reverseHttp(int status, UUID operation, SolicitacaoCompensacao request) throws Exception {
        return call(status, "POST", "consumptions/" + operation + "/reversal", request, request.compensationId());
    }
    private JsonNode query(UUID operation) throws Exception { return call(200, "GET", "operations/" + operation, null, null); }

    @Test void closingWinsWhileNoOperationRowExists() throws Exception { race(true); }
    @Test void consumptionWinsAndReturnCommitsOnce() throws Exception { race(false); }

    private void race(boolean closeWins) throws Exception {
        UUID item = material(5);
        var consumption = consumption(item);
        UUID id = consumption.operationId();
        var compensation = reversal();
        var locked = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        try (var pool = Executors.newVirtualThreadPerTaskExecutor()) {
            var first = pool.submit(() -> new TransactionTemplate(manager).execute(tx -> {
                operations.bloquearOperacao(id);
                locked.countDown();
                try { assertThat(release.await(10, TimeUnit.SECONDS)).isTrue(); }
                catch (InterruptedException ex) { throw new RuntimeException(ex); }
                return closeWins ? reverse.compensar(id, compensation) : consume.consumir(consumption);
            }));
            assertThat(locked.await(10, TimeUnit.SECONDS)).isTrue();
            assertThat(count("operacao_consumo_estoque", id)).isZero();
            var second = pool.submit(() -> closeWins
                    ? call(409, "POST", "consumptions", consumption, id)
                    : reverseHttp(200, id, compensation));
            long until = System.nanoTime() + TimeUnit.SECONDS.toNanos(8);
            boolean waiting = false;
            while (System.nanoTime() < until) {
                waiting = jdbc.queryForObject("SELECT count(*)>0 FROM pg_locks WHERE locktype='advisory' AND NOT granted", Boolean.class);
                if (waiting) { break; }
                Thread.sleep(20);
            }
            assertThat(waiting).as("loser is blocked on the PostgreSQL operation lock before a row exists").isTrue();
            assertThat(second.isDone()).isFalse();
            release.countDown();
            var firstResult = first.get(10, TimeUnit.SECONDS);
            var secondResult = second.get(10, TimeUnit.SECONDS);
            assertThat(firstResult.status()).isEqualTo(closeWins ? StatusOperacaoConsumo.CLOSED : StatusOperacaoConsumo.CONSUMED);
            assertThat(secondResult.path(closeWins ? "code" : "result").asString()).isEqualTo(closeWins ? "ATTEMPT_CLOSED" : "REVERSED");
        } finally { release.countDown(); }
        var result = query(id);
        assertThat(result.path("result").asString()).isEqualTo(closeWins ? "CLOSED" : "REVERSED");
        assertThat(result.path("compensationId").asString()).isEqualTo(compensation.compensationId().toString());
        assertThat(reverseHttp(200, id, compensation)).isEqualTo(result);
        assertThat(call(409, "POST", "consumptions", consumption, id).path("code").asString())
                .isEqualTo(closeWins ? "ATTEMPT_CLOSED" : "ATTEMPT_REVERSED");
        assertThat(balance(item)).isEqualTo(5);
        assertThat(count("movimentacao_estoque", id)).isEqualTo(closeWins ? 0 : 2);
        assertThat(count("compensacao_estoque", id)).isEqualTo(1);
        System.out.printf("I2 RACE %s item=%s operation=%s compensation=%s balances=%s result=%s%n",
                closeWins ? "CLOSED_FIRST" : "CONSUMED_FIRST", item, id, compensation.compensationId(),
                closeWins ? "[5,5,5]" : "[5,4,5,5]", result);
    }

    @Test void batchAuditReplayAndConflictingKeys() throws Exception {
        UUID a = material(5), b = material(3);
        var base = consumption(a);
        var request = new SolicitacaoConsumoEstoque(base.operationId(), base.serviceOrderId(), base.serviceOrderItemId(),
                base.responsibleUserId(), List.of(new SolicitacaoConsumoEstoque.ItemConsumo(a, 2), new SolicitacaoConsumoEstoque.ItemConsumo(b, 1)));
        var original = consume.consumir(request);
        var rows = jdbc.queryForList("SELECT * FROM movimentacao_estoque WHERE operation_id=? ORDER BY id", request.operationId());
        var rev = reversal();
        var returned = reverseHttp(200, request.operationId(), rev);
        assertThat(returned.path("movements").size()).isEqualTo(2);
        assertThat(balance(a)).isEqualTo(5); assertThat(balance(b)).isEqualTo(3);
        for (var row : rows) {
            assertThat(jdbc.queryForMap("SELECT * FROM movimentacao_estoque WHERE id=?", row.get("id"))).isEqualTo(row);
        }
        assertThat(jdbc.queryForObject("SELECT count(*) FROM movimentacao_estoque WHERE operation_id=? AND original_movement_id IS NOT NULL",
                Integer.class, request.operationId())).isEqualTo(2);
        assertThat(reverseHttp(200, request.operationId(), rev)).isEqualTo(returned);
        var changed = new SolicitacaoCompensacao(rev.compensationId(), "CANCELLED_BEFORE_USE", true, rev.evidence());
        assertThat(reverseHttp(409, request.operationId(), changed).path("code").asString()).isEqualTo("IDEMPOTENCY_CONFLICT");
        assertThat(reverseHttp(409, request.operationId(), reversal()).path("code").asString()).isEqualTo("IDEMPOTENCY_CONFLICT");
        assertThat(reverseHttp(409, UUID.randomUUID(), rev).path("code").asString()).isEqualTo("IDEMPOTENCY_CONFLICT");
        assertThat(jdbc.queryForObject("SELECT request->'evidence'->>'reference' FROM compensacao_estoque WHERE compensation_id=?",
                String.class, rev.compensationId())).isEqualTo(rev.evidence().reference());
        System.out.printf("I2 BATCH operation=%s original=%s reversal=%s balances=[5/3,3/2,5/3]%n", request.operationId(), original.movements(), returned);
    }

    @Test void rejectedIsDurableAndResolvedWithoutCredit() throws Exception {
        UUID item = material(0); var req = consumption(item); consume.consumir(req);
        var rev = reversal();
        assertThat(reverseHttp(409, req.operationId(), rev).path("code").asString()).isEqualTo("OPERATION_ALREADY_REJECTED");
        assertThat(reverseHttp(409, req.operationId(), rev).path("code").asString()).isEqualTo("OPERATION_ALREADY_REJECTED");
        assertThat(query(req.operationId()).path("result").asString()).isEqualTo("REJECTED");
        assertThat(balance(item)).isZero(); assertThat(count("movimentacao_estoque", req.operationId())).isZero();
        assertThat(count("compensacao_estoque", req.operationId())).isEqualTo(1);
    }

    @Test void invalidEvidenceAndMismatchedHeaderNeverCreditOrClose() throws Exception {
        UUID item = material(5); var req = consumption(item); consume.consumir(req);
        for (UUID id : List.of(req.operationId(), UUID.randomUUID())) {
            var valid = reversal();
            for (var invalid : List.of(
                    new SolicitacaoCompensacao(valid.compensationId(), valid.reason(), true, null),
                    new SolicitacaoCompensacao(valid.compensationId(), valid.reason(), true,
                            new SolicitacaoCompensacao.Evidence(UUID.randomUUID(), Instant.now().plusSeconds(3600), "future")),
                    new SolicitacaoCompensacao(valid.compensationId(), valid.reason(), true,
                            new SolicitacaoCompensacao.Evidence(UUID.randomUUID(), Instant.now(), " ")))) {
                assertThat(reverseHttp(409, id, invalid).path("code").asString()).isEqualTo("EVIDENCE_REQUIRED");
            }
            assertThat(reverseHttp(409, id, new SolicitacaoCompensacao(valid.compensationId(), valid.reason(), false, valid.evidence()))
                    .path("code").asString()).isEqualTo("MATERIAL_USE_NOT_RESOLVED");
            call(400, "POST", "consumptions/" + id + "/reversal", valid, UUID.randomUUID());
            assertThat(count("compensacao_estoque", id)).isZero();
        }
        assertThat(balance(item)).isEqualTo(4);
        assertThat(query(req.operationId()).path("result").asString()).isEqualTo("CONSUMED");
    }

    @Test void concurrentReplayCreditsOnlyOnce() throws Exception {
        UUID item = material(5); var req = consumption(item); consume.consumir(req); var rev = reversal();
        try (var pool = Executors.newVirtualThreadPerTaskExecutor()) {
            var a = pool.submit(() -> reverseHttp(200, req.operationId(), rev));
            var b = pool.submit(() -> reverseHttp(200, req.operationId(), rev));
            assertThat(a.get(10, TimeUnit.SECONDS)).isEqualTo(b.get(10, TimeUnit.SECONDS));
        }
        assertThat(balance(item)).isEqualTo(5); assertThat(count("movimentacao_estoque", req.operationId())).isEqualTo(2);
    }

    @Test void commitFailureRollsBackCreditAuditAndState() throws Exception {
        UUID item = material(5); var req = consumption(item); consume.consumir(req); var rev = reversal();
        jdbc.execute("CREATE FUNCTION i2_fail_compensation() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN RAISE EXCEPTION 'I2_COMMIT_FAILURE'; END $$");
        jdbc.execute("CREATE CONSTRAINT TRIGGER i2_fail_compensation AFTER INSERT ON compensacao_estoque DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION i2_fail_compensation()");
        try { reverseHttp(503, req.operationId(), rev); }
        finally {
            jdbc.execute("DROP TRIGGER i2_fail_compensation ON compensacao_estoque");
            jdbc.execute("DROP FUNCTION i2_fail_compensation()");
        }
        assertThat(balance(item)).isEqualTo(4);
        assertThat(count("compensacao_estoque", req.operationId())).isZero();
        assertThat(count("movimentacao_estoque", req.operationId())).isEqualTo(1);
        assertThat(query(req.operationId()).path("result").asString()).isEqualTo("CONSUMED");
        reverseHttp(200, req.operationId(), rev);
        assertThat(balance(item)).isEqualTo(5);
    }

    @Test void oneCompensationIdentityCannotCloseTwoOperationsConcurrently() throws Exception {
        var a = UUID.randomUUID(); var b = UUID.randomUUID(); var rev = reversal();
        try (var pool = Executors.newVirtualThreadPerTaskExecutor()) {
            var results = pool.invokeAll(List.of(
                    () -> compensateOrConflict(a, rev), () -> compensateOrConflict(b, rev)));
            assertThat(results.stream().map(r -> {
                try { return r.get(); } catch (Exception ex) { throw new RuntimeException(ex); }
            }).toList()).containsExactlyInAnyOrder("CLOSED", "IDEMPOTENCY_CONFLICT");
        }
        assertThat(count("operacao_consumo_estoque", a) + count("operacao_consumo_estoque", b)).isEqualTo(1);
    }

    private String compensateOrConflict(UUID id, SolicitacaoCompensacao req) {
        try { return reverse.compensar(id, req).status().name(); }
        catch (ConflitoCompensacaoException ex) { return ex.getMessage(); }
    }

    @Test void reversalRequiresOsIdentityAndUuidCorrelation() throws Exception {
        UUID id = UUID.randomUUID(); var req = reversal();
        for (String token : List.of("i2-billing", "wrong", "i2-os")) {
            var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port
                    + "/internal/inventory/consumptions/" + id + "/reversal"))
                    .header("Authorization", "Bearer " + token).header("Content-Type", "application/json")
                    .header("Idempotency-Key", req.compensationId().toString());
            if (!token.equals("i2-os")) { request.header("X-Correlation-Id", correlation.toString()); }
            var reply = http.send(request.POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(req))).build(),
                    HttpResponse.BodyHandlers.ofString());
            assertThat(reply.statusCode()).isEqualTo(token.equals("i2-billing") ? 403 : token.equals("wrong") ? 401 : 400);
        }
        assertThat(count("operacao_consumo_estoque", id)).isZero();
    }
}
