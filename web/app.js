const apiBase = '/api';
let saleItems = [];
let saleTotal = 0;

const elements = {
    status: document.getElementById('server-status'),
    productTable: document.querySelector('#productTable tbody'),
    refreshProducts: document.getElementById('refreshProducts'),
    productName: document.getElementById('productName'),
    productCategory: document.getElementById('productCategory'),
    productPrice: document.getElementById('productPrice'),
    productStock: document.getElementById('productStock'),
    productMinStock: document.getElementById('productMinStock'),
    saveProduct: document.getElementById('saveProduct'),
    saleProductId: document.getElementById('saleProductId'),
    saleQuantity: document.getElementById('saleQuantity'),
    addSaleItem: document.getElementById('addSaleItem'),
    saleList: document.getElementById('saleList'),
    saleTotal: document.getElementById('saleTotal'),
    finalizeSale: document.getElementById('finalizeSale'),
    cleanSale: document.getElementById('cleanSale'),
    todayTotal: document.getElementById('todayTotal'),
    lowStockCount: document.getElementById('lowStockCount')
};

const formatCurrency = value => new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(value);

async function apiFetch(path, options = {}) {
    try {
        const res = await fetch(path, options);
        if (!res.ok) {
            throw new Error(`Erro ${res.status}`);
        }
        return res.json();
    } catch (error) {
        elements.status.textContent = `Servidor: offline`; 
        elements.status.style.background = '#fee2e2';
        elements.status.style.color = '#991b1b';
        throw error;
    }
}

async function loadProducts() {
    const products = await apiFetch(`${apiBase}/produtos`);
    elements.productTable.innerHTML = '';
    products.forEach(produto => {
        const row = document.createElement('tr');
        row.innerHTML = `
            <td>${produto.id}</td>
            <td>${produto.nome}</td>
            <td>${categoriaLabel(produto.categoriaId)}</td>
            <td>${formatCurrency(produto.preco)}</td>
            <td>${produto.estoque}</td>
            <td>${produto.estoqueMinimo}</td>
        `;
        elements.productTable.appendChild(row);
    });
    updateStatusOnline();
}

async function loadReports() {
    const [today, lowStock] = await Promise.all([
        apiFetch(`${apiBase}/total-hoje`),
        apiFetch(`${apiBase}/estoque-baixo`)
    ]);
    elements.todayTotal.textContent = formatCurrency(today.totalHoje);
    elements.lowStockCount.textContent = `${lowStock.length} produto(s)`;
}

function categoriaLabel(id) {
    return {1:'Mercearia',2:'Bebidas',3:'Limpeza',4:'Hortifruti'}[id] || 'Outro';
}

function updateStatusOnline() {
    elements.status.textContent = 'Servidor: online';
    elements.status.style.background = '#dbeafe';
    elements.status.style.color = '#1d4ed8';
}

async function saveProduct() {
    const produto = {
        nome: elements.productName.value.trim(),
        categoriaId: Number(elements.productCategory.value),
        preco: Number(elements.productPrice.value),
        estoque: Number(elements.productStock.value),
        estoqueMinimo: Number(elements.productMinStock.value)
    };
    if (!produto.nome || produto.preco < 0 || produto.estoque < 0 || produto.estoqueMinimo < 0) {
        return alert('Preencha os dados do produto corretamente.');
    }
    await apiFetch(`${apiBase}/produtos`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(produto)
    });
    elements.productName.value = '';
    elements.productPrice.value = '';
    elements.productStock.value = '0';
    elements.productMinStock.value = '5';
    await loadProducts();
    await loadReports();
    alert('Produto cadastrado com sucesso!');
}

function addSaleItem() {
    const produtoId = Number(elements.saleProductId.value);
    const quantidade = Number(elements.saleQuantity.value);
    if (!produtoId || quantidade <= 0) {
        return alert('Informe o ID do produto e quantidade válidos.');
    }
    saleItems.push({ produtoId, quantidade });
    renderSaleItems();
    elements.saleProductId.value = '';
    elements.saleQuantity.value = '1';
}

function renderSaleItems() {
    elements.saleList.innerHTML = '';
    saleTotal = 0;
    saleItems.forEach((item, index) => {
        const li = document.createElement('li');
        li.textContent = `Produto ID ${item.produtoId} — Qtde ${item.quantidade}`;
        const remove = document.createElement('button');
        remove.textContent = 'Remover';
        remove.className = 'secondary';
        remove.addEventListener('click', () => {
            saleItems.splice(index, 1);
            renderSaleItems();
        });
        li.appendChild(remove);
        elements.saleList.appendChild(li);
    });
    elements.saleTotal.textContent = formatCurrency(saleTotal);
}

async function finalizeSale() {
    if (!saleItems.length) {
        return alert('Adicione ao menos um item à venda.');
    }
    const itens = await Promise.all(saleItems.map(async item => {
        const response = await apiFetch(`${apiBase}/produtos`);
        const produto = response.find(p => p.id === item.produtoId);
        if (!produto) throw new Error('Produto não encontrado: ' + item.produtoId);
        return { produtoId: item.produtoId, nomeProduto: produto.nome, quantidade: item.quantidade, precoUnitario: produto.preco };
    }));
    const result = await apiFetch(`${apiBase}/vendas`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ itens })
    });
    alert(`Venda registrada! Total: ${formatCurrency(result.total)}`);
    saleItems = [];
    renderSaleItems();
    await loadProducts();
    await loadReports();
}

function cleanSale() {
    saleItems = [];
    renderSaleItems();
}

function attachEvents() {
    elements.refreshProducts.addEventListener('click', loadProducts);
    elements.saveProduct.addEventListener('click', saveProduct);
    elements.addSaleItem.addEventListener('click', addSaleItem);
    elements.finalizeSale.addEventListener('click', finalizeSale);
    elements.cleanSale.addEventListener('click', cleanSale);
}

async function init() {
    attachEvents();
    try {
        await loadProducts();
        await loadReports();
    } catch (error) {
        console.error(error);
    }
}

init();
