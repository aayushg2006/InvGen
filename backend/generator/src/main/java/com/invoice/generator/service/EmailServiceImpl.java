package com.invoice.generator.service;

import com.invoice.generator.model.Invoice;
import com.invoice.generator.model.Payment;
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
import java.math.BigDecimal;
import java.util.Base64;
import java.util.List;

@Service
public class EmailServiceImpl {

    @Autowired
    private PdfGenerationService pdfGenerationService;

    @Autowired
    private RazorpayService razorpayService;

    @Value("${sendgrid.api.key}")
    private String sendGridApiKey;

    private final String FROM_EMAIL = "aayushgcode754@gmail.com";

    public void sendQuoteEmail(String userEmail, Quote quote) throws IOException {
        byte[] pdfBytes = pdfGenerationService.generateQuotePdf(quote);
        String subject = "Quote " + quote.getQuoteNumber() + " from " + quote.getShop().getShopName();

        String body = "Hello, <br><br>Please find your quote attached.<br><br>Thank you,<br>" + quote.getShop().getShopName();

        sendEmail(quote.getCustomer().getEmail(), userEmail, subject, body, "Quote-" + quote.getQuoteNumber() + ".pdf", pdfBytes);
    }

    public void sendPaymentReminderEmail(String userEmail, Invoice invoice) throws IOException {
        byte[] pdfBytes = pdfGenerationService.generateInvoicePdf(invoice);
        String subject = "Reminder: Payment for Invoice " + invoice.getInvoiceNumber();

        String paymentLink = null;
        if (Boolean.TRUE.equals(invoice.getShop().getPaymentsEnabled()) &&
            invoice.getBalanceDue() != null &&
            invoice.getBalanceDue().compareTo(BigDecimal.ZERO) > 0) {
            try {
                paymentLink = razorpayService.createPaymentLink(invoice);
            } catch (Exception e) {
                System.err.println("Failed to create payment link for reminder email for invoice " + invoice.getInvoiceNumber() + ": " + e.getMessage());
            }
        }

        String body = "Hello, <br><br>This is a friendly reminder that your payment for the attached invoice is due.<br><br>Thank you,<br>" + invoice.getShop().getShopName();

        if (paymentLink != null) {
            String payButtonHtml = "<br><br><a href=\"" + paymentLink + "\" " +
                                 "style=\"background-color:#4F46E5; color:#ffffff; padding:12px 24px; text-decoration:none; border-radius:8px; font-family:sans-serif; font-size:16px; font-weight:bold;\">" +
                                 "Pay Now</a>";
            body += payButtonHtml;
        }

        sendEmail(invoice.getCustomer().getEmail(), userEmail, subject, body, "Invoice-" + invoice.getInvoiceNumber() + ".pdf", pdfBytes);
    }
    
    public void sendInvoiceEmail(String userEmail, Invoice invoice, String recipientEmail, String customMessage) throws IOException {
        byte[] pdfBytes = pdfGenerationService.generateInvoicePdf(invoice);
        String subject = "Invoice " + invoice.getInvoiceNumber() + " from " + invoice.getShop().getShopName();

        String paymentLink = null;
        if (Boolean.TRUE.equals(invoice.getShop().getPaymentsEnabled()) &&
            invoice.getBalanceDue() != null &&
            invoice.getBalanceDue().compareTo(BigDecimal.ZERO) > 0) {
            try {
                paymentLink = razorpayService.createPaymentLink(invoice);
            } catch (Exception e) {
                System.err.println("Failed to create payment link for email for invoice " + invoice.getInvoiceNumber() + ": " + e.getMessage());
            }
        }

        String body = (customMessage != null && !customMessage.isEmpty()) ? customMessage :
                "Hello, <br><br>Please find your invoice attached.<br><br>Thank you,<br>" + invoice.getShop().getShopName();

        if (paymentLink != null) {
            String payButtonHtml = "<br><br><a href=\"" + paymentLink + "\" " +
                                 "style=\"background-color:#4F46E5; color:#ffffff; padding:12px 24px; text-decoration:none; border-radius:8px; font-family:sans-serif; font-size:16px; font-weight:bold;\">" +
                                 "Pay Now</a>";
            body += payButtonHtml;
        }

        sendEmail(recipientEmail, userEmail, subject, body, "Invoice-" + invoice.getInvoiceNumber() + ".pdf", pdfBytes);
    }
    
    public void sendPostPaymentEmail(String userEmail, Payment payment) throws IOException {
        Invoice invoice = payment.getInvoice();
        byte[] invoicePdf = pdfGenerationService.generateInvoicePdf(invoice);
        byte[] receiptPdf = pdfGenerationService.generatePaymentReceiptPdf(payment);

        String subject = "Payment Confirmation for Invoice " + invoice.getInvoiceNumber();
        String body = "Hello, <br><br>Thank you for your payment. Your updated invoice and payment receipt are attached.<br><br>Regards,<br>" + invoice.getShop().getShopName();

        Attachments invoiceAttachment = new Attachments();
        invoiceAttachment.setContent(Base64.getEncoder().encodeToString(invoicePdf));
        invoiceAttachment.setType("application/pdf");
        invoiceAttachment.setFilename("Invoice-" + invoice.getInvoiceNumber() + ".pdf");
        invoiceAttachment.setDisposition("attachment");

        Attachments receiptAttachment = new Attachments();
        receiptAttachment.setContent(Base64.getEncoder().encodeToString(receiptPdf));
        receiptAttachment.setType("application/pdf");
        receiptAttachment.setFilename("Receipt-PAY-" + payment.getId() + ".pdf");
        receiptAttachment.setDisposition("attachment");

        sendEmailWithAttachments(invoice.getCustomer().getEmail(), userEmail, subject, body, List.of(invoiceAttachment, receiptAttachment));
    }


    public void sendPaymentReceiptEmail(String userEmail, Payment payment) throws IOException {
        Invoice invoice = payment.getInvoice();
        byte[] pdfBytes = pdfGenerationService.generatePaymentReceiptPdf(payment);
        String subject = "Payment Receipt for Invoice " + invoice.getInvoiceNumber();
        String body = "Hello, <br><br>Thank you for your payment. Your receipt is attached.<br><br>Regards,<br>" + invoice.getShop().getShopName();

        sendEmail(invoice.getCustomer().getEmail(), userEmail, subject, body, "Receipt-PAY-" + payment.getId() + ".pdf", pdfBytes);
    }

    private void sendEmail(String to, String replyTo, String subject, String body, String attachmentFileName, byte[] attachmentBytes) throws IOException {
        Attachments attachments = new Attachments();
        String base64Content = Base64.getEncoder().encodeToString(attachmentBytes);
        attachments.setContent(base64Content);
        attachments.setType("application/pdf");
        attachments.setFilename(attachmentFileName);
        attachments.setDisposition("attachment");
        
        sendEmailWithAttachments(to, replyTo, subject, body, List.of(attachments));
    }
    
    private void sendEmailWithAttachments(String to, String replyTo, String subject, String body, List<Attachments> attachments) throws IOException {
        Email fromEmail = new Email(FROM_EMAIL);
        Email toEmail = new Email(to);
        Content content = new Content("text/html", body);
        Mail mail = new Mail(fromEmail, subject, toEmail, content);
        
        mail.setReplyTo(new Email(replyTo));

        if (attachments != null) {
            for (Attachments attachment : attachments) {
                mail.addAttachments(attachment);
            }
        }

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