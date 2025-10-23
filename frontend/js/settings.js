document.addEventListener('DOMContentLoaded', async () => {
    // --- Get all form elements ---
    const shopNameInput = document.getElementById('shopName');
    const gstinInput = document.getElementById('shopGstin');
    const addressTextarea = document.getElementById('shopAddress');
    const settingsForm = document.getElementById('settingsForm');
    const logoUploadInput = document.getElementById('logoUpload');
    const logoPreview = document.getElementById('logoPreview');
    const invoiceTitleInput = document.getElementById('invoiceTitle');
    const invoiceAccentColorInput = document.getElementById('invoiceAccentColor');
    const invoiceFooterTextarea = document.getElementById('invoiceFooter');

    // --- NEW PAYMENT GATEWAY ELEMENTS ---
    const paymentsEnabledToggle = document.getElementById('paymentsEnabled');
    const bankDetailsForm = document.getElementById('bank-details-form');
    const beneficiaryNameInput = document.getElementById('beneficiaryName');
    const bankAccountNumberInput = document.getElementById('bankAccountNumber');
    const bankIfscCodeInput = document.getElementById('bankIfscCode');

    let selectedLogoFile = null;

    // --- Function to load current shop settings ---
    async function loadSettings() {
        try {
            const shop = await fetchWithAuth('/settings');
            
            // Populate all existing fields
            shopNameInput.value = shop.shopName || '';
            gstinInput.value = shop.gstin || '';
            addressTextarea.value = shop.address || '';
            invoiceTitleInput.value = shop.invoiceTitle || 'INVOICE';
            invoiceAccentColorInput.value = shop.invoiceAccentColor || '#3B82F6';
            invoiceFooterTextarea.value = shop.invoiceFooter || '';

            if (shop.logoPath) {
                logoPreview.src = `http://localhost:8080${shop.logoPath}?t=${new Date().getTime()}`;
            }

            // --- NEW: Populate payment gateway fields ---
            paymentsEnabledToggle.checked = shop.paymentsEnabled;
            beneficiaryNameInput.value = shop.beneficiaryName || '';
            bankAccountNumberInput.value = shop.bankAccountNumber || '';
            bankIfscCodeInput.value = shop.bankIfscCode || '';
            
            // Show or hide the bank details form based on the toggle's state
            toggleBankDetailsForm();

        } catch (error) {
            // Error is handled by the fetchWithAuth function
        }
    }

    // --- NEW: Function to show/hide bank details form ---
    function toggleBankDetailsForm() {
        if (paymentsEnabledToggle.checked) {
            bankDetailsForm.style.display = 'block';
        } else {
            bankDetailsForm.style.display = 'none';
        }
    }

    // --- Event listener for the payments toggle ---
    paymentsEnabledToggle.addEventListener('change', toggleBankDetailsForm);


    // --- Event listener for new logo file selection ---
    logoUploadInput.addEventListener('change', (event) => {
        const file = event.target.files[0];
        if (file) {
            selectedLogoFile = file;
            const reader = new FileReader();
            reader.onload = (e) => {
                logoPreview.src = e.target.result;
            };
            reader.readAsDataURL(file);
        }
    });

    // --- Event listener for the main form submission ---
    settingsForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        
        // --- Step 1: Upload logo if a new one was selected ---
        if (selectedLogoFile) {
            const formData = new FormData();
            formData.append('logo', selectedLogoFile);
            try {
                const token = getToken();
                const response = await fetch('http://localhost:8080/api/files/uploadLogo', {
                    method: 'POST',
                    headers: { 'Authorization': `Bearer ${token}` },
                    body: formData,
                });
                if (!response.ok) throw new Error('Logo upload failed!');
                selectedLogoFile = null;
            } catch (error) {
                alert(error.message);
                return;
            }
        }

        // --- Step 2: Save the rest of the settings (including payment details) ---
        const settingsData = {
            shopName: shopNameInput.value,
            gstin: gstinInput.value,
            address: addressTextarea.value,
            invoiceTitle: invoiceTitleInput.value,
            invoiceAccentColor: invoiceAccentColorInput.value,
            invoiceFooter: invoiceFooterTextarea.value,
            
            // --- NEW: Add payment fields to the payload ---
            paymentsEnabled: paymentsEnabledToggle.checked,
            beneficiaryName: beneficiaryNameInput.value,
            bankAccountNumber: bankAccountNumberInput.value,
            bankIfscCode: bankIfscCodeInput.value,
        };

        try {
            await fetchWithAuth('/settings', {
                method: 'PUT',
                body: JSON.stringify(settingsData),
            });
            alert('Settings updated successfully!');
            loadSettings(); 
        } catch (error) {
            alert(`Failed to update settings: ${error.message}`);
        }
    });

    // --- Initial load of settings ---
    loadSettings();
});