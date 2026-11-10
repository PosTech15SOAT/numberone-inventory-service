
package br.com.numberone.inventory.domain.model;

import java.math.BigDecimal;
import java.util.UUID;

public class ItemEstoque {

    private UUID id;
    private String codigo;
    private String nome;
    private String descricao;
    private String tipoItem;
    private String unidadeMedida;
    private int quantidadeEstoque;
    private BigDecimal custoUnitario;
    private BigDecimal precoVenda;
    private int estoqueMinimo;
    private String marca;
    private String veiculoAplicavel;
    private boolean ativo;

    public ItemEstoque(
            UUID id,
            String codigo,
            String nome,
            String descricao,
            String tipoItem,
            String unidadeMedida,
            int quantidadeEstoque,
            BigDecimal custoUnitario,
            BigDecimal precoVenda,
            int estoqueMinimo,
            String marca,
            String veiculoAplicavel,
            boolean ativo) {

        if (codigo == null || codigo.isBlank()) {
            throw new IllegalArgumentException("Codigo obrigatorio");
        }

        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("Nome obrigatorio");
        }

        if (tipoItem == null || tipoItem.isBlank()) {
            throw new IllegalArgumentException("Tipo do item obrigatorio");
        }

        if (unidadeMedida == null || unidadeMedida.isBlank()) {
            throw new IllegalArgumentException("Unidade de medida obrigatoria");
        }

        if (quantidadeEstoque < 0 || estoqueMinimo < 0) {
            throw new IllegalArgumentException("Estoque nao pode ser negativo");
        }

        if (custoUnitario == null || custoUnitario.signum() <= 0
                || precoVenda == null || precoVenda.signum() <= 0) {
            throw new IllegalArgumentException("Valores devem ser positivos");
        }

        this.id = id;
        this.codigo = codigo;
        this.nome = nome;
        this.descricao = descricao;
        this.tipoItem = tipoItem;
        this.unidadeMedida = unidadeMedida;
        this.quantidadeEstoque = quantidadeEstoque;
        this.custoUnitario = custoUnitario;
        this.precoVenda = precoVenda;
        this.estoqueMinimo = estoqueMinimo;
        this.marca = marca;
        this.veiculoAplicavel = veiculoAplicavel;
        this.ativo = ativo;
    }

    public int getQuantidadeEstoque() {
        return quantidadeEstoque;
    }

    public boolean abaixoDoEstoqueMinimo() {
        return quantidadeEstoque < estoqueMinimo;
    }

    public void adicionarEstoque(int quantidade) {
        if (quantidade <= 0) {
            throw new IllegalArgumentException("Quantidade deve ser positiva");
        }

        this.quantidadeEstoque =
                Math.addExact(this.quantidadeEstoque, quantidade);
    }

    public void baixarEstoque(int quantidade) {
        if (quantidade <= 0 || quantidade > quantidadeEstoque) {
            throw new IllegalArgumentException("Quantidade invalida para baixa");
        }

        this.quantidadeEstoque -= quantidade;
    }


   public UUID getId() {
       return id;
   }

   public String getCodigo() {
       return codigo;
   }

   public String getNome() {
       return nome;
   }

   public String getDescricao() {
       return descricao;
   }

   public String getTipoItem() {
       return tipoItem;
   }

   public String getUnidadeMedida() {
       return unidadeMedida;
   }

   public BigDecimal getCustoUnitario() {
       return custoUnitario;
   }

   public BigDecimal getPrecoVenda() {
       return precoVenda;
   }

   public int getEstoqueMinimo() {
       return estoqueMinimo;
   }

   public String getMarca() {
       return marca;
   }

   public String getVeiculoAplicavel() {
       return veiculoAplicavel;
   }
 
   public boolean isAtivo() {
       return ativo;
   }

}
