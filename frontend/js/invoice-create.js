document.addEventListener('DOMContentLoaded', async () => {
    // --- Existing Elements ---
    const itemsTableBody = document.getElementById('invoice-items-body');
    const itemTemplate = document.getElementById('invoice-item-template');
    const addItemBtn = document.getElementById('addItemBtn');
    const customerInput = document.getElementById('customerName');
    const customerDataList = document.getElementById('customer-list');
    const statusSelect = document.getElementById('paymentStatus');
    const invoiceForm = document.getElementById('invoiceForm');
    const initialPaymentGroup = document.getElementById('initial-payment-group');
    const initialAmountPaidInput = document.getElementById('initialAmountPaid');
    const creditAlert = document.getElementById('credit-balance-alert');
    const creditBalanceAmount = document.getElementById('credit-balance-amount');
    const applyCreditCheckbox = document.getElementById('applyCredit');
    const creditAppliedRow = document.getElementById('credit-applied-row');
    const creditAppliedSpan = document.getElementById('credit-applied');
    const balanceDueRow = document.getElementById('balance-due-row');
    const balanceDueSpan = document.getElementById('balance-due');
    const paymentMethodSelect = document.getElementById('paymentMethod');
    const saveAndGenerateBtn = document.querySelector('button[type="submit"]');
    const saveAndEmailBtn = document.getElementById('saveAndEmailBtn');

    // --- NEW QR Code Modal Elements ---
    const qrModal = document.getElementById('qrModal');
    const qrCodeContainer = document.getElementById('qrcode');
    const qrStatusMessage = document.getElementById('qr-status-message');
    const qrCloseBtn = qrModal.querySelector('.modal-close');
    let qrCodeInstance = null;
    let paymentCheckInterval = null;

    let products = [];
    let customers = [];
    let currentCustomerCredit = 0;
    
    try {
        products = await fetchWithAuth('/products');
        customers = await fetchWithAuth('/customers');
        customers.forEach(customer => {
            const option = document.createElement('option');
            option.value = customer.name;
            option.dataset.id = customer.id;
            option.dataset.phone = customer.phoneNumber || '';
            option.dataset.email = customer.email || '';
            customerDataList.appendChild(option);
        });
    } catch (error) { /* Handled */ }

    const checkCustomerCredit = async () => {
        const enteredCustomerName = customerInput.value;
        const selectedOption = Array.from(customerDataList.options).find(opt => opt.value === enteredCustomerName);

        creditAlert.style.display = 'none';
        applyCreditCheckbox.checked = false;
        currentCustomerCredit = 0;

        if (selectedOption) {
            const customerId = selectedOption.dataset.id;
            document.getElementById('customerPhone').value = selectedOption.dataset.phone;
            document.getElementById('customerEmail').value = selectedOption.dataset.email;

            try {
                const creditData = await fetchWithAuth(`/credits/customer/${customerId}`);
                if (creditData && creditData.balance > 0) {
                    currentCustomerCredit = creditData.balance;
                    creditBalanceAmount.textContent = `₹${currentCustomerCredit.toFixed(2)}`;
                    creditAlert.style.display = 'block';
                }
            } catch (error) {
                console.error("Failed to fetch customer credit:", error);
            }
        }
        calculateTotals();
    };

    customerInput.addEventListener('change', checkCustomerCredit);
    applyCreditCheckbox.addEventListener('change', calculateTotals);

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
        let subtotal = 0, totalGst = 0;
        itemsTableBody.querySelectorAll('tr').forEach(row => {
            const price = parseFloat(row.querySelector('.item-price').value) || 0;
            const qty = parseInt(row.querySelector('.item-qty').value) || 0;
            const discount = parseFloat(row.querySelector('.item-discount').value) || 0;
            const product = products.find(p => p.id == row.querySelector('.item-select').value);
            
            if (product) {
                const discountedPrice = price * (1 - discount / 100);
                const itemSubtotal = discountedPrice * qty;
                const itemGst = itemSubtotal * (product.gstPercentage / 100);
                subtotal += itemSubtotal;
                totalGst += itemGst;
                row.querySelector('.item-total').textContent = `₹${itemSubtotal.toFixed(2)}`;
            }
        });
        
        const grandTotal = subtotal + totalGst;
        document.getElementById('subtotal').textContent = `₹${subtotal.toFixed(2)}`;
        document.getElementById('gst').textContent = `₹${totalGst.toFixed(2)}`;
        document.getElementById('grandTotal').textContent = `₹${grandTotal.toFixed(2)}`;

        if (applyCreditCheckbox.checked && currentCustomerCredit > 0) {
            const creditToApply = Math.min(grandTotal, currentCustomerCredit);
            const balanceDue = grandTotal - creditToApply;
            creditAppliedSpan.textContent = `-₹${creditToApply.toFixed(2)}`;
            balanceDueSpan.textContent = `₹${balanceDue.toFixed(2)}`;
            creditAppliedRow.style.display = 'grid';
            balanceDueRow.style.display = 'grid';
        } else {
            creditAppliedRow.style.display = 'none';
            balanceDueRow.style.display = 'none';
        }
    }

    addItemBtn.addEventListener('click', addNewItem);

    itemsTableBody.addEventListener('change', (e) => {
        if (e.target.classList.contains('item-select')) {
            const option = e.target.options[e.target.selectedIndex];
            e.target.closest('tr').querySelector('.item-price').value = parseFloat(option.dataset.price || 0).toFixed(2);
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

    const getInvoiceData = () => {
        const items = [];
        itemsTableBody.querySelectorAll('tr').forEach(row => {
            const productId = row.querySelector('.item-select').value;
            if (productId) items.push({
                productId,
                quantity: row.querySelector('.item-qty').value,
                discountPercentage: row.querySelector('.item-discount').value
            });
        });

        if (items.length === 0) {
            alert('Please add at least one item to the invoice.');
            return null;
        }

        const invoiceData = {
            status: statusSelect.value,
            items,
            paymentMethod: document.getElementById('paymentMethod').value,
            applyCredit: applyCreditCheckbox.checked
        };

        if (statusSelect.value === 'PARTIALLY_PAID') {
            invoiceData.initialAmountPaid = initialAmountPaidInput.value;
        }

        const existingCustomer = customers.find(c => c.name.toLowerCase() === customerInput.value.toLowerCase());
        if (existingCustomer) {
            invoiceData.customerId = existingCustomer.id;
        } else {
            invoiceData.newCustomerName = customerInput.value;
            invoiceData.newCustomerPhone = document.getElementById('customerPhone').value;
            invoiceData.newCustomerEmail = document.getElementById('customerEmail').value;
        }
        return invoiceData;
    };

    // --- NEW: Modal Control for QR Code ---
    const showQrModal = (paymentLink) => {
        qrCodeContainer.innerHTML = ''; // Clear previous QR code
        qrCodeInstance = new QRCode(qrCodeContainer, {
            text: paymentLink,
            width: 256,
            height: 256,
        });
        qrStatusMessage.textContent = 'Waiting for payment confirmation...';
        qrStatusMessage.style.color = 'inherit';
        qrModal.classList.add('active');
    };

    const hideQrModal = () => {
        if (paymentCheckInterval) {
            clearInterval(paymentCheckInterval);
        }
        qrModal.classList.remove('active');
    };

    qrCloseBtn.addEventListener('click', hideQrModal);

    // --- NEW: Function to check invoice status ---
    const startPaymentStatusCheck = (invoiceId) => {
        paymentCheckInterval = setInterval(async () => {
            try {
                const invoice = await fetchWithAuth(`/invoices/${invoiceId}`);
                if (invoice.status === 'PAID' || invoice.status === 'PARTIALLY_PAID') {
                    clearInterval(paymentCheckInterval);
                    qrStatusMessage.textContent = 'Payment Successful!';
                    qrStatusMessage.style.color = '#10B981';
                    setTimeout(() => {
                        hideQrModal();
                        window.location.href = `invoice-detail.html?id=${invoiceId}`;
                    }, 2000);
                }
            } catch (error) {
                console.error('Error checking payment status:', error);
            }
        }, 3000); // Check every 3 seconds
    };

    // --- UPDATED: Event listener to change button text ---
    [paymentMethodSelect, statusSelect].forEach(el => {
        el.addEventListener('change', () => {
            const paymentMethod = paymentMethodSelect.value;
            const status = statusSelect.value;
            initialPaymentGroup.style.display = status === 'PARTIALLY_PAID' ? 'block' : 'none';

            if ((status === 'PAID' || status === 'PARTIALLY_PAID') && (paymentMethod === 'UPI')) {
                saveAndGenerateBtn.innerHTML = '<i class="fas fa-qrcode"></i> Generate QR Code';
            } else {
                saveAndGenerateBtn.innerHTML = '<i class="fas fa-save"></i> Save and Generate';
            }
        });
    });

    // --- UPDATED: Main form submission logic ---
    invoiceForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        const invoiceData = getInvoiceData();
        if (!invoiceData) return;

        const paymentMethod = paymentMethodSelect.value;
        const status = statusSelect.value;

        // --- NEW WORKFLOW for Real-time QR Code ---
        if ((status === 'PAID' || status === 'PARTIALLY_PAID') && (paymentMethod === 'UPI')) {
            try {
                const response = await fetchWithAuth('/invoices/create-for-payment', {
                    method: 'POST',
                    body: JSON.stringify(invoiceData)
                });
                showQrModal(response.paymentLink);
                startPaymentStatusCheck(response.invoiceId);
            } catch (error) {
                alert(`Failed to generate QR Code: ${error.message}`);
            }
            return; // Stop here for the QR code flow
        }

        // --- ORIGINAL WORKFLOW for manual save ---
        try {
            const token = getToken();
            const response = await fetch(`${API_BASE_URL}/invoices`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${token}` },
                body: JSON.stringify(invoiceData)
            });

            if (!response.ok) throw new Error(await response.text());

            const blob = await response.blob();
            const url = window.URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.style.display = 'none';
            a.href = url;
            
            const contentDisposition = response.headers.get('content-disposition');
            let fileName = 'invoice.pdf';
            if (contentDisposition) {
                const fileNameMatch = contentDisposition.match(/filename="(.+)"/);
                if (fileNameMatch?.[1]) fileName = fileNameMatch[1];
            }
            
            a.download = fileName;
            document.body.appendChild(a);
            a.click();
            window.URL.revokeObjectURL(url);
            a.remove();
            
            alert('Invoice created and downloaded successfully!');
            window.location.href = 'invoices-list.html';
        } catch (error) {
            alert(`Failed to create invoice: ${error.message}`);
        }
    });

    saveAndEmailBtn.addEventListener('click', async () => {
        const invoiceData = getInvoiceData();
        if (!invoiceData) return;

        try {
            const response = await fetchWithAuth('/invoices/create-and-send', {
                method: 'POST',
                body: JSON.stringify(invoiceData)
            });
            alert(response);
            window.location.href = 'invoices-list.html';
        } catch (error) {
            alert(`Failed to create and send invoice: ${error.message}`);
        }
    });

    document.getElementById('issueDate').valueAsDate = new Date();
    addNewItem();
});