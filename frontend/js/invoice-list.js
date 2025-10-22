document.addEventListener('DOMContentLoaded', async () => {
    const tableBody = document.getElementById('invoices-table-body');
    const searchInput = document.getElementById('searchInput');
    
    // --- Payment Modal Elements ---
    const paymentModal = document.getElementById('paymentModal');
    const paymentModalTitle = document.getElementById('payment-modal-title');
    const paymentAmountInput = document.getElementById('paymentAmount');
    const paymentCloseBtn = paymentModal.querySelector('.modal-close');
    const paymentCancelBtn = paymentModal.querySelector('.modal-cancel');
    const paymentForm = paymentModal.querySelector('form');
    const overpaymentOptions = document.getElementById('overpayment-options');
    const overpaymentLabel = document.getElementById('overpayment-label');
    const sendReceiptCheckbox = document.getElementById('sendReceipt');

    // --- Email Modal Elements ---
    const emailModal = document.getElementById('emailModal');
    const emailModalTitle = document.getElementById('email-modal-title');
    const emailToInput = document.getElementById('emailTo');
    const emailMessageInput = document.getElementById('emailMessage');
    const emailCloseBtn = emailModal.querySelector('.modal-close');
    const emailCancelBtn = emailModal.querySelector('.modal-cancel');
    const emailForm = emailModal.querySelector('form');

    let allInvoices = [];
    let currentlyPayingInvoice = null;
    let currentlyEmailingInvoice = null;

    // --- Payment Modal Control ---
    const showPaymentModal = (invoice) => {
        currentlyPayingInvoice = invoice;
        paymentModalTitle.textContent = `Record Payment for ${invoice.invoiceNumber}`;
        
        // Correctly determine the balance to pre-fill
        let balanceToPay = invoice.balanceDue;
        if (balanceToPay === null || invoice.status === 'PENDING') {
            balanceToPay = invoice.totalAmount;
        }
        
        paymentAmountInput.value = balanceToPay > 0 ? balanceToPay.toFixed(2) : '';
        overpaymentOptions.style.display = 'none';
        paymentModal.classList.add('active');
    };
    const hidePaymentModal = () => {
        paymentModal.classList.remove('active');
        paymentForm.reset();
        currentlyPayingInvoice = null;
    };

    // --- THIS IS THE CORRECTED LOGIC ---
    paymentAmountInput.addEventListener('input', () => {
        if (!currentlyPayingInvoice) return;
        
        const amountPaid = parseFloat(paymentAmountInput.value) || 0;
        
        // Determine the correct current balance
        let currentBalance = currentlyPayingInvoice.balanceDue;
        if (currentBalance === null || currentlyPayingInvoice.status === 'PENDING') {
            currentBalance = currentlyPayingInvoice.totalAmount;
        }

        if (amountPaid > currentBalance) {
            const overpayment = amountPaid - currentBalance;
            overpaymentLabel.textContent = `Overpayment of ₹${overpayment.toFixed(2)} detected. How would you like to handle it?`;
            overpaymentOptions.style.display = 'block';
        } else {
            overpaymentOptions.style.display = 'none';
        }
    });

    // --- (Email Modal Control is unchanged) ---
    const showEmailModal = async (invoice) => {
        currentlyEmailingInvoice = invoice;
        emailModalTitle.textContent = `Send Invoice ${invoice.invoiceNumber}`;
        try {
            const customerDetails = await fetchWithAuth(`/customers`);
            const customer = customerDetails.find(c => c.name === invoice.customerName);
            emailToInput.value = customer ? customer.email : '';
        } catch (error) {
            console.error("Could not fetch customer email", error);
            emailToInput.value = '';
        }
        emailModal.classList.add('active');
    };
    const hideEmailModal = () => {
        emailModal.classList.remove('active');
        emailForm.reset();
        currentlyEmailingInvoice = null;
    };

    paymentCloseBtn.addEventListener('click', hidePaymentModal);
    paymentCancelBtn.addEventListener('click', hidePaymentModal);
    paymentModal.addEventListener('click', (e) => {
        if (e.target === paymentModal) hidePaymentModal();
    });

    emailCloseBtn.addEventListener('click', hideEmailModal);
    emailCancelBtn.addEventListener('click', hideEmailModal);
    emailModal.addEventListener('click', (e) => {
        if (e.target === emailModal) hideEmailModal();
    });

    async function loadInvoices() {
        try {
            allInvoices = await fetchWithAuth('/invoices');
            renderInvoices(allInvoices);
        } catch (error) { /* Handled by fetchWithAuth */ }
    }

    function renderInvoices(invoicesToRender) {
        tableBody.innerHTML = '';
        if (invoicesToRender.length === 0) {
            tableBody.innerHTML = `<tr><td colspan="7" style="text-align:center;">No invoices found.</td></tr>`;
            return;
        }
        invoicesToRender.forEach(invoice => {
            const statusClass = getStatusClass(invoice.status);
            
            // Correctly display the balance due
            let balanceDueText = 'N/A';
            if (invoice.balanceDue !== null) {
                balanceDueText = `₹${invoice.balanceDue.toFixed(2)}`;
            } else if (invoice.status === 'PENDING') {
                balanceDueText = `₹${invoice.totalAmount.toFixed(2)}`;
            }

            let actionHtml = '';
            if (invoice.status === 'PENDING' || invoice.status === 'PARTIALLY_PAID') {
                actionHtml = `<button class="btn btn-primary record-payment-btn" data-invoice-id="${invoice.id}">Record Payment</button>`;
            } else if (invoice.status === 'PAID') {
                actionHtml = '<span>Fully Paid</span>';
            } else {
                 actionHtml = '<span>Cancelled</span>';
            }

            const row = `
                <tr>
                    <td><a href="invoice-detail.html?id=${invoice.id}" title="View Details">${invoice.invoiceNumber}</a></td>
                    <td>${invoice.customerName}</td>
                    <td>${new Date(invoice.issueDate).toLocaleDateString()}</td>
                    <td>₹${invoice.totalAmount.toFixed(2)}</td>
                    <td>${balanceDueText}</td>
                    <td><span class="status-badge ${statusClass}">${invoice.status.replace('_', ' ')}</span></td>
                    <td class="table-actions">
                        ${actionHtml}
                        <a href="#" title="Send Email" class="action-icon email-btn" data-invoice-id="${invoice.id}">
                            <i class="fas fa-paper-plane"></i>
                        </a>
                        <a href="invoice-detail.html?id=${invoice.id}" title="View Details" class="action-icon">
                            <i class="fas fa-eye"></i>
                        </a>
                    </td>
                </tr>
            `;
            tableBody.innerHTML += row;
        });
    }

    function getStatusClass(status) {
        if (status === 'PAID') return 'status-paid';
        if (status === 'PENDING') return 'status-pending';
        if (status === 'PARTIALLY_PAID') return 'status-partially-paid';
        return 'status-overdue'; // For CANCELLED
    }
    
    tableBody.addEventListener('click', (e) => {
        const paymentBtn = e.target.closest('.record-payment-btn');
        const emailBtn = e.target.closest('.email-btn');

        if (paymentBtn) {
            const invoiceId = paymentBtn.dataset.invoiceId;
            const invoice = allInvoices.find(inv => inv.id == invoiceId);
            if (invoice) {
                showPaymentModal(invoice);
            }
        }

        if (emailBtn) {
            e.preventDefault();
            const invoiceId = emailBtn.dataset.invoiceId;
            const invoice = allInvoices.find(inv => inv.id == invoiceId);
            if (invoice) {
                showEmailModal(invoice);
            }
        }
    });
    
    paymentForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        if (!currentlyPayingInvoice) return;

        const paymentData = {
            amount: paymentAmountInput.value,
            paymentMethod: document.getElementById('paymentMethod').value,
            sendReceipt: sendReceiptCheckbox.checked,
            overpaymentChoice: document.querySelector('input[name="overpaymentChoice"]:checked').value
        };

        try {
            await fetchWithAuth(`/invoices/${currentlyPayingInvoice.id}/payments`, {
                method: 'POST',
                body: JSON.stringify(paymentData),
            });
            hidePaymentModal();
            loadInvoices(); 
        } catch (error) {
            alert(`Failed to record payment: ${error.message}`);
        }
    });

    emailForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        if (!currentlyEmailingInvoice) return;

        const emailData = {
            to: emailToInput.value,
            customMessage: emailMessageInput.value,
        };

        try {
            const response = await fetchWithAuth(`/invoices/${currentlyEmailingInvoice.id}/email`, {
                method: 'POST',
                body: JSON.stringify(emailData),
            });
            alert(response); 
            hideEmailModal();
        } catch (error) {
            alert(`Failed to send email: ${error.message}`);
        }
    });

    searchInput.addEventListener('input', (e) => {
        const searchTerm = e.target.value.toLowerCase();
        const filteredInvoices = allInvoices.filter(invoice =>
            (invoice.customerName && invoice.customerName.toLowerCase().includes(searchTerm)) ||
            (invoice.invoiceNumber && invoice.invoiceNumber.toLowerCase().includes(searchTerm))
        );
        renderInvoices(filteredInvoices);
    });

    loadInvoices();
});