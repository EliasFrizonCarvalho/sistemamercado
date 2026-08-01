package model;

public class Produto {
    private int id;
    private String nome;
    private int categoriaId;
    private double preco;
    private int estoque;
    private int estoqueMinimo;

    public Produto() {}

    public Produto(String nome, int categoriaId, double preco, int estoque, int estoqueMinimo) {
        this.nome = nome;
        this.categoriaId = categoriaId;
        this.preco = preco;
        this.estoque = estoque;
        this.estoqueMinimo = estoqueMinimo;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public int getCategoriaId() { return categoriaId; }
    public void setCategoriaId(int categoriaId) { this.categoriaId = categoriaId; }
    public double getPreco() { return preco; }
    public void setPreco(double preco) { this.preco = preco; }
    public int getEstoque() { return estoque; }
    public void setEstoque(int estoque) { this.estoque = estoque; }
    public int getEstoqueMinimo() { return estoqueMinimo; }
    public void setEstoqueMinimo(int estoqueMinimo) { this.estoqueMinimo = estoqueMinimo; }

    @Override
    public String toString() {
        return String.format("#%-4d %-25s R$ %-8.2f estoque: %-4d (min: %d)",
                id, nome, preco, estoque, estoqueMinimo);
    }
}
