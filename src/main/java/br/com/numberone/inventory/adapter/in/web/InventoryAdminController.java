package br.com.numberone.inventory.adapter.in.web;

import br.com.numberone.inventory.application.port.in.GerenciarCatalogoUseCase;
import br.com.numberone.inventory.application.service.ItemIndisponivelException;
import br.com.numberone.inventory.domain.model.ItemEstoque;
import br.com.numberone.inventory.domain.model.MovimentacaoEstoque;
import br.com.numberone.inventory.domain.model.OrigemMovimentacao;
import br.com.numberone.inventory.domain.model.TipoMovimentacao;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
class InventoryAdminController {
    private final GerenciarCatalogoUseCase catalogo;

    InventoryAdminController(GerenciarCatalogoUseCase catalogo) {
        this.catalogo = catalogo;
    }

    @PostMapping("/itens")
    ResponseEntity<ItemEstoque> cadastrar(@Valid @RequestBody Cadastro body,
                                          @RequestHeader(InventoryHttp.AUTHOR) String autor) {
        var item = catalogo.cadastrar(body.toDomain(), InventoryHttp.quantidade(body.quantidadeEstoque()), InventoryHttp.uuid(autor));
        var location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(item.getId()).toUri();
        return ResponseEntity.created(location).body(item);
    }

    @GetMapping("/itens")
    List<ItemEstoque> listar() {
        return catalogo.listar();
    }

    @GetMapping("/itens/{itemId}")
    ItemEstoque consultar(@PathVariable UUID itemId) {
        return catalogo.consultar(itemId).orElseThrow(() -> new ItemIndisponivelException(itemId));
    }

    @PostMapping("/estoque/entrada")
    ResponseEntity<Movimento> entrada(@Valid @RequestBody Entrada body,
                                      @RequestHeader(InventoryHttp.AUTHOR) String autor) {
        var movimento = catalogo.entrada(body.idItemEstoque(), InventoryHttp.quantidade(body.quantidade()),
                body.origemMovimentacao(), body.referenciaOrigemId(), body.observacao(), InventoryHttp.uuid(autor));
        return ResponseEntity.status(201).body(Movimento.from(movimento));
    }

    @GetMapping("/estoque/itens/{itemId}/movimentacoes")
    List<Movimento> movimentos(@PathVariable UUID itemId) {
        return catalogo.consultarMovimentos(itemId).stream().map(Movimento::from).toList();
    }

    record Cadastro(@NotBlank @Size(max = 100) String codigo, @NotBlank @Size(max = 150) String nome,
                    @Size(max = 255) String descricao, @NotBlank @Size(max = 50) String tipoItem,
                    @NotBlank @Size(max = 50) String unidadeMedida,
                    @NotNull @Positive @Digits(integer = 8, fraction = 2) BigDecimal custoUnitario,
                    @NotNull @Positive @Digits(integer = 8, fraction = 2) BigDecimal precoVenda,
                    @NotNull JsonNode quantidadeEstoque, @NotNull JsonNode estoqueMinimo,
                    @Size(max = 100) String marca, @Size(max = 255) String veiculoAplicavel, Boolean ativo) {
        ItemEstoque toDomain() {
            return new ItemEstoque(null, codigo, nome, descricao, tipoItem, unidadeMedida, 0,
                    custoUnitario, precoVenda, InventoryHttp.quantidade(estoqueMinimo), marca,
                    veiculoAplicavel, !Boolean.FALSE.equals(ativo));
        }
    }

    record Entrada(@NotNull UUID idItemEstoque, @NotNull JsonNode quantidade,
                   @NotNull OrigemMovimentacao origemMovimentacao, UUID referenciaOrigemId,
                   @Size(max = 500) String observacao) { }

    record Movimento(UUID id, UUID inventoryItemId, TipoMovimentacao tipoMovimentacao,
                     OrigemMovimentacao origemMovimentacao, UUID referenciaOrigemId, int quantidadeAntes,
                     int quantidadeDepois, String observacao, UUID usuarioResponsavelId, LocalDateTime createdAt) {
        static Movimento from(MovimentacaoEstoque m) {
            return new Movimento(m.id(), m.itemEstoqueId(), m.tipoMovimentacao(), m.origemMovimentacao(),
                    m.referenciaOrigemId(), m.quantidadeAntes(), m.quantidadeDepois(), m.observacao(),
                    m.usuarioResponsavelId(), m.createdAt());
        }
    }
}
