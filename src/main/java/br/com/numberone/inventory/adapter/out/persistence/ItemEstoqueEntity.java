
package br.com.numberone.inventory.adapter.out.persistence;

import br.com.numberone.inventory.domain.model.ItemEstoque;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "item_estoque")
public class ItemEstoqueEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 100)
    private String codigo;

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(length = 255)
    private String descricao;

    @Column(name = "tipo_item", nullable = false, length = 50)
    private String tipoItem;

    @Column(name = "unidade_medida", nullable = false, length = 50)
    private String unidadeMedida;

    @Column(name = "quantidade_estoque", nullable = false)
    private int quantidadeEstoque;

    @Column(name = "custo_unitario", nullable = false,
            precision = 10, scale = 2)
    private BigDecimal custoUnitario;

    @Column(name = "preco_venda", nullable = false,
            precision = 10, scale = 2)
    private BigDecimal precoVenda;

    @Column(name = "estoque_minimo", nullable = false)
    private int estoqueMinimo;

    @Column(length = 100)
    private String marca;

    @Column(name = "veiculo_aplicavel", length = 255)
    private String veiculoAplicavel;

    @Column(nullable = false)
    private boolean ativo;

    @Column(name = "created_at", nullable = false,
            insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false,
            insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    protected ItemEstoqueEntity() {
    }

    public static ItemEstoqueEntity fromDomain(ItemEstoque item) {

        ItemEstoqueEntity entity = new ItemEstoqueEntity();

        entity.id = item.getId();
        entity.codigo = item.getCodigo();
        entity.nome = item.getNome();
        entity.descricao = item.getDescricao();
        entity.tipoItem = item.getTipoItem();
        entity.unidadeMedida = item.getUnidadeMedida();
        entity.quantidadeEstoque = item.getQuantidadeEstoque();
        entity.custoUnitario = item.getCustoUnitario();
        entity.precoVenda = item.getPrecoVenda();
        entity.estoqueMinimo = item.getEstoqueMinimo();
        entity.marca = item.getMarca();
        entity.veiculoAplicavel = item.getVeiculoAplicavel();
        entity.ativo = item.isAtivo();

        return entity;
    }

    public ItemEstoque toDomain() {
        return new ItemEstoque(
                id,
                codigo,
                nome,
                descricao,
                tipoItem,
                unidadeMedida,
                quantidadeEstoque,
                custoUnitario,
                precoVenda,
                estoqueMinimo,
                marca,
                veiculoAplicavel,
                ativo
        );
    }
}
