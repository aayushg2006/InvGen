document.addEventListener('DOMContentLoaded', async () => {
    const itemsTableBody = document.getElementById('quote-items-body');
    const itemTemplate = document.getElementById('quote-item-template');
    const addItemBtn = document.getElementById('addItemBtn');
    const customerInput = document.getElementById('customerName');
    const customerDataList = document.getElementById('customer-list');
    const quoteForm = document.getElementById('quoteForm');
    let products = [];
    let customers = [];
    
    try {
        products = await fetchWithAuth('/products');
        customers = await fetchWithAuth('/customers');

        customers.forEach(customer => {
            const option = document.createElement('option');
            option.value = customer.name;
            option.dataset.id = customer.id;
            customerDataList.appendChild(option);
        });
    } catch (error) {
        // Errors are handled in fetchWithAuth
    }

    function addNewItem() {
        const newRow = itemTemplate.content.cloneNode(true);
        const selectElement = newRow.querySelector('.item-select');
        selectElement.innerHTML = '<option value="">Select a product</option>';
        products.forEach(product => {
            const option = document.createElement('option');
            option.value = product.id;
            option.textContent = product.name;
            option.dataset.price = product.sellingPrice;
            selectElement.appendChild(option);
        });
        itemsTableBody.appendChild(newRow);
    }

    function calculateTotals() {
        let subtotal = 0;
        let totalGst = 0;
        itemsTableBody.querySelectorAll('tr').forEach(row => {
            const price = parseFloat(row.querySelector('.item-price').value) || 0;
            const qty = parseInt(row.querySelector('.item-qty').value) || 0;
            const discount = parseFloat(row.querySelector('.item-discount').value) || 0;
            const selectedProductId = row.querySelector('.item-select').value;
            const product = products.find(p => p.id == selectedProductId);
            
            if (product) {
                const discountAmount = price * (discount / 100);
                const discountedPrice = price - discountAmount;
                const itemSubtotal = discountedPrice * qty;
                const gstRate = product.gstPercentage / 100;
                const itemGst = itemSubtotal * gstRate;
                subtotal += itemSubtotal;
                totalGst += itemGst;
                row.querySelector('.item-total').textContent = `₹${(itemSubtotal).toFixed(2)}`;
            }
        });
        
        document.getElementById('subtotal').textContent = `₹${subtotal.toFixed(2)}`;
        document.getElementById('gst').textContent = `₹${totalGst.toFixed(2)}`;
        document.getElementById('grandTotal').textContent = `₹${(subtotal + totalGst).toFixed(2)}`;
    }

    addItemBtn.addEventListener('click', addNewItem);

    itemsTableBody.addEventListener('change', (e) => {
        if (e.target.classList.contains('item-select')) {
            const price = e.target.options[e.target.selectedIndex].dataset.price || 0;
            e.target.closest('tr').querySelector('.item-price').value = parseFloat(price).toFixed(2);
        }
        calculateTotals();
    });

    itemsTableBody.addEventListener('input', (e) => {
        if (e.target.classList.contains('item-qty') || e.target.classList.contains('item-discount')) {
            calculateTotals();
        }
    });

    itemsTableBody.addEventListener('click', (e) => {
        if (e.target.closest('.btn-delete')) {
            e.target.closest('tr').remove();
            calculateTotals();
        }
    });

    quoteForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        
        const items = [];
        itemsTableBody.querySelectorAll('tr').forEach(row => {
            const productId = row.querySelector('.item-select').value;
            if (productId) {
                items.push({
                    productId: productId,
                    quantity: row.querySelector('.item-qty').value,
                    discountPercentage: row.querySelector('.item-discount').value
                });
            }
        });

        if (items.length === 0) {
            alert('Please add at least one item to the quote.');
            return;
        }

        const quoteData = { items: items };

        const enteredCustomerName = customerInput.value;
        const existingCustomer = customers.find(c => c.name.toLowerCase() === enteredCustomerName.toLowerCase());

        if (existingCustomer) {
            quoteData.customerId = existingCustomer.id;
        } else {
            quoteData.newCustomerName = enteredCustomerName;
            quoteData.newCustomerPhone = document.getElementById('customerPhone').value;
            quoteData.newCustomerEmail = document.getElementById('customerEmail').value;
        }

        try {
            await fetchWithAuth('/quotes', {
                method: 'POST',
                body: JSON.stringify(quoteData)
            });
            alert('Quote created successfully!');
            window.location.href = 'quotes-list.html';
        } catch (error) {
            alert(`Failed to create quote: ${error.message}`);
        }
    });

    document.getElementById('issueDate').valueAsDate = new Date();
    addNewItem();
});