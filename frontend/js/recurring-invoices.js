document.addEventListener('DOMContentLoaded', async () => {
    const profilesTableBody = document.getElementById('recurring-profiles-body');
    const itemsTableBody = document.getElementById('recurring-items-body');
    const itemTemplate = document.getElementById('recurring-item-template');
    const addItemBtn = document.getElementById('addItemBtn');
    const customerInput = document.getElementById('customerName');
    const customerDataList = document.getElementById('customer-list');
    const recurringForm = document.getElementById('recurringForm');
    const formTitle = document.querySelector('.card-header h2');
    const submitButton = document.querySelector('#recurringForm button[type="submit"]');
    const autoSendCheckbox = document.getElementById('autoSendEmail');

    let products = [];
    let customers = [];
    let allProfiles = [];
    let editingProfileId = null;

    async function loadProfiles() {
        try {
            allProfiles = await fetchWithAuth('/recurring-invoices');
            renderProfiles();
        } catch (error) {
            console.error("Failed to load recurring profiles:", error);
        }
    }

    function renderProfiles() {
        profilesTableBody.innerHTML = '';
        if (allProfiles.length === 0) {
            profilesTableBody.innerHTML = '<tr><td colspan="5" style="text-align: center;">No recurring profiles found.</td></tr>';
            return;
        }

        allProfiles.forEach(profile => {
            const row = `
                <tr>
                    <td>${profile.customerName}</td>
                    <td>${profile.frequency}</td>
                    <td>${new Date(profile.startDate).toLocaleDateString()}</td>
                    <td>${new Date(profile.nextIssueDate).toLocaleDateString()}</td>
                    <td class="table-actions">
                        <a href="#" class="edit-btn" data-id="${profile.id}" title="Edit"><i class="fas fa-pen"></i></a>
                        <a href="#" class="delete-btn" data-id="${profile.id}" title="Delete"><i class="fas fa-trash"></i></a>
                    </td>
                </tr>
            `;
            profilesTableBody.innerHTML += row;
        });
    }

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
        console.error("Failed to load initial data:", error);
    }

    function addNewItem(itemToEdit = null) {
        const newRow = itemTemplate.content.cloneNode(true);
        const selectElement = newRow.querySelector('.item-select');
        selectElement.innerHTML = '<option value="">Select a product</option>';
        products.forEach(product => {
            const option = document.createElement('option');
            option.value = product.id;
            option.textContent = product.name;
            selectElement.appendChild(option);
        });

        if (itemToEdit) {
            selectElement.value = itemToEdit.productId;
            newRow.querySelector('.item-qty').value = itemToEdit.quantity;
            newRow.querySelector('.item-discount').value = itemToEdit.discountPercentage;
        }

        itemsTableBody.appendChild(newRow);
    }

    addItemBtn.addEventListener('click', () => addNewItem());

    itemsTableBody.addEventListener('click', (e) => {
        if (e.target.closest('.btn-delete')) {
            e.target.closest('tr').remove();
        }
    });
    
    profilesTableBody.addEventListener('click', async (e) => {
        const deleteBtn = e.target.closest('.delete-btn');
        const editBtn = e.target.closest('.edit-btn');
        
        if (editBtn) {
            e.preventDefault();
            const profileId = editBtn.dataset.id;
            const profileToEdit = allProfiles.find(p => p.id == profileId);
            
            if (profileToEdit) {
                editingProfileId = profileId;
                customerInput.value = profileToEdit.customerName;
                document.getElementById('frequency').value = profileToEdit.frequency;
                document.getElementById('startDate').value = profileToEdit.startDate;
                document.getElementById('endDate').value = profileToEdit.endDate || '';
                autoSendCheckbox.checked = profileToEdit.autoSendEmail;
                
                itemsTableBody.innerHTML = '';
                if (profileToEdit.items.length > 0) {
                    profileToEdit.items.forEach(item => addNewItem(item));
                } else {
                    addNewItem();
                }
                
                submitButton.innerHTML = '<i class="fas fa-save"></i> Update Profile';
                formTitle.textContent = 'Edit Recurring Profile';
                window.scrollTo(0, document.body.scrollHeight);
            }
        }
        
        if (deleteBtn) {
            e.preventDefault();
            const profileId = deleteBtn.dataset.id;
            
            if (confirm('Are you sure you want to delete this recurring profile? This action cannot be undone.')) {
                try {
                    await fetchWithAuth(`/recurring-invoices/${profileId}`, { method: 'DELETE' });
                    alert('Profile deleted successfully.');
                    loadProfiles();
                } catch (error) {
                    alert(`Failed to delete profile: ${error.message}`);
                }
            }
        }
    });

    recurringForm.addEventListener('submit', async (e) => {
        e.preventDefault();

        const selectedCustomerOption = Array.from(customerDataList.options).find(opt => opt.value === customerInput.value);
        if (!selectedCustomerOption) {
            alert('Please select a valid customer from the list.');
            return;
        }

        const items = [];
        itemsTableBody.querySelectorAll('tr').forEach(row => {
            const productId = row.querySelector('.item-select').value;
            if (productId) {
                items.push({
                    productId,
                    quantity: parseInt(row.querySelector('.item-qty').value),
                    discountPercentage: parseFloat(row.querySelector('.item-discount').value) || 0
                });
            }
        });

        if (items.length === 0) {
            alert('Please add at least one item to the profile.');
            return;
        }

        const recurringData = {
            customerId: selectedCustomerOption.dataset.id,
            frequency: document.getElementById('frequency').value,
            startDate: document.getElementById('startDate').value,
            endDate: document.getElementById('endDate').value || null,
            autoSendEmail: autoSendCheckbox.checked,
            items: items
        };
        
        if (!recurringData.startDate) {
            alert('Please select a start date.');
            return;
        }

        try {
            if (editingProfileId) {
                await fetchWithAuth(`/recurring-invoices/${editingProfileId}`, {
                    method: 'PUT',
                    body: JSON.stringify(recurringData)
                });
                alert('Recurring profile updated successfully!');
            } else {
                await fetchWithAuth('/recurring-invoices', {
                    method: 'POST',
                    body: JSON.stringify(recurringData)
                });
                alert('Recurring profile saved successfully!');
            }
            
            editingProfileId = null;
            recurringForm.reset();
            itemsTableBody.innerHTML = '';
            addNewItem();
            submitButton.innerHTML = '<i class="fas fa-save"></i> Save Profile';
            formTitle.textContent = 'Create New Recurring Profile';
            loadProfiles();
        } catch (error) {
            alert(`Failed to save profile: ${error.message}`);
        }
    });

    addNewItem(); 
    loadProfiles(); 
});