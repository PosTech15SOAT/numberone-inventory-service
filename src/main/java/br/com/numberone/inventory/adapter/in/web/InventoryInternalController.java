package br.com.numberone.inventory.adapter.in.web;

import br.com.numberone.inventory.application.service.CatalogoEstoqueService;
import br.com.numberone.inventory.application.service.ConsumirEstoqueService;
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
    private final CatalogoEstoqueService catalogo;
    private final ConsumirEstoqueService consumo;

    InventoryInternalController(CatalogoEstoqueService catalogo, ConsumirEstoqueService consumo) {
        this.catalogo = catalogo;
        this.consumo = consumo;
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
        log.info("CT-05 correlationId={} operationId={} result={}", correlation, operationId, resultado.status());
        return switch (resultado.status()) {
            case CONSUMED -> ResponseEntity.ok(OperationReply.from(resultado));
            case INSUFFICIENT_STOCK -> ResponseEntity.status(409).body(OperationReply.from(resultado));
            case PROCESSING -> ResponseEntity.status(503).body(new InventoryHttp.Problem(
                    "DEPENDENCY_UNAVAILABLE", "Operacao em processamento.", correlation, operationId));
        };
    }

    @GetMapping("/operations/{operationId}")
    ResponseEntity<OperationReply> consultar(@PathVariable String operationId,
                                             @RequestAttribute(InventoryHttp.REQUEST_CORRELATION) UUID correlation) {
        UUID id = InventoryHttp.uuid(operationId);
        var resultado = consumo.consultar(id);
        if (resultado.isEmpty()) {
            log.info("CT-08 correlationId={} operationId={} result=UNKNOWN", correlation, id);
            return ResponseEntity.status(404).body(new OperationReply(id, "UNKNOWN", null, null));
        }
        var resposta = OperationReply.from(resultado.get());
        log.info("CT-08 correlationId={} operationId={} result={}", correlation, id, resposta.result());
        if (resultado.get().status() == StatusOperacaoConsumo.PROCESSING) {
            return ResponseEntity.accepted().header("Retry-After", "1").body(resposta);
        }
        return ResponseEntity.ok(resposta);
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
    record OperationReply(UUID operationId, String result, String code, List<UUID> movements) {
        static OperationReply from(ResultadoConsumoEstoque resultado) {
            UUID id = resultado.operationId();
            return switch (resultado.status()) {
                case CONSUMED -> new OperationReply(id, "CONSUMED", null, resultado.movements());
                case INSUFFICIENT_STOCK -> new OperationReply(id, "REJECTED", "INSUFFICIENT_STOCK", List.of());
                case PROCESSING -> new OperationReply(id, "PENDING", null, null);
            };
        }
    }
}
