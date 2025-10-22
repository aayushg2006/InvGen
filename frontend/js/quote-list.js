document.addEventListener('DOMContentLoaded', async () => {
    const tableBody = document.getElementById('quotes-table-body');
    let allQuotes = [];

    async function loadQuotes() {
        try {
            allQuotes = await fetchWithAuth('/quotes');
            renderQuotes(allQuotes);
        } catch (error) {
            // Error is handled by fetchWithAuth
        }
    }

    function renderQuotes(quotesToRender) {
        tableBody.innerHTML = '';
        if (quotesToRender.length === 0) {
            tableBody.innerHTML = `<tr><td colspan="6" style="text-align:center;">No quotes found.</td></tr>`;
            return;
        }
        quotesToRender.forEach(quote => {
            const row = `
                <tr>
                    <td>${quote.quoteNumber}</td>
                    <td>${quote.customerName}</td>
                    <td>${new Date(quote.issueDate).toLocaleDateString()}</td>
                    <td>₹${quote.totalAmount.toFixed(2)}</td>
                    <td><span class="status-badge">${quote.status}</span></td>
                    <td class="table-actions">
                        ${quote.status !== 'CONVERTED' ? `<button class="btn btn-primary convert-btn" data-id="${quote.id}">Convert to Invoice</button>` : 'Converted'}
                    </td>
                </tr>
            `;
            tableBody.innerHTML += row;
        });
    }

    tableBody.addEventListener('click', async (e) => {
        const convertBtn = e.target.closest('.convert-btn');
        if (convertBtn) {
            e.preventDefault();
            const quoteId = convertBtn.dataset.id;
            if (confirm('Are you sure you want to convert this quote to an invoice?')) {
                try {
                    const response = await fetchWithAuth(`/quotes/${quoteId}/convert`, {
                        method: 'POST'
                    });
                    alert('Quote successfully converted to invoice!');
                    // Redirect to the new invoice's detail page
                    window.location.href = `invoice-detail.html?id=${response.newInvoiceId}`;
                } catch (error) {
                    alert(`Failed to convert quote: ${error.message}`);
                }
            }
        }
    });

    // Initial load
    loadQuotes();
});