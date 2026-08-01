import dao.ProdutoDAO;
import dao.VendaDAO;
import db.Conexao;
import model.ItemVenda;
import model.Produto;
import model.Venda;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class Main {
    private final ProdutoDAO produtoDAO = new ProdutoDAO();
    private final VendaDAO vendaDAO = new VendaDAO();
    private final Map<Integer, String> categorias = new LinkedHashMap<>();
    private final NumberFormat moeda = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR"));

    private final DefaultTableModel produtoTableModel = new DefaultTableModel(new Object[]{"ID", "Nome", "Categoria", "Preço", "Estoque", "Estoque mínimo"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final DefaultTableModel vendaTableModel = new DefaultTableModel(new Object[]{"Produto", "Quantidade", "Preço unitário", "Subtotal"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final DefaultTableModel estoqueBaixoTableModel = new DefaultTableModel(new Object[]{"ID", "Nome", "Categoria", "Estoque", "Estoque mínimo"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    private final Venda vendaAtual = new Venda();

    private JFrame frame;
    private JTable tabelaProdutos;
    private JTable tabelaVenda;
    private JTable tabelaEstoqueBaixo;

    private JTextField buscaField;
    private JTextField idField;
    private JTextField nomeField;
    private JComboBox<String> categoriaCombo;
    private JTextField precoField;
    private JTextField estoqueField;
    private JTextField estoqueMinimoField;

    private JTextField vendaProdutoIdField;
    private JTextField vendaQuantidadeField;
    private JLabel totalVendaLabel;
    private JLabel totalVendidoHojeLabel;

    public static void main(String[] args) {
        Conexao.inicializar();
        SwingUtilities.invokeLater(() -> new Main().createAndShowGui());
    }

    public Main() {
        categorias.put(1, "Mercearia");
        categorias.put(2, "Bebidas");
        categorias.put(3, "Limpeza");
        categorias.put(4, "Hortifruti");
    }

    private void createAndShowGui() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }

        frame = new JFrame("Mercado - Sistema de Gestão");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setMinimumSize(new Dimension(1080, 760));
        frame.setLayout(new BorderLayout());
        frame.add(createHeader(), BorderLayout.NORTH);
        frame.add(createContent(), BorderLayout.CENTER);
        frame.add(createFooter(), BorderLayout.SOUTH);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        carregarProdutos("");
        atualizarRelatorios();
    }

    private JPanel createHeader() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(new EmptyBorder(16, 20, 16, 20));
        panel.setBackground(new Color(248, 248, 250));

        JLabel title = new JLabel("Sistema de Mercado");
        title.setFont(new Font("Segoe UI", Font.BOLD, 28));
        JLabel subtitle = new JLabel("Gerencie produtos, estoque e vendas com design limpo e funcional.");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        subtitle.setForeground(Color.DARK_GRAY);

        panel.add(title, BorderLayout.NORTH);
        panel.add(subtitle, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createFooter() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        JLabel rodape = new JLabel("Web API disponível em http://localhost:8080 | Use o app desktop ou o servidor web para portfolio");
        rodape.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        panel.add(rodape);
        return panel;
    }

    private JComponent createContent() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Produtos", createProdutoPanel());
        tabs.addTab("Vendas", createVendaPanel());
        tabs.addTab("Relatórios", createRelatorioPanel());
        return tabs;
    }

    private JPanel createProdutoPanel() {
        JPanel panel = new JPanel(new BorderLayout(14, 14));
        panel.setBorder(new EmptyBorder(12, 12, 12, 12));

        JPanel searchPanel = new JPanel(new BorderLayout(8, 8));
        buscaField = new JTextField();
        buscaField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        JButton botaoBuscar = new JButton("Buscar");
        JButton botaoReset = new JButton("Limpar busca");
        botaoBuscar.addActionListener(e -> carregarProdutos(buscaField.getText().trim()));
        botaoReset.addActionListener(e -> {
            buscaField.setText("");
            carregarProdutos("");
        });

        JPanel searchButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        searchButtons.add(botaoBuscar);
        searchButtons.add(botaoReset);

        searchPanel.add(new JLabel("Buscar por nome ou categoria:"), BorderLayout.WEST);
        searchPanel.add(buscaField, BorderLayout.CENTER);
        searchPanel.add(searchButtons, BorderLayout.EAST);

        tabelaProdutos = new JTable(produtoTableModel);
        tabelaProdutos.setFillsViewportHeight(true);
        tabelaProdutos.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabelaProdutos.getSelectionModel().addListSelectionListener(this::onProdutoSelecionado);
        aplicarRenderizadoresTabela(tabelaProdutos);

        JPanel tabelaPanel = new JPanel(new BorderLayout());
        tabelaPanel.setBorder(BorderFactory.createTitledBorder("Produtos cadastrados"));
        tabelaPanel.add(new JScrollPane(tabelaProdutos), BorderLayout.CENTER);

        JPanel formPanel = createProdutoFormPanel();
        formPanel.setBorder(BorderFactory.createTitledBorder("Detalhes do produto"));

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, tabelaPanel, formPanel);
        split.setResizeWeight(0.70);
        split.setContinuousLayout(true);

        panel.add(searchPanel, BorderLayout.NORTH);
        panel.add(split, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createProdutoFormPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        idField = new JTextField();
        idField.setEditable(false);
        idField.setBackground(Color.WHITE);
        nomeField = new JTextField(24);
        categoriaCombo = new JComboBox<>(categorias.values().toArray(new String[0]));
        precoField = new JTextField(12);
        estoqueField = new JTextField(10);
        estoqueMinimoField = new JTextField(10);

        addFormField(panel, gbc, 0, "ID:", idField);
        addFormField(panel, gbc, 1, "Nome:", nomeField);
        addFormField(panel, gbc, 2, "Categoria:", categoriaCombo);
        addFormField(panel, gbc, 3, "Preço (R$):", precoField);
        addFormField(panel, gbc, 4, "Estoque:", estoqueField);
        addFormField(panel, gbc, 5, "Estoque mínimo:", estoqueMinimoField);

        JButton botaoCadastrar = new JButton("Cadastrar");
        JButton botaoAtualizar = new JButton("Atualizar");
        JButton botaoRemover = new JButton("Remover");
        JButton botaoLimpar = new JButton("Novo produto");
        botaoCadastrar.addActionListener(e -> cadastrarProduto());
        botaoAtualizar.addActionListener(e -> atualizarProduto());
        botaoRemover.addActionListener(e -> removerProduto());
        botaoLimpar.addActionListener(e -> limparFormulario());

        JPanel botoes = new JPanel(new GridLayout(2, 2, 10, 10));
        botoes.add(botaoCadastrar);
        botoes.add(botaoAtualizar);
        botoes.add(botaoRemover);
        botoes.add(botaoLimpar);

        gbc.gridy = 6;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.weightx = 1;
        panel.add(botoes, gbc);
        return panel;
    }

    private JPanel createVendaPanel() {
        JPanel panel = new JPanel(new BorderLayout(14, 14));
        panel.setBorder(new EmptyBorder(12, 12, 12, 12));

        JPanel vendaForm = new JPanel(new GridBagLayout());
        vendaForm.setBorder(BorderFactory.createTitledBorder("Registro de venda"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        vendaProdutoIdField = new JTextField(10);
        vendaQuantidadeField = new JTextField(8);
        addFormField(vendaForm, gbc, 0, "ID do produto:", vendaProdutoIdField);
        addFormField(vendaForm, gbc, 1, "Quantidade:", vendaQuantidadeField);

        JButton botaoAdicionar = new JButton("Adicionar item");
        JButton botaoFinalizar = new JButton("Finalizar venda");
        JButton botaoLimpar = new JButton("Limpar carrinho");
        botaoAdicionar.addActionListener(e -> adicionarItemVenda());
        botaoFinalizar.addActionListener(e -> finalizarVenda());
        botaoLimpar.addActionListener(e -> cancelarVenda());

        JPanel botoes = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        botoes.add(botaoAdicionar);
        botoes.add(botaoFinalizar);
        botoes.add(botaoLimpar);

        totalVendaLabel = new JLabel("Total da venda: R$ 0,00");
        totalVendaLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));

        tabelaVenda = new JTable(vendaTableModel);
        tabelaVenda.setFillsViewportHeight(true);
        aplicarRenderizadoresTabela(tabelaVenda);

        panel.add(vendaForm, BorderLayout.NORTH);
        panel.add(new JScrollPane(tabelaVenda), BorderLayout.CENTER);
        panel.add(botoes, BorderLayout.SOUTH);
        panel.add(totalVendaLabel, BorderLayout.PAGE_END);
        return panel;
    }

    private JPanel createRelatorioPanel() {
        JPanel panel = new JPanel(new BorderLayout(14, 14));
        panel.setBorder(new EmptyBorder(12, 12, 12, 12));

        totalVendidoHojeLabel = new JLabel("Total vendido hoje: R$ 0,00");
        totalVendidoHojeLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));

        JButton botaoAtualizar = new JButton("Atualizar relatório");
        botaoAtualizar.addActionListener(e -> atualizarRelatorios());

        JPanel topo = new JPanel(new BorderLayout(10, 10));
        topo.add(totalVendidoHojeLabel, BorderLayout.WEST);
        topo.add(botaoAtualizar, BorderLayout.EAST);

        tabelaEstoqueBaixo = new JTable(estoqueBaixoTableModel);
        tabelaEstoqueBaixo.setFillsViewportHeight(true);
        aplicarRenderizadoresTabela(tabelaEstoqueBaixo);

        JPanel tabelaPanel = new JPanel(new BorderLayout());
        tabelaPanel.setBorder(BorderFactory.createTitledBorder("Estoque em atenção"));
        tabelaPanel.add(new JScrollPane(tabelaEstoqueBaixo), BorderLayout.CENTER);

        panel.add(topo, BorderLayout.NORTH);
        panel.add(tabelaPanel, BorderLayout.CENTER);
        return panel;
    }

    private void aplicarRenderizadoresTabela(JTable tabela) {
        DefaultTableCellRenderer alignRight = new DefaultTableCellRenderer();
        alignRight.setHorizontalAlignment(SwingConstants.RIGHT);
        tabela.getColumnModel().getColumn(3).setCellRenderer(alignRight);
    }

    private void addFormField(JPanel panel, GridBagConstraints gbc, int row, String label, Component field) {
        gbc.gridy = row;
        gbc.gridx = 0;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        panel.add(new JLabel(label), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        panel.add(field, gbc);
    }

    private void carregarProdutos(String filtro) {
        produtoTableModel.setRowCount(0);
        try {
            List<Produto> produtos = produtoDAO.listarTodos();
            String termo = filtro == null ? "" : filtro.toLowerCase(Locale.ROOT);
            for (Produto produto : produtos) {
                String categoria = categorias.getOrDefault(produto.getCategoriaId(), "-");
                if (!termo.isBlank() && !produto.getNome().toLowerCase(Locale.ROOT).contains(termo) && !categoria.toLowerCase(Locale.ROOT).contains(termo)) {
                    continue;
                }
                produtoTableModel.addRow(new Object[]{produto.getId(), produto.getNome(), categoria, moeda.format(produto.getPreco()), produto.getEstoque(), produto.getEstoqueMinimo()});
            }
        } catch (SQLException e) {
            showError("Erro ao carregar produtos: " + e.getMessage());
        }
    }

    private void cadastrarProduto() {
        try {
            Produto produto = lerProdutoDoFormulario();
            produtoDAO.inserir(produto);
            showInfo("Produto cadastrado com sucesso.");
            limparFormulario();
            carregarProdutos(buscaField.getText().trim());
            atualizarRelatorios();
        } catch (Exception e) {
            showError("Erro ao cadastrar produto: " + e.getMessage());
        }
    }

    private void atualizarProduto() {
        try {
            int id = Integer.parseInt(idField.getText());
            Produto produto = lerProdutoDoFormulario();
            produto.setId(id);
            produtoDAO.atualizar(produto);
            showInfo("Produto atualizado com sucesso.");
            limparFormulario();
            carregarProdutos(buscaField.getText().trim());
            atualizarRelatorios();
        } catch (NumberFormatException e) {
            showError("Selecione um produto válido antes de atualizar.");
        } catch (Exception e) {
            showError("Erro ao atualizar produto: " + e.getMessage());
        }
    }

    private void removerProduto() {
        try {
            int id = Integer.parseInt(idField.getText());
            produtoDAO.remover(id);
            showInfo("Produto removido com sucesso.");
            limparFormulario();
            carregarProdutos(buscaField.getText().trim());
            atualizarRelatorios();
        } catch (NumberFormatException e) {
            showError("Selecione um produto válido antes de remover.");
        } catch (Exception e) {
            showError("Erro ao remover produto: " + e.getMessage());
        }
    }

    private Produto lerProdutoDoFormulario() {
        String nome = nomeField.getText().trim();
        int categoriaId = categoriaCombo.getSelectedIndex() + 1;
        double preco = Double.parseDouble(precoField.getText().trim().replace(',', '.'));
        int estoque = Integer.parseInt(estoqueField.getText().trim());
        int estoqueMinimo = Integer.parseInt(estoqueMinimoField.getText().trim());

        if (nome.isEmpty()) {
            throw new IllegalArgumentException("O nome do produto não pode ficar em branco.");
        }
        if (preco < 0) {
            throw new IllegalArgumentException("O preço deve ser maior ou igual a zero.");
        }
        if (estoque < 0 || estoqueMinimo < 0) {
            throw new IllegalArgumentException("O estoque e o estoque mínimo devem ser valores positivos.");
        }

        return new Produto(nome, categoriaId, preco, estoque, estoqueMinimo);
    }

    private void limparFormulario() {
        idField.setText("");
        nomeField.setText("");
        categoriaCombo.setSelectedIndex(0);
        precoField.setText("");
        estoqueField.setText("");
        estoqueMinimoField.setText("");
    }

    private void adicionarItemVenda() {
        try {
            int produtoId = Integer.parseInt(vendaProdutoIdField.getText().trim());
            int quantidade = Integer.parseInt(vendaQuantidadeField.getText().trim());
            if (quantidade <= 0) {
                showError("A quantidade deve ser maior que zero.");
                return;
            }

            Produto produto = produtoDAO.buscarPorId(produtoId);
            if (produto == null) {
                showError("Produto não encontrado.");
                return;
            }
            if (quantidade > produto.getEstoque()) {
                showError("Estoque insuficiente. Disponível: " + produto.getEstoque());
                return;
            }

            ItemVenda item = new ItemVenda(produto.getId(), produto.getNome(), quantidade, produto.getPreco());
            vendaAtual.adicionarItem(item);
            vendaTableModel.addRow(new Object[]{produto.getNome(), quantidade, moeda.format(produto.getPreco()), moeda.format(item.getQuantidade() * item.getPrecoUnitario())});
            atualizarTotalVenda();
            vendaProdutoIdField.setText("");
            vendaQuantidadeField.setText("");
        } catch (NumberFormatException e) {
            showError("Informe um ID de produto e quantidade válidos.");
        } catch (SQLException e) {
            showError("Erro ao adicionar item à venda: " + e.getMessage());
        }
    }

    private void finalizarVenda() {
        if (vendaAtual.getItens().isEmpty()) {
            showError("Adicione ao menos um item à venda antes de finalizar.");
            return;
        }
        try {
            int vendaId = vendaDAO.registrar(vendaAtual);
            showInfo("Venda registrada com sucesso. ID: " + vendaId + " Total: " + moeda.format(vendaAtual.getTotal()));
            cancelarVenda();
            carregarProdutos(buscaField.getText().trim());
            atualizarRelatorios();
        } catch (SQLException e) {
            showError("Erro ao finalizar venda: " + e.getMessage());
        }
    }

    private void cancelarVenda() {
        vendaAtual.getItens().clear();
        vendaAtual.setTotal(0);
        vendaTableModel.setRowCount(0);
        atualizarTotalVenda();
        vendaProdutoIdField.setText("");
        vendaQuantidadeField.setText("");
    }

    private void atualizarTotalVenda() {
        totalVendaLabel.setText("Total da venda: " + moeda.format(vendaAtual.getTotal()));
    }

    private void atualizarRelatorios() {
        estoqueBaixoTableModel.setRowCount(0);
        try {
            List<Produto> lista = produtoDAO.estoqueBaixo();
            for (Produto produto : lista) {
                estoqueBaixoTableModel.addRow(new Object[]{produto.getId(), produto.getNome(), categorias.getOrDefault(produto.getCategoriaId(), "-"), produto.getEstoque(), produto.getEstoqueMinimo()});
            }
            totalVendidoHojeLabel.setText("Total vendido hoje: " + moeda.format(vendaDAO.totalVendidoHoje()));
        } catch (SQLException e) {
            showError("Erro ao atualizar relatórios: " + e.getMessage());
        }
    }

    private void showError(String mensagem) {
        JOptionPane.showMessageDialog(frame, mensagem, "Erro", JOptionPane.ERROR_MESSAGE);
    }

    private void showInfo(String mensagem) {
        JOptionPane.showMessageDialog(frame, mensagem, "Sucesso", JOptionPane.INFORMATION_MESSAGE);
    }

    private void onProdutoSelecionado(ListSelectionEvent event) {
        if (event.getValueIsAdjusting() || tabelaProdutos.getSelectedRow() < 0) {
            return;
        }
        int linha = tabelaProdutos.getSelectedRow();
        idField.setText(String.valueOf(tabelaProdutos.getValueAt(linha, 0)));
        nomeField.setText(String.valueOf(tabelaProdutos.getValueAt(linha, 1)));
        categoriaCombo.setSelectedItem(String.valueOf(tabelaProdutos.getValueAt(linha, 2)));
        String preco = String.valueOf(tabelaProdutos.getValueAt(linha, 3)).replace("R$", "").trim();
        precoField.setText(preco);
        estoqueField.setText(String.valueOf(tabelaProdutos.getValueAt(linha, 4)));
        estoqueMinimoField.setText(String.valueOf(tabelaProdutos.getValueAt(linha, 5)));
    }
}
