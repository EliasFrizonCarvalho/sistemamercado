const apiBase = '/api';
const searchInput = document.getElementById('homeSearch');
const searchButton = document.getElementById('searchButton');
const clearSearchButton = document.getElementById('clearSearch');
const serverStatus = document.getElementById('serverStatus');
const resultCount = document.getElementById('resultCount');
const searchResults = document.getElementById('searchResults');
let produtos = [];

const categorias = {
    1: 'Mercearia',
    2: 'Bebidas',
    3: 'Limpeza',
    4: 'Hortifruti'
};

async function fetchProdutos() {
    try {
        const res = await fetch(`${apiBase}/produtos`);
        if (!res.ok) {
            throw new Error('Falha ao carregar produtos');
        }
        produtos = await res.json();
        serverStatus.textContent = 'Servidor: online';
        serverStatus.style.color = '#1d4ed8';
        return produtos;
    } catch (error) {
        serverStatus.textContent = 'Servidor: offline';
        serverStatus.style.color = '#991b1b';
        produtos = [];
        return [];
    }
}

function formatCurrency(value) {
    return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(value);
}

function renderResults(lista, query) {
    searchResults.innerHTML = '';
    if (!lista.length) {
        resultCount.textContent = query ? 'Nenhum produto encontrado.' : 'Digite algo para pesquisar produtos.';
        return;
    }

    resultCount.textContent = `Mostrando ${lista.length} produto(s) para “${query}”.`;
    lista.forEach(produto => {
        const item = document.createElement('article');
        item.className = 'result-card';
        item.innerHTML = `
            <h3>${produto.nome} <span>(${categorias[produto.categoriaId] || 'Outro'})</span></h3>
            <p>ID: ${produto.id} · Estoque: ${produto.estoque} · Preço: ${formatCurrency(produto.preco)}</p>
        `;
        searchResults.appendChild(item);
    });
}

function filterProdutos(query) {
    const texto = query.trim().toLowerCase();
    if (!texto) {
        renderResults([], '');
        return;
    }
    const resultados = produtos.filter(produto => {
        const nome = produto.nome.toLowerCase();
        const categoria = categorias[produto.categoriaId]?.toLowerCase() || '';
        return nome.includes(texto)
            || categoria.includes(texto)
            || produto.id.toString() === texto;
    });
    renderResults(resultados, query);
}

async function search() {
    await fetchProdutos();
    filterProdutos(searchInput.value);
}

function resetSearch() {
    searchInput.value = '';
    searchResults.innerHTML = '';
    resultCount.textContent = 'Digite algo para pesquisar produtos.';
}

searchButton.addEventListener('click', search);
clearSearchButton.addEventListener('click', resetSearch);
searchInput.addEventListener('keydown', event => {
    if (event.key === 'Enter') {
        event.preventDefault();
        search();
    }
});

window.addEventListener('load', async () => {
    await fetchProdutos();
    resetSearch();
});
