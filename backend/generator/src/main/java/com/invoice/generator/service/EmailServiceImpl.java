package com.invoice.generator.service;

import com.invoice.generator.model.Invoice;
import com.invoice.generator.model.Payment;
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
import java.math.BigDecimal;
import java.util.Base64;

@Service
public class EmailServiceImpl {

    @Autowired
    private PdfGenerationService pdfGenerationService;

    @Autowired
    private RazorpayService razorpayService; // Injected RazorpayService

    @Value("${sendgrid.api.key}")
    private String sendGridApiKey;

    private final String FROM_EMAIL = "aayushgcode754@gmail.com";

    /**
     * Sends an invoice email. If payments are enabled and there is a balance due,
     * it automatically generates and includes a "Pay Now" button in the email.
     * @param userEmail The email of the logged-in user (for the Reply-To header).
     * @param invoice The invoice object.
     * @param recipientEmail The email of the customer.
     * @param customMessage An optional custom message from the user.
     * @throws IOException
     */
    public void sendInvoiceEmail(String userEmail, Invoice invoice, String recipientEmail, String customMessage) throws IOException {
        byte[] pdfBytes = pdfGenerationService.generateInvoicePdf(invoice);
        String subject = "Invoice " + invoice.getInvoiceNumber() + " from " + invoice.getShop().getShopName();

        String paymentLink = null;
        // Check if payments are enabled for the shop AND if there's a balance due
        if (Boolean.TRUE.equals(invoice.getShop().getPaymentsEnabled()) &&
            invoice.getBalanceDue() != null &&
            invoice.getBalanceDue().compareTo(BigDecimal.ZERO) > 0) {
            try {
                // If conditions are met, create a payment link
                paymentLink = razorpayService.createPaymentLink(invoice);
            } catch (Exception e) {
                System.err.println("Failed to create payment link for email for invoice " + invoice.getInvoiceNumber() + ": " + e.getMessage());
                // If it fails, we'll just send the email without the link.
            }
        }

        String body = (customMessage != null && !customMessage.isEmpty()) ? customMessage :
                "Hello, <br><br>Please find your invoice attached.<br><br>Thank you,<br>" + invoice.getShop().getShopName();

        // Only if a paymentLink was successfully created, add the button to the email body
        if (paymentLink != null) {
            String payButtonHtml = "<br><br><a href=\"" + paymentLink + "\" " +
                                 "style=\"background-color:#4F46E5; color:#ffffff; padding:12px 24px; text-decoration:none; border-radius:8px; font-family:sans-serif; font-size:16px; font-weight:bold;\">" +
                                 "Pay Now</a>";
            body += payButtonHtml;
        }

        sendEmail(recipientEmail, userEmail, subject, body, "Invoice-" + invoice.getInvoiceNumber() + ".pdf", pdfBytes);
    }

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
        
        mail.setReplyTo(new Email(replyTo));

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
            
            System.out.println("Email sent to " + to + "! Status Code: " + response.getStatusCode());
            
        } catch (IOException ex) {
            System.err.println("Error sending email: " + ex.getMessage());
            throw ex;
        }
    }
}