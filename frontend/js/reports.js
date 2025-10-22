document.addEventListener('DOMContentLoaded', () => {
    const startDateInput = document.getElementById('startDate');
    const endDateInput = document.getElementById('endDate');
    const generateReportBtn = document.getElementById('generateReportBtn');
    const chartContainer = document.getElementById('paymentMethodChart').getContext('2d');
    let paymentChart;

    const today = new Date();
    const firstDayOfMonth = new Date(today.getFullYear(), today.getMonth(), 1);
    startDateInput.value = firstDayOfMonth.toISOString().split('T')[0];
    endDateInput.value = today.toISOString().split('T')[0];

    async function generateReport() {
        const startDateString = startDateInput.value;
        const endDateString = endDateInput.value;

        if (!startDateString || !endDateString) {
            alert("Please select both a start and end date.");
            return;
        }

        // --- THIS IS THE ROBUST FIX ---
        // Construct date strings explicitly to avoid timezone issues.
        // This creates a format like "2023-10-26T00:00:00" which is universally understood.
        const startDate = `${startDateString}T00:00:00`;
        const endDate = `${endDateString}T23:59:59`;
        
        console.log(`Requesting report data from ${startDate} to ${endDate}`); // For debugging

        try {
            const data = await fetchWithAuth(`/reports/payment-summary?startDate=${startDate}&endDate=${endDate}`);
            
            if (data.length === 0) {
                if (paymentChart) paymentChart.destroy();
                alert("No payment data found for the selected date range.");
                return;
            }

            const labels = data.map(d => d.paymentMethod || 'N/A');
            const amounts = data.map(d => d.totalAmount);
            const isDarkMode = document.documentElement.getAttribute('data-theme') === 'dark';

            if (paymentChart) {
                paymentChart.destroy();
            }

            paymentChart = new Chart(chartContainer, {
                type: 'pie',
                data: {
                    labels: labels,
                    datasets: [{
                        label: 'Revenue',
                        data: amounts,
                        backgroundColor: ['#10B981', '#3B82F6', '#F59E0B', '#EF4444', '#6366F1', '#8B5CF6'],
                        borderColor: isDarkMode ? '#111827' : '#FFFFFF',
                        borderWidth: 4
                    }]
                },
                options: {
                    responsive: true,
                    plugins: {
                        legend: {
                            position: 'top',
                            labels: { color: isDarkMode ? '#f9fafb' : '#111827' }
                        },
                        title: {
                            display: true,
                            text: 'Revenue by Payment Method',
                            color: isDarkMode ? '#f9fafb' : '#111827'
                        }
                    }
                }
            });
        } catch (error) {
            console.error("Report generation failed:", error); // For debugging
            alert(`Failed to generate report: ${error.message}`);
        }
    }

    generateReportBtn.addEventListener('click', generateReport);
    generateReport(); // Load initial report
});