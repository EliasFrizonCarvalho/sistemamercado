-- Schema do Sistema de Mercado (SQLite)

CREATE TABLE IF NOT EXISTS categorias (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    nome TEXT NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS produtos (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    nome TEXT NOT NULL,
    categoria_id INTEGER,
    preco REAL NOT NULL,
    estoque INTEGER NOT NULL DEFAULT 0,
    estoque_minimo INTEGER NOT NULL DEFAULT 5,
    FOREIGN KEY (categoria_id) REFERENCES categorias(id)
);

CREATE TABLE IF NOT EXISTS vendas (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    data_venda TEXT NOT NULL,
    total REAL NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS itens_venda (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    venda_id INTEGER NOT NULL,
    produto_id INTEGER NOT NULL,
    quantidade INTEGER NOT NULL,
    preco_unitario REAL NOT NULL,
    FOREIGN KEY (venda_id) REFERENCES vendas(id),
    FOREIGN KEY (produto_id) REFERENCES produtos(id)
);

INSERT OR IGNORE INTO categorias (id, nome) VALUES
 (1,'Mercearia'), (2,'Bebidas'), (3,'Limpeza'), (4,'Hortifruti');
