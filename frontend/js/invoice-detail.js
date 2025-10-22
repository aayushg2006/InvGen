document.addEventListener('DOMContentLoaded', async () => {
    const invoiceContent = document.getElementById('invoice-content');
    
    const urlParams = new URLSearchParams(window.location.search);
    const invoiceId = urlParams.get('id');

    if (!invoiceId) {
        invoiceContent.innerHTML = `<p>Error: No invoice ID provided.</p>`;
        return;
    }

    try {
        const invoice = await fetchWithAuth(`/invoices/${invoiceId}`);
        
        const formatDate = (dateString) => new Date(dateString).toLocaleDateString();

        let itemsHtml = '';
        invoice.items.forEach(item => {
            const lineSubtotal = item.pricePerUnit * item.quantity;
            itemsHtml += `
                <tr>
                    <td>${item.productName}</td>
                    <td>${item.quantity}</td>
                    <td>₹${item.originalPrice.toFixed(2)}</td>
                    <td>${item.discountPercentage.toFixed(2)}%</td>
                    <td>₹${lineSubtotal.toFixed(2)}</td>
                </tr>
            `;
        });

        let paymentHistoryHtml = '';
        if (invoice.payments && invoice.payments.length > 0) {
            paymentHistoryHtml += `
                <h4 style="margin-top: 30px;">Payment History</h4>
                <table class="product-table invoice-items-detail-table">
                    <thead>
                        <tr>
                            <th>Date</th>
                            <th>Payment Method</th>
                            <th>Amount</th>
                            <th>Action</th>
                        </tr>
                    </thead>
                    <tbody>
            `;
            invoice.payments.forEach(p => {
                if (p.amount < 0) return; // Don't show refunds in this list for now

                paymentHistoryHtml += `
                    <tr>
                        <td>${formatDate(p.paymentDate)}</td>
                        <td>${p.paymentMethod || 'N/A'}</td>
                        <td>₹${p.amount.toFixed(2)}</td>
                        <td class="table-actions">
                            <a href="#" class="action-icon email-receipt-btn" data-payment-id="${p.id}" title="Email Receipt">
                                <i class="fas fa-paper-plane"></i>
                            </a>
                            <a href="#" class="action-icon download-receipt-btn" data-payment-id="${p.id}" title="Download Receipt">
                                <i class="fas fa-download"></i>
                            </a>
                        </td>
                    </tr>
                `;
            });
            paymentHistoryHtml += '</tbody></table>';
        }

        const invoiceTitle = invoice.shopGstin ? (invoice.shopTitle || 'INVOICE') : 'BILL';
        
        const invoiceHtml = `
            <div class="invoice-header">
                <div>
                    ${invoice.shopLogoPath ? `<img src="http://localhost:8080${invoice.shopLogoPath}" alt="Shop Logo" class="shop-logo">` : ''}
                    <h4>${invoice.shopName}</h4>
                    <p>${invoice.shopAddress || ''}</p>
                    <p>${invoice.shopGstin ? `GSTIN: ${invoice.shopGstin}` : ''}</p>
                </div>
                <div class="invoice-header-right">
                    <h2>${invoiceTitle}</h2>
                    <p><strong>Invoice #:</strong> ${invoice.invoiceNumber}</p>
                    <p><strong>Status:</strong> ${invoice.status.replace('_', ' ')}</p>
                </div>
            </div>

            <div class="invoice-meta-details">
                <div>
                    <h4>Billed To:</h4>
                    <p>${invoice.customerName}</p>
                    <p>${invoice.customerPhone || ''}</p>
                    <p>${invoice.customerEmail || ''}</p>
                </div>
                <div>
                    <h4>Details:</h4>
                    <p><strong>Issue Date:</strong> ${formatDate(invoice.issueDate)}</p>
                </div>
            </div>

            <table class="product-table invoice-items-detail-table">
                <thead>
                    <tr>
                        <th>Item</th>
                        <th>Qty</th>
                        <th>Rate</th>
                        <th>Discount</th>
                        <th>Amount</th>
                    </tr>
                </thead>
                <tbody>
                    ${itemsHtml}
                </tbody>
            </table>

            <div class="invoice-totals">
                <div><span>Subtotal:</span> <span>₹${invoice.subtotal.toFixed(2)}</span></div>
                <div><span>Total GST:</span> <span>₹${invoice.totalGst.toFixed(2)}</span></div>
                <div class="grand-total"><span>Grand Total:</span> <span>₹${invoice.grandTotal.toFixed(2)}</span></div>
                
                ${(invoice.amountPaid !== null && invoice.amountPaid >= 0) ? `
                    <div style="margin-top: 10px;"><span>Amount Paid:</span> <span>₹${invoice.amountPaid.toFixed(2)}</span></div>
                    <div class="grand-total" style="color: var(--primary-color);"><span>Balance Due:</span> <span>₹${invoice.balanceDue.toFixed(2)}</span></div>
                ` : ''}
            </div>

            ${paymentHistoryHtml}
        `;

        invoiceContent.innerHTML = invoiceHtml;

    } catch (error) {
        invoiceContent.innerHTML = `<p>Error loading invoice details: ${error.message}</p>`;
    }

    invoiceContent.addEventListener('click', async (e) => {
        const downloadBtn = e.target.closest('.download-receipt-btn');
        const emailBtn = e.target.closest('.email-receipt-btn');

        if (downloadBtn) {
            e.preventDefault();
            const paymentId = downloadBtn.dataset.paymentId;
            
            try {
                const token = getToken();
                const response = await fetch(`${API_BASE_URL}/payments/${paymentId}/receipt`, {
                    method: 'GET',
                    headers: { 'Authorization': `Bearer ${token}` }
                });

                if (!response.ok) {
                    throw new Error('Could not download receipt.');
                }

                const blob = await response.blob();
                const url = window.URL.createObjectURL(blob);
                const a = document.createElement('a');
                a.style.display = 'none';
                a.href = url;
                
                const contentDisposition = response.headers.get('content-disposition');
                let fileName = `Receipt-PAY-${paymentId}.pdf`;
                if (contentDisposition) {
                    const fileNameMatch = contentDisposition.match(/filename="(.+)"/);
                    if (fileNameMatch && fileNameMatch.length === 2)
                        fileName = fileNameMatch[1];
                }
                
                a.download = fileName;
                document.body.appendChild(a);
                a.click();
                window.URL.revokeObjectURL(url);
                a.remove();

            } catch (error) {
                alert(error.message);
            }
        } else if (emailBtn) {
            e.preventDefault();
            const paymentId = emailBtn.dataset.paymentId;
            if (confirm('Are you sure you want to email this receipt to the customer?')) {
                try {
                    const response = await fetchWithAuth(`/payments/${paymentId}/email`, {
                        method: 'POST'
                    });
                    alert(response);
                } catch (error) {
                    alert(`Failed to send receipt: ${error.message}`);
                }
            }
        }
    });
});