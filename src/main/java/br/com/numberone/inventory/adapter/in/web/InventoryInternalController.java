package br.com.numberone.inventory.adapter.in.web;

import br.com.numberone.inventory.application.port.in.GerenciarCatalogoUseCase;
import br.com.numberone.inventory.application.port.in.ConsumirEstoqueUseCase;
import br.com.numberone.inventory.application.port.in.CompensarEstoqueUseCase;
import br.com.numberone.inventory.domain.model.SolicitacaoCompensacao;
import br.com.numberone.inventory.application.service.ItemIndisponivelException;
import br.com.numberone.inventory.domain.model.ResultadoConsumoEstoque;
import br.com.numberone.inventory.domain.model.SolicitacaoConsumoEstoque;
import br.com.numberone.inventory.domain.model.StatusOperacaoConsumo;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.JsonNode;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/internal/inventory")
class InventoryInternalController {
    private static final Logger log = LoggerFactory.getLogger(InventoryInternalController.class);
    private final GerenciarCatalogoUseCase catalogo;
    private final ConsumirEstoqueUseCase consumo;
    private final CompensarEstoqueUseCase compensacao;

    InventoryInternalController(GerenciarCatalogoUseCase catalogo, ConsumirEstoqueUseCase consumo, CompensarEstoqueUseCase compensacao) {
        this.catalogo = catalogo;
        this.consumo = consumo;
        this.compensacao = compensacao;
    }

    @GetMapping("/items/{inventoryItemId}")
    MaterialReference referencia(@PathVariable String inventoryItemId) {
        UUID id = InventoryHttp.uuid(inventoryItemId);
        var item = catalogo.consultar(id).orElseThrow(() -> new ItemIndisponivelException(id));
        String descricao = item.getDescricao();
        if (descricao == null || descricao.isBlank()) {
            descricao = item.getNome();
        }
        return new MaterialReference(id, descricao, item.getPrecoVenda().toPlainString(), item.isAtivo());
    }

    @PostMapping("/consumptions")
    ResponseEntity<?> consumir(@RequestHeader("Idempotency-Key") String key,
                               @RequestAttribute(InventoryHttp.REQUEST_CORRELATION) UUID correlation,
                               @RequestBody ConsumptionRequest body) {
        UUID operationId = InventoryHttp.uuid(key);
        if (!operationId.equals(InventoryHttp.uuid(body.operationId())) || body.items() == null) {
            throw new IllegalArgumentException("Identidade ou lote de consumo invalido.");
        }
        var pedido = new SolicitacaoConsumoEstoque(operationId, InventoryHttp.uuid(body.serviceOrderId()),
                InventoryHttp.uuid(body.serviceOrderItemId()), InventoryHttp.uuid(body.responsibleUserId()),
                body.items().stream().map(Supply::toDomain).toList());
        var resultado = consumo.consumir(pedido);
        safely(() -> log.atInfo().addKeyValue("event.code", "INVENTORY_CONSUMPTION_PROCESSED")
                .addKeyValue("service_order.id", pedido.serviceOrderId())
                .addKeyValue("operation.id", operationId).addKeyValue("event.outcome", resultado.status())
                .log("CT-05: consumo de estoque processado"));
        return switch (resultado.status()) {
            case CONSUMED -> ResponseEntity.ok(OperationReply.from(resultado));
            case INSUFFICIENT_STOCK -> ResponseEntity.status(409).body(OperationReply.from(resultado));
            case PROCESSING -> ResponseEntity.status(503).body(new InventoryHttp.Problem(
                    "DEPENDENCY_UNAVAILABLE", "Operacao em processamento.", correlation, operationId));
            case CLOSED, REVERSED -> throw new IllegalStateException("Consumo de tentativa encerrada.");
        };
    }

