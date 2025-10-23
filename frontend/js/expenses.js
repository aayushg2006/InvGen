document.addEventListener('DOMContentLoaded', () => {
    const tableBody = document.getElementById('expenses-table-body');
    const modal = document.getElementById('expenseModal');
    const modalTitle = document.getElementById('modal-title');
    const addExpenseBtn = document.getElementById('addExpenseBtn');
    const closeBtn = modal.querySelector('.modal-close');
    const cancelBtn = modal.querySelector('.modal-cancel');
    const expenseForm = modal.querySelector('form');

    let allExpenses = [];
    let editingExpenseId = null;

    const showModal = () => modal.classList.add('active');
    const hideModal = () => {
        modal.classList.remove('active');
        expenseForm.reset();
        editingExpenseId = null;
        modalTitle.textContent = 'Add New Expense';
    };

    addExpenseBtn.addEventListener('click', () => {
        document.getElementById('expenseDate').valueAsDate = new Date();
        showModal();
    });
    
    closeBtn.addEventListener('click', hideModal);
    cancelBtn.addEventListener('click', hideModal);
    modal.addEventListener('click', (e) => {
        if (e.target === modal) hideModal();
    });

    async function loadExpenses() {
        try {
            allExpenses = await fetchWithAuth('/expenses');
            renderExpenses();
        } catch (error) {
            console.error("Failed to load expenses:", error);
        }
    }

    function renderExpenses() {
        tableBody.innerHTML = '';
        if (allExpenses.length === 0) {
            tableBody.innerHTML = '<tr><td colspan="5" style="text-align: center;">No expenses logged yet.</td></tr>';
            return;
        }
        allExpenses.forEach(expense => {
            const row = `
                <tr>
                    <td>${new Date(expense.date).toLocaleDateString()}</td>
                    <td>${expense.description}</td>
                    <td>${expense.category || 'N/A'}</td>
                    <td>₹${expense.amount.toFixed(2)}</td>
                    <td class="table-actions">
                        <a href="#" class="edit-btn" data-id="${expense.id}" title="Edit"><i class="fas fa-pen"></i></a>
                        <a href="#" class="delete-btn" data-id="${expense.id}" title="Delete"><i class="fas fa-trash"></i></a>
                    </td>
                </tr>
            `;
            tableBody.innerHTML += row;
        });
    }

    expenseForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        
        const expenseData = {
            description: document.getElementById('expenseDescription').value,
            amount: document.getElementById('expenseAmount').value,
            date: document.getElementById('expenseDate').value,
            category: document.getElementById('expenseCategory').value,
        };

        try {
            if (editingExpenseId) {
                await fetchWithAuth(`/expenses/${editingExpenseId}`, {
                    method: 'PUT',
                    body: JSON.stringify(expenseData),
                });
            } else {
                await fetchWithAuth('/expenses', {
                    method: 'POST',
                    body: JSON.stringify(expenseData),
                });
            }
            hideModal();
            loadExpenses();
        } catch (error) {
            alert(`Failed to save expense: ${error.message}`);
        }
    });

    tableBody.addEventListener('click', async (e) => {
        const editBtn = e.target.closest('.edit-btn');
        const deleteBtn = e.target.closest('.delete-btn');

        if (editBtn) {
            e.preventDefault();
            const expenseId = editBtn.dataset.id;
            const expenseToEdit = allExpenses.find(ex => ex.id == expenseId);

            if (expenseToEdit) {
                editingExpenseId = expenseId;
                document.getElementById('expenseDescription').value = expenseToEdit.description;
                document.getElementById('expenseAmount').value = expenseToEdit.amount;
                document.getElementById('expenseDate').value = expenseToEdit.date;
                document.getElementById('expenseCategory').value = expenseToEdit.category;
                modalTitle.textContent = 'Edit Expense';
                showModal();
            }
        }

        if (deleteBtn) {
            e.preventDefault();
            const expenseId = deleteBtn.dataset.id;
            if (confirm('Are you sure you want to delete this expense?')) {
                try {
                    await fetchWithAuth(`/expenses/${expenseId}`, { method: 'DELETE' });
                    loadExpenses();
                } catch (error) {
                    alert(`Failed to delete expense: ${error.message}`);
                }
            }
        }
    });

    loadExpenses();
});