const STORAGE_PRODUCTS = 'mercado_demo_products';
const searchInput = document.getElementById('homeSearch');
const searchButton = document.getElementById('searchButton');
const clearSearchButton = document.getElementById('clearSearch');
const resultCount = document.getElementById('resultCount');
const searchResults = document.getElementById('searchResults');

const categorias = {
    1: 'Mercearia',
    2: 'Bebidas',
    3: 'Limpeza',
    4: 'Hortifruti'
};

const defaultProducts = [
    { id: 1, nome: 'Arroz Integral 5kg', categoriaId: 1, preco: 29.90, estoque: 28, estoqueMinimo: 8 },
    { id: 2, nome: 'Refrigerante Cola 2L', categoriaId: 2, preco: 12.50, estoque: 14, estoqueMinimo: 5 },
    { id: 3, nome: 'Detergente Líquido 500ml', categoriaId: 3, preco: 5.70, estoque: 22, estoqueMinimo: 6 },
    { id: 4, nome: 'Banana Nanica 1kg', categoriaId: 4, preco: 7.80, estoque: 16, estoqueMinimo: 8 },
    { id: 5, nome: 'Pão de Forma Integral', categoriaId: 1, preco: 10.40, estoque: 11, estoqueMinimo: 4 },
    { id: 6, nome: 'Leite Integral 1L', categoriaId: 1, preco: 6.25, estoque: 19, estoqueMinimo: 6 }
];

function getProducts() {
    const stored = localStorage.getItem(STORAGE_PRODUCTS);
    return stored ? JSON.parse(stored) : defaultProducts;
}

function saveProducts(products) {
    localStorage.setItem(STORAGE_PRODUCTS, JSON.stringify(products));
}

function formatCurrency(value) {
    return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(value);
}

function renderResults(produtos, query) {
    searchResults.innerHTML = '';
    if (!produtos.length) {
        resultCount.textContent = query ? `Nenhum produto encontrado para “${query}”.` : 'Digite para buscar produtos.';
        return;
    }

    resultCount.textContent = `Mostrando ${produtos.length} produto(s) para “${query}”.`;
    produtos.forEach(produto => {
        const item = document.createElement('article');
        item.className = 'result-card';
        item.innerHTML = `
            <h3>${produto.nome} <span>(${categorias[produto.categoriaId] || 'Outro'})</span></h3>
            <p>ID: ${produto.id} · Estoque: ${produto.estoque} · Preço: ${formatCurrency(produto.preco)}</p>
        `;
        searchResults.appendChild(item);
    });
}

function searchProducts() {
    const query = searchInput.value.trim().toLowerCase();
    const produtos = getProducts();
    if (!query) {
        renderResults(produtos.slice(0, 4), 'destaque');
        return;
    }

    const results = produtos.filter(produto => {
        const nome = produto.nome.toLowerCase();
        const categoria = (categorias[produto.categoriaId] || '').toLowerCase();
        return nome.includes(query)
            || categoria.includes(query)
            || produto.id.toString() === query;
    });

    renderResults(results, query);
}

function clearSearch() {
    searchInput.value = '';
    renderResults(getProducts().slice(0, 4), 'destaque');
}

searchButton.addEventListener('click', searchProducts);
clearSearchButton.addEventListener('click', clearSearch);
searchInput.addEventListener('keydown', event => {
    if (event.key === 'Enter') {
        event.preventDefault();
        searchProducts();
    }
});

window.addEventListener('load', () => {
    if (!localStorage.getItem(STORAGE_PRODUCTS)) {
        saveProducts(defaultProducts);
    }
    clearSearch();
});
