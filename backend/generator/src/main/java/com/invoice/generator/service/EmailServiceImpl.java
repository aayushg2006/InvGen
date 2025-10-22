package com.invoice.generator.service;

import com.invoice.generator.model.Invoice;
import com.invoice.generator.model.Payment; // <-- IMPORT ADDED
import com.invoice.generator.model.Quote;
import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Attachments;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Base64;

@Service
public class EmailServiceImpl {

    @Autowired
    private PdfGenerationService pdfGenerationService;

    @Value("${sendgrid.api.key}")
    private String sendGridApiKey;

    // IMPORTANT: This should be the "From Email Address" you verified in SendGrid.
    private final String FROM_EMAIL = "aayushgcode754@gmail.com";

    /**
     * Sends an invoice email.
     * @param userEmail The email of the logged-in user (for the Reply-To header).
     * @param invoice The invoice object.
     * @param recipientEmail The email of the customer.
     * @param customMessage An optional custom message from the user.
     * @throws IOException
     */
    public void sendInvoiceEmail(String userEmail, Invoice invoice, String recipientEmail, String customMessage) throws IOException {
        byte[] pdfBytes = pdfGenerationService.generateInvoicePdf(invoice);
        String subject = "Invoice " + invoice.getInvoiceNumber() + " from " + invoice.getShop().getShopName();
        String body = (customMessage != null && !customMessage.isEmpty()) ? customMessage :
                "Hello, <br><br>Please find your invoice attached.<br><br>Thank you,<br>" + invoice.getShop().getShopName();

        sendEmail(recipientEmail, userEmail, subject, body, "Invoice-" + invoice.getInvoiceNumber() + ".pdf", pdfBytes);
    }

    // --- NEW METHOD FOR SENDING PAYMENT RECEIPTS ---
    public void sendPaymentReceiptEmail(String userEmail, Payment payment) throws IOException {
        Invoice invoice = payment.getInvoice();
        byte[] pdfBytes = pdfGenerationService.generatePaymentReceiptPdf(payment);
        String subject = "Payment Receipt for Invoice " + invoice.getInvoiceNumber();
        String body = "Hello, <br><br>Thank you for your payment. Your receipt is attached.<br><br>Regards,<br>" + invoice.getShop().getShopName();

        sendEmail(invoice.getCustomer().getEmail(), userEmail, subject, body, "Receipt-PAY-" + payment.getId() + ".pdf", pdfBytes);
    }


    private void sendEmail(String to, String replyTo, String subject, String body, String attachmentFileName, byte[] attachmentBytes) throws IOException {
        Email fromEmail = new Email(FROM_EMAIL);
        Email toEmail = new Email(to);
        Content content = new Content("text/html", body);
        Mail mail = new Mail(fromEmail, subject, toEmail, content);
        
        // This is the key part for our strategy: set the user's email as the Reply-To address.
        mail.setReplyTo(new Email(replyTo));

        // Add the PDF attachment
        Attachments attachments = new Attachments();
        String base64Content = Base64.getEncoder().encodeToString(attachmentBytes);
        attachments.setContent(base64Content);
        attachments.setType("application/pdf");
        attachments.setFilename(attachmentFileName);
        attachments.setDisposition("attachment");
        mail.addAttachments(attachments);

        SendGrid sg = new SendGrid(sendGridApiKey);
        Request request = new Request();
        try {
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());
            Response response = sg.api(request);
            
            // Log the result for debugging
            System.out.println("Email sent to " + to + "! Status Code: " + response.getStatusCode());
            
        } catch (IOException ex) {
            System.err.println("Error sending email: " + ex.getMessage());
            throw ex;
        }
    }
}