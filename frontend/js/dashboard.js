document.addEventListener('DOMContentLoaded', async () => {
    try {
        // --- Fetch all data first ---
        const stats = await fetchWithAuth('/dashboard/stats');
        const recentInvoices = await fetchWithAuth('/invoices');
        const lowStockProducts = await fetchWithAuth('/products/low-stock');

        // --- Populate Stat Cards ---
        document.getElementById('total-revenue').textContent = `₹${stats.totalRevenue.toFixed(2)}`;
        document.getElementById('total-gst-payable').textContent = `₹${stats.totalGstPayable.toFixed(2)}`;
        document.getElementById('invoices-due').textContent = stats.invoicesDue;
        document.getElementById('invoices-paid').textContent = stats.invoicesPaid;
        document.getElementById('invoices-partially-paid').textContent = stats.invoicesPartiallyPaid;

        // --- Populate Recent Invoices Table ---
        const tableBody = document.getElementById('recent-invoices-body');
        tableBody.innerHTML = '';
        const statusClassMap = {
            'PAID': 'status-paid',
            'PENDING': 'status-pending',
            'PARTIALLY_PAID': 'status-partially-paid',
            'CANCELLED': 'status-overdue'
        };

        recentInvoices.slice(0, 5).forEach(invoice => {
            const statusClass = statusClassMap[invoice.status] || 'status-pending';
            const row = `
                <tr>
                    <td><a href="invoice-detail.html?id=${invoice.id}">${invoice.invoiceNumber}</a></td>
                    <td>${invoice.customerName}</td>
                    <td>₹${invoice.totalAmount.toFixed(2)}</td>
                    <td><span class="status-badge ${statusClass}">${invoice.status.replace('_', ' ')}</span></td>
                </tr>
            `;
            tableBody.innerHTML += row;
        });

        // --- Populate Low Stock Widget ---
        const lowStockWidget = document.getElementById('low-stock-widget');
        const lowStockList = document.getElementById('low-stock-list');

        if (lowStockProducts.length > 0) {
            lowStockWidget.style.display = 'block';
            lowStockList.innerHTML = '';
            lowStockProducts.forEach(product => {
                const li = document.createElement('li');
                li.style.display = 'flex';
                li.style.justifyContent = 'space-between';
                li.style.padding = '8px 0';
                li.style.borderBottom = '1px solid var(--border-color)';
                li.innerHTML = `
                    <span>${product.name}</span>
                    <span style="font-weight: 600; color: #EF4444;">${product.quantityInStock} left</span>
                `;
                lowStockList.appendChild(li);
            });
        }

        // --- Populate Invoice Status Chart ---
        const ctx = document.getElementById('invoiceStatusChart').getContext('2d');
        new Chart(ctx, {
            type: 'doughnut',
            data: {
                labels: ['Paid', 'Partially Paid', 'Due (Pending)'],
                datasets: [{
                    label: 'Invoice Status',
                    data: [stats.invoicesPaid, stats.invoicesPartiallyPaid, stats.invoicesDue],
                    backgroundColor: [
                        '#10B981',
                        '#F59E0B',
                        '#EF4444'
                    ],
                    borderColor: document.documentElement.getAttribute('data-theme') === 'dark' ? '#111827' : '#FFFFFF',
                    borderWidth: 4
                }]
            },
            options: {
                responsive: true,
                plugins: {
                    legend: {
                        position: 'top',
                        labels: {
                            color: document.documentElement.getAttribute('data-theme') === 'dark' ? '#f9fafb' : '#111827'
                        }
                    }
                }
            }
        });

    } catch (error) {
        console.error("Failed to load dashboard data:", error);
        // You can add a user-friendly error message on the page here if you want
    }
});