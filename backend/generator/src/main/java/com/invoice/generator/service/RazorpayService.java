package com.invoice.generator.service;

import com.invoice.generator.model.Invoice;
import com.invoice.generator.model.Shop;
import com.razorpay.RazorpayException;
import com.razorpay.Utils; // Import the Utils class for verification
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import okhttp3.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Base64;

@Service
public class RazorpayService {

    @Value("${razorpay.key.id}")
    private String keyId;

    @Value("${razorpay.key.secret}")
    private String keySecret;

    @Value("${razorpay.webhook.secret}")
    private String webhookSecret;

    private static final String RAZORPAY_API_BASE_URL = "https://api.razorpay.com/v1";
    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");

    public boolean verifyWebhookSignature(String payload, String signature) {
        try {
            return Utils.verifyWebhookSignature(payload, signature, this.webhookSecret);
        } catch (RazorpayException e) {
            System.err.println("Webhook signature verification failed: " + e.getMessage());
            return false;
        }
    }

    public String createFundAccount(Shop shop) throws RazorpayException, IOException {
        // ... (this method remains unchanged)
        JSONObject contactRequest = new JSONObject();
        contactRequest.put("name", shop.getBeneficiaryName());
        contactRequest.put("email", shop.getUsers().get(0).getUsername());
        contactRequest.put("type", "vendor");

        JSONObject contact = makeRazorpayRequest("/contacts", contactRequest);
        String contactId = contact.getString("id");

        JSONObject fundAccountRequest = new JSONObject();
        fundAccountRequest.put("contact_id", contactId);
        fundAccountRequest.put("account_type", "bank_account");

        JSONObject bankAccountDetails = new JSONObject();
        bankAccountDetails.put("name", shop.getBeneficiaryName());
        bankAccountDetails.put("ifsc", shop.getBankIfscCode());
        bankAccountDetails.put("account_number", shop.getBankAccountNumber());

        fundAccountRequest.put("bank_account", bankAccountDetails);

        JSONObject fundAccount = makeRazorpayRequest("/fund_accounts", fundAccountRequest);

        return fundAccount.getString("id");
    }

    // --- OVERLOADED METHOD TO KEEP EXISTING CALLS WORKING ---
    public String createPaymentLink(Invoice invoice) throws IOException, RazorpayException {
        return createPaymentLink(invoice, null); // Call the main method with null amount override
    }

    // --- THIS IS THE UPDATED METHOD ---
    public String createPaymentLink(Invoice invoice, BigDecimal amountToPayOverride) throws IOException, RazorpayException {
        JSONObject linkRequest = new JSONObject();

        // Use the override amount if provided, otherwise use the invoice's balance due
        BigDecimal amountToRequest = (amountToPayOverride != null && amountToPayOverride.compareTo(BigDecimal.ZERO) > 0)
                                     ? amountToPayOverride
                                     : invoice.getBalanceDue();

        if (amountToRequest == null || amountToRequest.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount to request for payment link must be greater than zero.");
        }

        long amountInPaise = amountToRequest.multiply(new BigDecimal("100")).longValue();

        linkRequest.put("amount", amountInPaise);
        linkRequest.put("currency", "INR");
        linkRequest.put("accept_partial", false); // Important: Never accept partial for this link
        linkRequest.put("description", "Payment for Invoice " + invoice.getInvoiceNumber());

        JSONObject customer = new JSONObject();
        customer.put("name", invoice.getCustomer().getName());
        customer.put("email", invoice.getCustomer().getEmail());
        linkRequest.put("customer", customer);

        JSONObject notify = new JSONObject();
        notify.put("sms", true);
        notify.put("email", true);
        linkRequest.put("notify", notify);

        linkRequest.put("reminder_enable", true);

        JSONObject notes = new JSONObject();
        notes.put("invoice_id", invoice.getId().toString());
        notes.put("shop_id", invoice.getShop().getId().toString());
        linkRequest.put("notes", notes);

        JSONObject response = makeRazorpayRequest("/payment_links", linkRequest);
        return response.getString("short_url");
    }

    // ... (rest of the methods: listFundAccounts, makeRazorpayRequest, makeRazorpayGetRequest remain unchanged)
    public String listFundAccounts() throws IOException, RazorpayException {
        JSONObject response = makeRazorpayGetRequest("/fund_accounts");
        return response.toString();
    }

    private JSONObject makeRazorpayRequest(String endpoint, JSONObject requestBody) throws IOException, RazorpayException {
        OkHttpClient client = new OkHttpClient();
        String credentials = keyId + ":" + keySecret;
        String basicAuth = "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes());
        RequestBody body = RequestBody.create(requestBody.toString(), JSON_MEDIA_TYPE);
        Request request = new Request.Builder()
                .url(RAZORPAY_API_BASE_URL + endpoint)
                .header("Authorization", basicAuth)
                .header("Content-Type", "application/json")
                .post(body)
                .build();
        try (Response response = client.newCall(request).execute()) {
            String responseBody = response.body().string();
            if (!response.isSuccessful()) {
                throw new RazorpayException("Razorpay API Error: " + responseBody);
            }
            return new JSONObject(responseBody);
        }
    }

    private JSONObject makeRazorpayGetRequest(String endpoint) throws IOException, RazorpayException {
        OkHttpClient client = new OkHttpClient();
        String credentials = keyId + ":" + keySecret;
        String basicAuth = "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes());
        Request request = new Request.Builder()
                .url(RAZORPAY_API_BASE_URL + endpoint)
                .header("Authorization", basicAuth)
                .get()
                .build();
        try (Response response = client.newCall(request).execute()) {
            String responseBody = response.body().string();
            if (!response.isSuccessful()) {
                throw new RazorpayException("Razorpay API Error: " + responseBody);
            }
            return new JSONObject(responseBody);
        }
    }
}
