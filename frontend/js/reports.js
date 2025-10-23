document.addEventListener('DOMContentLoaded', () => {
    const startDateInput = document.getElementById('startDate');
    const endDateInput = document.getElementById('endDate');
    const generateReportBtn = document.getElementById('generateReportBtn');
    
    // Chart canvases
    const paymentChartCtx = document.getElementById('paymentMethodChart').getContext('2d');
    const productChartCtx = document.getElementById('salesByProductChart').getContext('2d');
    
    // Table body
    const customerTableBody = document.getElementById('revenueByCustomerBody');
    
    // P&L section
    const pnlSection = document.getElementById('pnl-section');

    let paymentChart, productChart;

    // Set default date range to the current month
    const today = new Date();
    const firstDayOfMonth = new Date(today.getFullYear(), today.getMonth(), 1);
    startDateInput.value = firstDayOfMonth.toISOString().split('T')[0];
    endDateInput.value = today.toISOString().split('T')[0];

    async function generateAllReports() {
        const startDate = `${startDateInput.value}T00:00:00`;
        const endDate = `${endDateInput.value}T23:59:59`;

        if (!startDateInput.value || !endDateInput.value) {
            alert("Please select both a start and end date.");
            return;
        }

        try {
            // Fetch all data in parallel
            const [pnlData, paymentData, productData, customerData] = await Promise.all([
                fetchWithAuth(`/reports/profit-and-loss?startDate=${startDate}&endDate=${endDate}`),
                fetchWithAuth(`/reports/payment-summary?startDate=${startDate}&endDate=${endDate}`),
                fetchWithAuth(`/reports/sales-by-product?startDate=${startDate}&endDate=${endDate}`),
                fetchWithAuth(`/reports/revenue-by-customer?startDate=${startDate}&endDate=${endDate}`)
            ]);
            
            renderPnlStatement(pnlData);
            renderPaymentChart(paymentData);
            renderProductChart(productData);
            renderCustomerTable(customerData);

        } catch (error) {
            console.error("Report generation failed:", error);
            alert(`Failed to generate reports: ${error.message}`);
        }
    }
    
    function renderPnlStatement(data) {
        const formatCurrency = (amount) => `₹${amount.toFixed(2)}`;
        const profitClass = data.netProfit >= 0 ? 'status-paid' : 'status-overdue';

        pnlSection.innerHTML = `
            <div class="invoice-totals" style="max-width: 400px; margin-left: 0; text-align: left;">
                <div><span>Total Revenue:</span> <span>${formatCurrency(data.totalRevenue)}</span></div>
                <div><span>Cost of Goods Sold:</span> <span>-${formatCurrency(data.costOfGoodsSold)}</span></div>
                <div class="grand-total" style="grid-template-columns: 1fr 1fr;"><span>Gross Profit:</span> <span>${formatCurrency(data.grossProfit)}</span></div>
                <div style="margin-top: 10px;"><span>Operating Expenses:</span> <span>-${formatCurrency(data.totalExpenses)}</span></div>
                <div class="grand-total ${profitClass}" style="grid-template-columns: 1fr 1fr;"><span>Net Profit:</span> <span>${formatCurrency(data.netProfit)}</span></div>
            </div>
        `;
    }

    function renderPaymentChart(data) {
        if (paymentChart) paymentChart.destroy();
        const isDarkMode = document.documentElement.getAttribute('data-theme') === 'dark';
        
        paymentChart = new Chart(paymentChartCtx, {
            type: 'pie',
            data: {
                labels: data.map(d => d.paymentMethod || 'N/A'),
                datasets: [{
                    label: 'Revenue',
                    data: data.map(d => d.totalAmount),
                    backgroundColor: ['#10B981', '#3B82F6', '#F59E0B', '#EF4444', '#6366F1'],
                    borderColor: isDarkMode ? '#111827' : '#FFFFFF',
                    borderWidth: 4
                }]
            },
            options: {
                responsive: true,
                plugins: {
                    legend: { position: 'top', labels: { color: isDarkMode ? '#f9fafb' : '#111827' } }
                }
            }
        });
    }

    function renderProductChart(data) {
        if (productChart) productChart.destroy();
        const isDarkMode = document.documentElement.getAttribute('data-theme') === 'dark';

        productChart = new Chart(productChartCtx, {
            type: 'bar',
            data: {
                labels: data.map(d => d.productName),
                datasets: [{
                    label: 'Total Revenue',
                    data: data.map(d => d.totalRevenue),
                    backgroundColor: '#3B82F6',
                }]
            },
            options: {
                responsive: true,
                indexAxis: 'y', // Horizontal bar chart
                plugins: {
                    legend: { display: false }
                },
                scales: {
                    x: { ticks: { color: isDarkMode ? '#f9fafb' : '#111827' } },
                    y: { ticks: { color: isDarkMode ? '#f9fafb' : '#111827' } }
                }
            }
        });
    }

    function renderCustomerTable(data) {
        customerTableBody.innerHTML = '';
        if (data.length === 0) {
            customerTableBody.innerHTML = '<tr><td colspan="2" style="text-align:center;">No data for this period.</td></tr>';
            return;
        }
        data.forEach(item => {
            customerTableBody.innerHTML += `
                <tr>
                    <td>${item.customerName}</td>
                    <td>₹${item.totalRevenue.toFixed(2)}</td>
                </tr>
            `;
        });
    }

    generateReportBtn.addEventListener('click', generateAllReports);
    generateAllReports(); // Load initial report
});