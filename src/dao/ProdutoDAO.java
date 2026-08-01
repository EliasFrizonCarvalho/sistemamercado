package dao;

import db.Conexao;
import model.Produto;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProdutoDAO {

    public void inserir(Produto p) throws SQLException {
        String sql = "INSERT INTO produtos (nome, categoria_id, preco, estoque, estoque_minimo) VALUES (?,?,?,?,?)";
        try (Connection con = Conexao.get(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, p.getNome());
            ps.setInt(2, p.getCategoriaId());
            ps.setDouble(3, p.getPreco());
            ps.setInt(4, p.getEstoque());
            ps.setInt(5, p.getEstoqueMinimo());
            ps.executeUpdate();
        }
    }

    public List<Produto> listarTodos() throws SQLException {
        List<Produto> lista = new ArrayList<>();
        String sql = "SELECT * FROM produtos ORDER BY nome";
        try (Connection con = Conexao.get(); Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) lista.add(mapear(rs));
        }
        return lista;
    }

    public Produto buscarPorId(int id) throws SQLException {
        String sql = "SELECT * FROM produtos WHERE id = ?";
        try (Connection con = Conexao.get(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        }
    }

    public void atualizar(Produto p) throws SQLException {
        String sql = "UPDATE produtos SET nome=?, categoria_id=?, preco=?, estoque=?, estoque_minimo=? WHERE id=?";
        try (Connection con = Conexao.get(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, p.getNome());
            ps.setInt(2, p.getCategoriaId());
            ps.setDouble(3, p.getPreco());
            ps.setInt(4, p.getEstoque());
            ps.setInt(5, p.getEstoqueMinimo());
            ps.setInt(6, p.getId());
            ps.executeUpdate();
        }
    }

    public void remover(int id) throws SQLException {
        String sql = "DELETE FROM produtos WHERE id = ?";
        try (Connection con = Conexao.get(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public void baixarEstoque(int produtoId, int quantidade, Connection con) throws SQLException {
        String sql = "UPDATE produtos SET estoque = estoque - ? WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, quantidade);
            ps.setInt(2, produtoId);
            ps.executeUpdate();
        }
    }

    public List<Produto> estoqueBaixo() throws SQLException {
        List<Produto> lista = new ArrayList<>();
        String sql = "SELECT * FROM produtos WHERE estoque <= estoque_minimo ORDER BY estoque";
        try (Connection con = Conexao.get(); Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) lista.add(mapear(rs));
        }
        return lista;
    }

    private Produto mapear(ResultSet rs) throws SQLException {
        Produto p = new Produto();
        p.setId(rs.getInt("id"));
        p.setNome(rs.getString("nome"));
        p.setCategoriaId(rs.getInt("categoria_id"));
        p.setPreco(rs.getDouble("preco"));
        p.setEstoque(rs.getInt("estoque"));
        p.setEstoqueMinimo(rs.getInt("estoque_minimo"));
        return p;
    }
}
