const STORAGE_PRODUCTS = 'mercado_demo_products';
const STORAGE_SALES = 'mercado_demo_sales';
const categories = {
    1: 'Mercearia',
    2: 'Bebidas',
    3: 'Limpeza',
    4: 'Hortifruti'
};

const elements = {
    searchProduct: document.getElementById('searchProduct'),
    productTableBody: document.querySelector('#productTable tbody'),
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

const defaultProducts = [
    { id: 1, nome: 'Arroz Integral 5kg', categoriaId: 1, preco: 29.90, estoque: 28, estoqueMinimo: 8 },
    { id: 2, nome: 'Refrigerante Cola 2L', categoriaId: 2, preco: 12.50, estoque: 14, estoqueMinimo: 5 },
    { id: 3, nome: 'Detergente Líquido 500ml', categoriaId: 3, preco: 5.70, estoque: 22, estoqueMinimo: 6 },
    { id: 4, nome: 'Banana Nanica 1kg', categoriaId: 4, preco: 7.80, estoque: 16, estoqueMinimo: 8 },
    { id: 5, nome: 'Pão de Forma Integral', categoriaId: 1, preco: 10.40, estoque: 11, estoqueMinimo: 4 }
];

function getProducts() {
    const products = localStorage.getItem(STORAGE_PRODUCTS);
    return products ? JSON.parse(products) : [...defaultProducts];
}

function saveProducts(products) {
    localStorage.setItem(STORAGE_PRODUCTS, JSON.stringify(products));
}

function getSales() {
    const sales = localStorage.getItem(STORAGE_SALES);
    return sales ? JSON.parse(sales) : [];
}

function saveSales(sales) {
    localStorage.setItem(STORAGE_SALES, JSON.stringify(sales));
}

function formatCurrency(value) {
    return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(value);
}

function getNextProductId(products) {
    return products.reduce((max, product) => Math.max(max, product.id), 0) + 1;
}

function renderProducts(products) {
    elements.productTableBody.innerHTML = '';
    products.forEach(produto => {
        const row = document.createElement('tr');
        row.innerHTML = `
            <td>${produto.id}</td>
            <td>${produto.nome}</td>
            <td>${categories[produto.categoriaId] || 'Outro'}</td>
            <td>${formatCurrency(produto.preco)}</td>
            <td>${produto.estoque}</td>
            <td>${produto.estoqueMinimo}</td>
        `;
        elements.productTableBody.appendChild(row);
    });
}

function filterProducts(query) {
    const normalized = query.trim().toLowerCase();
    const products = getProducts();
    if (!normalized) {
        return products;
    }
    return products.filter(produto => {
        const nome = produto.nome.toLowerCase();
        const categoria = (categories[produto.categoriaId] || '').toLowerCase();
        return nome.includes(normalized)
            || categoria.includes(normalized)
            || produto.id.toString() === normalized;
    });
}

function updateReports() {
    const products = getProducts();
    const sales = getSales();
    const totalHoje = sales
        .filter(venda => isToday(new Date(venda.data)))
        .reduce((sum, venda) => sum + venda.total, 0);

    const lowStock = products.filter(produto => produto.estoque <= produto.estoqueMinimo);
    elements.todayTotal.textContent = formatCurrency(totalHoje);
    elements.lowStockCount.textContent = `${lowStock.length} produto(s)`;
}

function isToday(date) {
    const today = new Date();
    return date.getFullYear() === today.getFullYear()
        && date.getMonth() === today.getMonth()
        && date.getDate() === today.getDate();
}

let saleItems = [];

function renderSaleItems() {
    elements.saleList.innerHTML = '';
    let total = 0;
    saleItems.forEach((item, index) => {
        const li = document.createElement('li');
        li.textContent = `ID ${item.produtoId} — ${item.nomeProduto} x ${item.quantidade}`;
        const remove = document.createElement('button');
        remove.textContent = 'Remover';
        remove.className = 'button button-secondary';
        remove.addEventListener('click', () => {
            saleItems.splice(index, 1);
            renderSaleItems();
        });
        li.appendChild(remove);
        elements.saleList.appendChild(li);
        total += item.precoUnitario * item.quantidade;
    });
    elements.saleTotal.textContent = formatCurrency(total);
}

function addProduct() {
    const nome = elements.productName.value.trim();
    const categoriaId = Number(elements.productCategory.value);
    const preco = Number(elements.productPrice.value);
    const estoque = Number(elements.productStock.value);
    const estoqueMinimo = Number(elements.productMinStock.value);

    if (!nome || preco < 0 || estoque < 0 || estoqueMinimo < 0) {
        return alert('Preencha todos os campos corretamente.');
    }

    const products = getProducts();
    const product = {
        id: getNextProductId(products),
        nome,
        categoriaId,
        preco,
        estoque,
        estoqueMinimo
    };
    products.push(product);
    saveProducts(products);
    renderProducts(filterProducts(elements.searchProduct.value));
    updateReports();
    elements.productName.value = '';
    elements.productPrice.value = '';
    elements.productStock.value = '0';
    elements.productMinStock.value = '5';
    alert('Produto salvo com sucesso!');
}

function addSaleItem() {
    const produtoId = Number(elements.saleProductId.value);
    const quantidade = Number(elements.saleQuantity.value);
    const products = getProducts();
    const produto = products.find(p => p.id === produtoId);

    if (!produto) {
        return alert('Produto não encontrado.');
    }
    if (quantidade <= 0) {
        return alert('Informe uma quantidade válida.');
    }
    if (quantidade > produto.estoque) {
        return alert('Estoque insuficiente para este produto.');
    }

    saleItems.push({
        produtoId,
        nomeProduto: produto.nome,
        quantidade,
        precoUnitario: produto.preco
    });
    renderSaleItems();
    elements.saleProductId.value = '';
    elements.saleQuantity.value = '1';
}

function finalizeSale() {
    if (!saleItems.length) {
        return alert('Adicione itens à venda antes de finalizar.');
    }

    const products = getProducts();
    const updatedProducts = [...products];
    const items = saleItems.map(item => {
        const produto = updatedProducts.find(p => p.id === item.produtoId);
        produto.estoque -= item.quantidade;
        return item;
    });

    const total = items.reduce((sum, item) => sum + item.precoUnitario * item.quantidade, 0);
    const sales = getSales();
    sales.push({ data: new Date().toISOString(), itens: items, total });

    saveProducts(updatedProducts);
    saveSales(sales);
    saleItems = [];
    renderSaleItems();
    renderProducts(filterProducts(elements.searchProduct.value));
    updateReports();
    alert(`Venda finalizada: ${formatCurrency(total)}`);
}

function clearSale() {
    saleItems = [];
    renderSaleItems();
}

function attachEvents() {
    elements.refreshProducts.addEventListener('click', () => renderProducts(filterProducts(elements.searchProduct.value)));
    elements.searchProduct.addEventListener('input', () => renderProducts(filterProducts(elements.searchProduct.value)));
    elements.saveProduct.addEventListener('click', addProduct);
    elements.addSaleItem.addEventListener('click', addSaleItem);
    elements.finalizeSale.addEventListener('click', finalizeSale);
    elements.cleanSale.addEventListener('click', clearSale);
}

function initDashboard() {
    if (!localStorage.getItem(STORAGE_PRODUCTS)) {
        saveProducts(defaultProducts);
    }
    renderProducts(getProducts());
    updateReports();
    attachEvents();
}

initDashboard();
