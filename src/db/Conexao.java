package db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Conexao {
    private static final String URL = "jdbc:sqlite:mercado.db";

    static {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Driver SQLite não encontrado no classpath.", e);
        }
    }

    public static Connection get() throws SQLException {
        return DriverManager.getConnection(URL);
    }

    /** Cria as tabelas a partir do schema.sql na primeira execução. */
    public static void inicializar() {
        try (Connection con = get(); Statement st = con.createStatement()) {
            String sql = new String(Files.readAllBytes(Paths.get("schema.sql")));
            for (String comando : sql.split(";")) {
                if (!comando.isBlank()) st.execute(comando);
            }
        } catch (Exception e) {
            System.err.println("Erro ao inicializar banco: " + e.getMessage());
        }
    }
}