    @PostMapping("/consumptions/{operationId}/reversal")
    ResponseEntity<?> compensar(@PathVariable String operationId, @RequestHeader("Idempotency-Key") String key,
            @RequestAttribute(InventoryHttp.REQUEST_CORRELATION) UUID correlation,
            @RequestBody SolicitacaoCompensacao body) {
        UUID id = InventoryHttp.uuid(operationId);
        UUID compensationId = InventoryHttp.uuid(key);
        if (!compensationId.equals(body.compensationId())) {
            throw new IllegalArgumentException("Idempotency-Key deve corresponder ao compensationId.");
        }
        var resultado = compensacao.compensar(id, body);
        safely(() -> log.atInfo().addKeyValue("event.code", "INVENTORY_COMPENSATION_PROCESSED")
                .addKeyValue("operation.id", id).addKeyValue("compensation.id", compensationId)
                .addKeyValue("event.outcome", resultado.status()).log("CT-08B: compensação de estoque processada"));
        // Translate after commit so the confirmed refusal and its compensation audit remain durable.
        if (resultado.status() == StatusOperacaoConsumo.INSUFFICIENT_STOCK) {
            return ResponseEntity.status(409).body(new InventoryHttp.Problem("OPERATION_ALREADY_REJECTED",
                    "Tentativa recusada; confirmar pelo CT-08.", correlation, id, compensationId));
        }
        return ResponseEntity.ok(OperationReply.from(resultado));
    }

    @GetMapping("/operations/{operationId}")
    ResponseEntity<OperationReply> consultar(@PathVariable String operationId,
                                             @RequestAttribute(InventoryHttp.REQUEST_CORRELATION) UUID correlation) {
        UUID id = InventoryHttp.uuid(operationId);
        var resultado = consumo.consultar(id);
        if (resultado.isEmpty()) {
            safely(() -> log.atInfo().addKeyValue("event.code", "INVENTORY_OPERATION_QUERIED")
                    .addKeyValue("operation.id", id).addKeyValue("event.outcome", "UNKNOWN")
                    .log("CT-08: operação de estoque consultada"));
            return ResponseEntity.status(404).body(new OperationReply(id, "UNKNOWN", null, null, null));
        }
        var resposta = OperationReply.from(resultado.get());
        safely(() -> log.atInfo().addKeyValue("event.code", "INVENTORY_OPERATION_QUERIED")
                .addKeyValue("operation.id", id).addKeyValue("event.outcome", resposta.result())
                .log("CT-08: operação de estoque consultada"));
        if (resultado.get().status() == StatusOperacaoConsumo.PROCESSING) {
            return ResponseEntity.accepted().header("Retry-After", "1").body(resposta);
        }
        return ResponseEntity.ok(resposta);
    }

    private static void safely(Runnable logging) {
        try { logging.run(); } catch (RuntimeException ignored) { /* Não altera a operação. */ }
    }

    record MaterialReference(UUID id, String description, String referenceUnitPrice, boolean active) { }

    record ConsumptionRequest(String operationId, String serviceOrderId, String serviceOrderItemId,
                              String responsibleUserId, List<Supply> items) {
        ConsumptionRequest {
            if (items != null && items.stream().anyMatch(java.util.Objects::isNull)) {
                throw new IllegalArgumentException("Material obrigatorio.");
            }
        }
    }

    record Supply(String inventoryItemId, JsonNode quantity) {
        SolicitacaoConsumoEstoque.ItemConsumo toDomain() {
            return new SolicitacaoConsumoEstoque.ItemConsumo(InventoryHttp.uuid(inventoryItemId), InventoryHttp.quantidade(quantity));
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record OperationReply(UUID operationId, String result, String code, List<UUID> movements, UUID compensationId) {
        static OperationReply from(ResultadoConsumoEstoque resultado) {
            UUID id = resultado.operationId();
            return switch (resultado.status()) {
                case CONSUMED -> new OperationReply(id, "CONSUMED", null, resultado.movements(), null);
                case INSUFFICIENT_STOCK -> new OperationReply(id, "REJECTED", "INSUFFICIENT_STOCK", List.of(), null);
                case PROCESSING -> new OperationReply(id, "PENDING", null, null, null);
                case CLOSED, REVERSED -> new OperationReply(id, resultado.status().name(), null,
                        resultado.movements(), resultado.compensationId());
            };
        }
    }
}
