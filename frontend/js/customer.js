document.addEventListener('DOMContentLoaded', () => {
    const tableBody = document.getElementById('customers-table-body');
    const modal = document.getElementById('customerModal');
    const addCustomerBtn = document.getElementById('addCustomerBtn');
    const closeBtn = modal.querySelector('.modal-close');
    const cancelBtn = modal.querySelector('.modal-cancel');
    const customerForm = document.querySelector('#customerModal form');
    let allCustomers = [];
    let editingCustomerId = null;

    // --- Modal Control ---
    const showModal = () => modal.classList.add('active');
    const hideModal = () => {
        modal.classList.remove('active');
        customerForm.reset();
        editingCustomerId = null; // Reset editing state
    };

    addCustomerBtn.addEventListener('click', () => {
        editingCustomerId = null;
        modal.querySelector('h2').textContent = 'Add New Customer';
        showModal();
    });

    closeBtn.addEventListener('click', hideModal);
    cancelBtn.addEventListener('click', hideModal);
    modal.addEventListener('click', (e) => {
        if (e.target === modal) hideModal();
    });

    // --- API & Rendering ---
    async function loadCustomers() {
        try {
            allCustomers = await fetchWithAuth('/customers');
            renderCustomers();
        } catch (error) { /* Handled by fetchWithAuth */ }
    }

    function renderCustomers() {
        tableBody.innerHTML = '';
        allCustomers.forEach(customer => {
            // Format the credit balance for display
            const creditBalanceDisplay = customer.creditBalance ? `₹${customer.creditBalance.toFixed(2)}` : '₹0.00';

            const row = `
                <tr>
                    <td>${customer.name}</td>
                    <td>${customer.phoneNumber || 'N/A'}</td>
                    <td>${customer.email || 'N/A'}</td>
                    <td>${creditBalanceDisplay}</td>
                    <td class="table-actions">
                        <a href="#" class="edit-btn" data-id="${customer.id}" title="Edit"><i class="fas fa-pen"></i></a>
                        <a href="#" class="delete-btn" data-id="${customer.id}" title="Delete"><i class="fas fa-trash"></i></a>
                    </td>
                </tr>
            `;
            tableBody.innerHTML += row;
        });
    }
    
    // --- Event Handlers ---
    customerForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        const customerData = {
            name: document.getElementById('customerNameModal').value,
            phoneNumber: document.getElementById('customerPhone').value,
            email: document.getElementById('customerEmail').value,
        };

        try {
            if (editingCustomerId) {
                // Update existing customer
                await fetchWithAuth(`/customers/${editingCustomerId}`, {
                    method: 'PUT',
                    body: JSON.stringify(customerData),
                });
            } else {
                // Create new customer
                await fetchWithAuth('/customers', {
                    method: 'POST',
                    body: JSON.stringify(customerData),
                });
            }
            hideModal();
            loadCustomers();
        } catch (error) {
            alert(`Failed to save customer: ${error.message}`);
        }
    });

    tableBody.addEventListener('click', async (e) => {
        const editBtn = e.target.closest('.edit-btn');
        const deleteBtn = e.target.closest('.delete-btn');

        if (editBtn) {
            e.preventDefault();
            editingCustomerId = editBtn.dataset.id;
            const customer = allCustomers.find(c => c.id == editingCustomerId);
            
            document.getElementById('customerNameModal').value = customer.name;
            document.getElementById('customerPhone').value = customer.phoneNumber;
            document.getElementById('customerEmail').value = customer.email;
            
            modal.querySelector('h2').textContent = 'Edit Customer';
            showModal();
        }

        if (deleteBtn) {
            e.preventDefault();
            const customerId = deleteBtn.dataset.id;
            if (confirm('Are you sure you want to delete this customer?')) {
                try {
                    await fetchWithAuth(`/customers/${customerId}`, { method: 'DELETE' });
                    loadCustomers();
                } catch (error) {
                    alert(`Failed to delete customer: ${error.message}`);
                }
            }
        }
    });

    // --- Initial Load ---
    loadCustomers();
});