import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import dao.ProdutoDAO;
import dao.VendaDAO;
import db.Conexao;
import model.ItemVenda;
import model.Produto;
import model.Venda;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WebServer {
    private static final ProdutoDAO produtoDAO = new ProdutoDAO();
    private static final VendaDAO vendaDAO = new VendaDAO();
    private static final Path WEB_ROOT = Path.of("web");

    public static void main(String[] args) throws Exception {
        Conexao.inicializar();
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/", new StaticHandler());
        server.createContext("/api/", new ApiHandler());
        server.setExecutor(null);
        server.start();
        System.out.println("Servidor web iniciado em http://localhost:8080");
    }

    private static class StaticHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String requested = exchange.getRequestURI().getPath();
            if (requested.equals("/")) {
                requested = "/index.html";
            }
            Path file = WEB_ROOT.resolve(requested.substring(1)).normalize();
            if (!file.startsWith(WEB_ROOT) || !Files.exists(file) || Files.isDirectory(file)) {
                sendText(exchange, 404, "Arquivo não encontrado");
                return;
            }
            String contentType = switch (getExtension(file.getFileName().toString())) {
                case "css" -> "text/css; charset=UTF-8";
                case "js" -> "application/javascript; charset=UTF-8";
                case "html" -> "text/html; charset=UTF-8";
                default -> "application/octet-stream";
            };
            exchange.getResponseHeaders().set("Content-Type", contentType);
            byte[] bytes = Files.readAllBytes(file);
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }

    private static class ApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                String path = exchange.getRequestURI().getPath();
                String method = exchange.getRequestMethod();
                if (path.equals("/api/produtos") && method.equals("GET")) {
                    handleListProdutos(exchange);
                } else if (path.equals("/api/estoque-baixo") && method.equals("GET")) {
                    handleEstoqueBaixo(exchange);
                } else if (path.equals("/api/total-hoje") && method.equals("GET")) {
                    handleTotalHoje(exchange);
                } else if (path.equals("/api/produtos") && method.equals("POST")) {
                    handleCreateProduto(exchange);
                } else if (path.startsWith("/api/produtos/") && method.equals("PUT")) {
                    handleUpdateProduto(exchange);
                } else if (path.startsWith("/api/produtos/") && method.equals("DELETE")) {
                    handleRemoveProduto(exchange);
                } else if (path.equals("/api/vendas") && method.equals("POST")) {
                    handleCreateVenda(exchange);
                } else {
                    sendJson(exchange, 404, "{\"error\":\"Rota não encontrada\"}");
                }
            } catch (Exception e) {
                sendJson(exchange, 500, "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
            }
        }
    }

    private static void handleListProdutos(HttpExchange exchange) throws IOException, SQLException {
        List<Produto> produtos = produtoDAO.listarTodos();
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < produtos.size(); i++) {
            Produto produto = produtos.get(i);
            if (i > 0) json.append(",");
            json.append(produtoToJson(produto));
        }
        json.append("]");
        sendJson(exchange, 200, json.toString());
    }

    private static void handleEstoqueBaixo(HttpExchange exchange) throws IOException, SQLException {
        List<Produto> produtos = produtoDAO.estoqueBaixo();
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < produtos.size(); i++) {
            if (i > 0) json.append(",");
            json.append(produtoToJson(produtos.get(i)));
        }
        json.append("]");
        sendJson(exchange, 200, json.toString());
    }

    private static void handleTotalHoje(HttpExchange exchange) throws IOException, SQLException {
        double total = vendaDAO.totalVendidoHoje();
        sendJson(exchange, 200, "{\"totalHoje\":" + total + "}");
    }

    private static void handleCreateProduto(HttpExchange exchange) throws IOException, SQLException {
        String body = readRequestBody(exchange);
        Map<String, Object> data = parseJson(body);
        Produto produto = new Produto(
            asString(data.get("nome")),
            asInt(data.get("categoriaId"), 1),
            asDouble(data.get("preco"), 0),
            asInt(data.get("estoque"), 0),
            asInt(data.get("estoqueMinimo"), 0)
        );
        produtoDAO.inserir(produto);
        sendJson(exchange, 201, "{\"success\":true}");
    }

    private static void handleUpdateProduto(HttpExchange exchange) throws IOException, SQLException {
        String id = exchange.getRequestURI().getPath().replaceFirst("/api/produtos/", "");
        String body = readRequestBody(exchange);
        Map<String, Object> data = parseJson(body);
        Produto produto = produtoDAO.buscarPorId(Integer.parseInt(id));
        if (produto == null) {
            sendJson(exchange, 404, "{\"error\":\"Produto não encontrado\"}");
            return;
        }
        produto.setNome(asString(data.getOrDefault("nome", produto.getNome())));
        produto.setCategoriaId(asInt(data.getOrDefault("categoriaId", produto.getCategoriaId()), produto.getCategoriaId()));
        produto.setPreco(asDouble(data.getOrDefault("preco", produto.getPreco()), produto.getPreco()));
        produto.setEstoque(asInt(data.getOrDefault("estoque", produto.getEstoque()), produto.getEstoque()));
        produto.setEstoqueMinimo(asInt(data.getOrDefault("estoqueMinimo", produto.getEstoqueMinimo()), produto.getEstoqueMinimo()));
        produtoDAO.atualizar(produto);
        sendJson(exchange, 200, "{\"success\":true}");
    }

    private static void handleRemoveProduto(HttpExchange exchange) throws IOException, SQLException {
        String id = exchange.getRequestURI().getPath().replaceFirst("/api/produtos/", "");
        produtoDAO.remover(Integer.parseInt(id));
        sendJson(exchange, 200, "{\"success\":true}");
    }

    private static void handleCreateVenda(HttpExchange exchange) throws IOException, SQLException {
        String body = readRequestBody(exchange);
        Map<String, Object> data = parseJson(body);
        Venda venda = new Venda();
        Object itensObj = data.get("itens");
        if (itensObj instanceof List<?> itens) {
            for (Object itemObj : itens) {
                if (itemObj instanceof Map<?, ?> itemMap) {
                    int produtoId = asInt(itemMap.get("produtoId"), 0);
                    String nome = asString(itemMap.get("nomeProduto"));
                    int quantidade = asInt(itemMap.get("quantidade"), 0);
                    double preco = asDouble(itemMap.get("precoUnitario"), 0);
                    venda.adicionarItem(new ItemVenda(produtoId, nome, quantidade, preco));
                }
            }
        }
        vendaDAO.registrar(venda);
        sendJson(exchange, 201, "{\"success\":true,\"total\":" + venda.getTotal() + "}");
    }

    private static String produtoToJson(Produto produto) {
        return "{"
            + "\"id\":" + produto.getId() + ","
            + "\"nome\":\"" + escapeJson(produto.getNome()) + "\"," 
            + "\"categoriaId\":" + produto.getCategoriaId() + ","
            + "\"preco\":" + produto.getPreco() + ","
            + "\"estoque\":" + produto.getEstoque() + ","
            + "\"estoqueMinimo\":" + produto.getEstoqueMinimo()
            + "}";
    }

    private static Object parseJsonValue(String json, int[] idx) {
        skipWhitespace(json, idx);
        if (idx[0] >= json.length()) {
            return null;
        }
        char c = json.charAt(idx[0]);
        if (c == '"') {
            return parseJsonString(json, idx);
        }
        if (c == '{') {
            return parseJsonObject(json, idx);
        }
        if (c == '[') {
            return parseJsonArray(json, idx);
        }
        if (c == 't' || c == 'f' || c == 'n') {
            return parseJsonLiteral(json, idx);
        }
        return parseJsonNumber(json, idx);
    }

    private static Map<String, Object> parseJson(String json) {
        Object result = parseJsonValue(json, new int[]{0});
        if (result instanceof Map<?, ?> map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> casted = (Map<String, Object>) map;
            return casted;
        }
        return new HashMap<>();
    }

    private static Map<String, Object> parseJsonObject(String json, int[] idx) {
        Map<String, Object> map = new HashMap<>();
        idx[0]++; // skip '{'
        while (true) {
            skipWhitespace(json, idx);
            if (idx[0] >= json.length() || json.charAt(idx[0]) == '}') {
                idx[0]++;
                break;
            }
            String key = parseJsonString(json, idx);
            skipWhitespace(json, idx);
            if (idx[0] < json.length() && json.charAt(idx[0]) == ':') {
                idx[0]++;
            }
            Object value = parseJsonValue(json, idx);
            map.put(key, value);
            skipWhitespace(json, idx);
            if (idx[0] < json.length() && json.charAt(idx[0]) == ',') {
                idx[0]++;
            }
        }
        return map;
    }

    private static List<Object> parseJsonArray(String json, int[] idx) {
        List<Object> list = new ArrayList<>();
        idx[0]++;
        while (true) {
            skipWhitespace(json, idx);
            if (idx[0] >= json.length()) {
                break;
            }
            if (json.charAt(idx[0]) == ']') {
                idx[0]++;
                break;
            }
            Object value = parseJsonValue(json, idx);
            list.add(value);
            skipWhitespace(json, idx);
            if (idx[0] < json.length() && json.charAt(idx[0]) == ',') {
                idx[0]++;
            }
        }
        return list;
    }

    private static String parseJsonString(String json, int[] idx) {
        StringBuilder sb = new StringBuilder();
        idx[0]++;
        while (idx[0] < json.length()) {
            char c = json.charAt(idx[0]++);
            if (c == '"') {
                break;
            }
            if (c == '\\' && idx[0] < json.length()) {
                char next = json.charAt(idx[0]++);
                sb.append(switch (next) {
                    case '"' -> '"';
                    case '\\' -> '\\';
                    case '/' -> '/';
                    case 'b' -> '\b';
                    case 'f' -> '\f';
                    case 'n' -> '\n';
                    case 'r' -> '\r';
                    case 't' -> '\t';
                    default -> next;
                });
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static Object parseJsonLiteral(String json, int[] idx) {
        if (json.startsWith("true", idx[0])) {
            idx[0] += 4;
            return Boolean.TRUE;
        }
        if (json.startsWith("false", idx[0])) {
            idx[0] += 5;
            return Boolean.FALSE;
        }
        if (json.startsWith("null", idx[0])) {
            idx[0] += 4;
            return null;
        }
        return null;
    }

    private static Number parseJsonNumber(String json, int[] idx) {
        int start = idx[0];
        while (idx[0] < json.length()) {
            char c = json.charAt(idx[0]);
            if ((c >= '0' && c <= '9') || c == '-' || c == '+' || c == '.' || c == 'e' || c == 'E') {
                idx[0]++;
                continue;
            }
            break;
        }
        String token = json.substring(start, idx[0]);
        if (token.contains(".") || token.contains("e") || token.contains("E")) {
            try {
                return Double.parseDouble(token);
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        try {
            return Long.parseLong(token);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static void skipWhitespace(String json, int[] idx) {
        while (idx[0] < json.length() && Character.isWhitespace(json.charAt(idx[0]))) {
            idx[0]++;
        }
    }

    private static String asString(Object value) {
        return value == null ? "" : value.toString();
    }

    private static int asInt(Object value, int defaultValue) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(asString(value));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static double asDouble(Object value, double defaultValue) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        try {
            return Double.parseDouble(asString(value));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static String getExtension(String fileName) {
        int idx = fileName.lastIndexOf('.');
        return idx < 0 ? "" : fileName.substring(idx + 1).toLowerCase();
    }

    private static String readRequestBody(HttpExchange exchange) throws IOException {
        try (InputStream input = exchange.getRequestBody()) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static Map<String, String> parseJsonStringMap(String json) {
        Map<String, String> result = new HashMap<>();
        if (json == null || json.isBlank()) {
            return result;
        }
        String trimmed = json.trim();
        if (trimmed.startsWith("{")) {
            trimmed = trimmed.substring(1);
        }
        if (trimmed.endsWith("}")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        String[] pairs = trimmed.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
        for (String pair : pairs) {
            String[] parts = pair.split(":", 2);
            if (parts.length != 2) continue;
            String key = parts[0].trim().replaceAll("^\"|\"$", "");
            String value = parts[1].trim();
            value = value.replaceAll("^\"|\"$", "");
            result.put(key, value);
        }
        return result;
    }

    private static String escapeJson(String input) {
        return input == null ? "" : input.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }

    private static void sendJson(HttpExchange exchange, int code, String body) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static void sendText(HttpExchange exchange, int code, String body) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
