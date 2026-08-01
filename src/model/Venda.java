package model;

import java.util.ArrayList;
import java.util.List;

public class Venda {
    private int id;
    private String dataVenda;
    private double total;
    private List<ItemVenda> itens = new ArrayList<>();

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getDataVenda() { return dataVenda; }
    public void setDataVenda(String dataVenda) { this.dataVenda = dataVenda; }
    public double getTotal() { return total; }
    public void setTotal(double total) { this.total = total; }
    public List<ItemVenda> getItens() { return itens; }

    public void adicionarItem(ItemVenda item) {
        itens.add(item);
        total += item.getQuantidade() * item.getPrecoUnitario();
    }
}
