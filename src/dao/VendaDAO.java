package dao;

import db.Conexao;
import model.ItemVenda;
import model.Venda;
import java.sql.*;
import java.time.LocalDateTime;

public class VendaDAO {
    private final ProdutoDAO produtoDAO = new ProdutoDAO();

    /** Registra a venda, os itens e baixa o estoque em uma única transação. */
    public int registrar(Venda venda) throws SQLException {
        try (Connection con = Conexao.get()) {
            con.setAutoCommit(false);
            try {
                String sqlVenda = "INSERT INTO vendas (data_venda, total) VALUES (?,?)";
                int vendaId;
                try (PreparedStatement ps = con.prepareStatement(sqlVenda, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, LocalDateTime.now().toString());
                    ps.setDouble(2, venda.getTotal());
                    ps.executeUpdate();
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        rs.next();
                        vendaId = rs.getInt(1);
                    }
                }

                String sqlItem = "INSERT INTO itens_venda (venda_id, produto_id, quantidade, preco_unitario) VALUES (?,?,?,?)";
                try (PreparedStatement ps = con.prepareStatement(sqlItem)) {
                    for (ItemVenda item : venda.getItens()) {
                        ps.setInt(1, vendaId);
                        ps.setInt(2, item.getProdutoId());
                        ps.setInt(3, item.getQuantidade());
                        ps.setDouble(4, item.getPrecoUnitario());
                        ps.addBatch();
                        produtoDAO.baixarEstoque(item.getProdutoId(), item.getQuantidade(), con);
                    }
                    ps.executeBatch();
                }

                con.commit();
                return vendaId;
            } catch (SQLException e) {
                con.rollback();
                throw e;
            }
        }
    }

    public double totalVendidoHoje() throws SQLException {
        String sql = "SELECT COALESCE(SUM(total),0) FROM vendas WHERE data_venda LIKE ?";
        try (Connection con = Conexao.get(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, java.time.LocalDate.now() + "%");
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getDouble(1);
            }
        }
    }
}
