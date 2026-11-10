
package br.com.numberone.inventory.adapter.in.web;

import br.com.numberone.inventory.application.service.ConsultarPrecosService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/internal/inventory")
public class ConsultarPrecosController {

    private final ConsultarPrecosService consultarPrecosService;

    public ConsultarPrecosController(
            ConsultarPrecosService consultarPrecosService) {
        this.consultarPrecosService = consultarPrecosService;
    }

    @PostMapping("/consultar-precos")
    public ResponseEntity<ConsultarPrecosResponse> consultarPrecos(
            @Valid @RequestBody ConsultarPrecosRequest request) {

        List<ConsultarPrecosResponse.ItemPreco> itens =
                consultarPrecosService
                        .consultar(request.inventoryItemIds())
                        .stream()
                        .map(preco -> new ConsultarPrecosResponse.ItemPreco(
                                preco.inventoryItemId(),
                                preco.salePrice().toPlainString(),
                                preco.currency()
                        ))
                        .toList();

        return ResponseEntity.ok(new ConsultarPrecosResponse(itens));
    }

    public record ConsultarPrecosRequest(
            @NotEmpty
            List<@NotNull UUID> inventoryItemIds
    ) {
    }

    public record ConsultarPrecosResponse(
            List<ItemPreco> items
    ) {
        public record ItemPreco(
                UUID inventoryItemId,
                String salePrice,
                String currency
        ) {
        }
    }
}
