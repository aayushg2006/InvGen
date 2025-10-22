document.addEventListener('DOMContentLoaded', () => {
    
    const tableBody = document.getElementById('product-table-body');
    const modal = document.getElementById('productModal');
    const modalTitle = document.getElementById('modal-title');
    const addProductBtn = document.getElementById('addProductBtn');
    const closeBtn = modal.querySelector('.modal-close');
    const cancelBtn = modal.querySelector('.modal-cancel');
    const productForm = document.querySelector('#productModal form');

    let allProducts = [];
    let editingProductId = null;

    const showModal = () => modal.classList.add('active');
    const hideModal = () => {
        modal.classList.remove('active');
        productForm.reset();
        editingProductId = null;
    };

    addProductBtn.addEventListener('click', () => {
        editingProductId = null;
        modalTitle.textContent = 'Add New Product';
        showModal();
    });
    
    closeBtn.addEventListener('click', hideModal);
    cancelBtn.addEventListener('click', hideModal);
    modal.addEventListener('click', (e) => {
        if (e.target === modal) hideModal();
    });

    async function loadProducts() {
        try {
            allProducts = await fetchWithAuth('/products');
            renderProducts();
        } catch (error) {
            // Error handled by fetchWithAuth
        }
    }

    function renderProducts() {
        tableBody.innerHTML = '';
        allProducts.forEach(product => {
            const costPriceDisplay = product.costPrice ? `₹${product.costPrice.toFixed(2)}` : 'N/A';
            
            let stockDisplay = 'N/A';
            let stockClass = '';
            if (product.quantityInStock !== null && product.quantityInStock !== undefined) {
                stockDisplay = product.quantityInStock;
                const threshold = product.lowStockThreshold || 10;
                if (product.quantityInStock <= threshold) {
                    stockClass = 'status-overdue';
                }
            }

            const row = `
                <tr>
                    <td>${product.id}</td>
                    <td>${product.name}</td>
                    <td>${costPriceDisplay}</td>
                    <td>₹${product.sellingPrice.toFixed(2)}</td>
                    <td>${product.categoryName}</td>
                    <td>${product.gstPercentage}%</td>
                    <td class="${stockClass}">${stockDisplay}</td>
                    <td class="table-actions">
                        <a href="#" class="edit-btn" data-id="${product.id}" title="Edit"><i class="fas fa-pen"></i></a>
                    </td>
                </tr>
            `;
            tableBody.innerHTML += row;
        });
    }

    productForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        
        const productData = {
            name: document.getElementById('productName').value,
            sellingPrice: document.getElementById('sellingPrice').value,
            costPrice: document.getElementById('costPrice').value,
            quantityInStock: document.getElementById('quantityInStock').value,
            lowStockThreshold: document.getElementById('lowStockThreshold').value,
            categoryId: document.getElementById('productCategory').value,
        };

        try {
            if (editingProductId) {
                await fetchWithAuth(`/products/${editingProductId}`, {
                    method: 'PUT',
                    body: JSON.stringify(productData),
                });
            } else {
                await fetchWithAuth('/products', {
                    method: 'POST',
                    body: JSON.stringify(productData),
                });
            }
            hideModal();
            loadProducts();
        } catch (error) {
            alert(`Failed to save product: ${error.message}`);
        }
    });

    tableBody.addEventListener('click', (e) => {
        const editBtn = e.target.closest('.edit-btn');
        if (editBtn) {
            e.preventDefault();
            const productId = editBtn.dataset.id;
            const productToEdit = allProducts.find(p => p.id == productId);

            if (productToEdit) {
                editingProductId = productId;

                document.getElementById('productName').value = productToEdit.name;
                document.getElementById('quantityInStock').value = productToEdit.quantityInStock || '';
                document.getElementById('lowStockThreshold').value = productToEdit.lowStockThreshold || '';
                document.getElementById('costPrice').value = productToEdit.costPrice || '';
                document.getElementById('sellingPrice').value = productToEdit.sellingPrice;
                document.getElementById('productCategory').value = productToEdit.categoryId;

                modalTitle.textContent = 'Edit Product';
                showModal();
            }
        }
    });

    const categorySelect = document.getElementById('productCategory');
    const categories = [
        { id: 1, name: 'Daily Essentials' }, { id: 2, name: 'Dairy & Spreads' },
        { id: 3, name: 'Packaged Snacks' }, { id: 4, name: 'Electronics' },
        { id: 5, name: 'Hardware' }, { id: 6, name: 'Automotive Parts' },
        { id: 7, name: 'Apparel' }, { id: 8, name: 'Stationery' }
    ];
    categories.forEach(cat => {
        const option = document.createElement('option');
        option.value = cat.id;
        option.textContent = cat.name;
        categorySelect.appendChild(option);
    });

    loadProducts();
});