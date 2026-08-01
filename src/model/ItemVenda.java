package model;

public class ItemVenda {
    private int produtoId;
    private String nomeProduto;
    private int quantidade;
    private double precoUnitario;

    public ItemVenda(int produtoId, String nomeProduto, int quantidade, double precoUnitario) {
        this.produtoId = produtoId;
        this.nomeProduto = nomeProduto;
        this.quantidade = quantidade;
        this.precoUnitario = precoUnitario;
    }

    public int getProdutoId() { return produtoId; }
    public String getNomeProduto() { return nomeProduto; }
    public int getQuantidade() { return quantidade; }
    public double getPrecoUnitario() { return precoUnitario; }
}
