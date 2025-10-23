package com.invoice.generator.service;

import com.invoice.generator.model.Invoice;
import com.invoice.generator.model.InvoiceItem;
import com.invoice.generator.model.Payment;
import com.invoice.generator.model.Quote;
import com.invoice.generator.model.QuoteItem;
import com.invoice.generator.model.Shop;
import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.Color;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

@Service
public class PdfGenerationService {

    @Value("${file.upload-dir}")
    private String uploadDir;

    private Color parseHexColor(String hexColor) {
        if (hexColor == null || hexColor.length() != 7) {
            return new DeviceRgb(59, 130, 246); // Default blue if invalid
        }
        try {
            int r = Integer.valueOf(hexColor.substring(1, 3), 16);
            int g = Integer.valueOf(hexColor.substring(3, 5), 16);
            int b = Integer.valueOf(hexColor.substring(5, 7), 16);
            return new DeviceRgb(r, g, b);
        } catch (NumberFormatException e) {
            return new DeviceRgb(59, 130, 246); // Default on error
        }
    }

    public byte[] generateQuotePdf(Quote quote) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf, PageSize.A4);
        document.setMargins(40, 40, 40, 40);

        Shop shop = quote.getShop();
        Color accentColor = parseHexColor(shop.getInvoiceAccentColor());

        Table header = new Table(UnitValue.createPercentArray(new float[]{1, 1}));
        header.setWidth(UnitValue.createPercentValue(100));
        header.setMarginBottom(20);

        Cell shopInfoCell = new Cell().setPadding(0).setBorder(Border.NO_BORDER);
        if (shop.getLogoPath() != null && !shop.getLogoPath().isEmpty()) {
            try {
                String logoFileName = shop.getLogoPath().substring(shop.getLogoPath().lastIndexOf("/") + 1);
                String fullLogoFilePath = uploadDir + "logos" + File.separator + logoFileName;
                File logoFile = new File(fullLogoFilePath);

                if (logoFile.exists() && logoFile.canRead()) {
                    ImageData data = ImageDataFactory.create(fullLogoFilePath);
                    Image img = new Image(data);
                    img.setWidth(UnitValue.createPercentValue(30));
                    img.setMarginBottom(5);
                    shopInfoCell.add(img);
                } else {
                    shopInfoCell.add(createFallbackLogoText());
                }
            } catch (Exception e) {
                shopInfoCell.add(createFallbackLogoText());
            }
        } else {
            shopInfoCell.add(createFallbackLogoText());
        }
        shopInfoCell.add(new Paragraph(shop.getShopName()).setBold().setFontSize(14));
        shopInfoCell.add(new Paragraph(shop.getAddress()).setFontSize(10));
        shopInfoCell.add(new Paragraph("GSTIN: " + shop.getGstin()).setFontSize(10));
        header.addCell(shopInfoCell);
        
        Cell quoteDetailsCell = new Cell().setPadding(0).setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.RIGHT);
        quoteDetailsCell.add(new Paragraph("QUOTE").setBold().setFontSize(24).setMarginBottom(5).setFontColor(accentColor));
        quoteDetailsCell.add(new Paragraph("Quote #: " + quote.getQuoteNumber()).setFontSize(10));
        quoteDetailsCell.add(new Paragraph("Date: " + quote.getIssueDate().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))).setFontSize(10).setMarginTop(10));
        header.addCell(quoteDetailsCell);
        document.add(header);
        
        document.add(new Paragraph("Prepared For:").setBold().setFontSize(10).setMarginBottom(2));
        document.add(new Paragraph(quote.getCustomer().getName()).setFontSize(10));
        if (quote.getCustomer().getPhoneNumber() != null && !quote.getCustomer().getPhoneNumber().isEmpty()) {
            document.add(new Paragraph("Phone: " + quote.getCustomer().getPhoneNumber()).setFontSize(10));
        }
        if (quote.getCustomer().getEmail() != null && !quote.getCustomer().getEmail().isEmpty()) {
            document.add(new Paragraph("Email: " + quote.getCustomer().getEmail()).setFontSize(10));
        }

        Table itemsTable = new Table(UnitValue.createPercentArray(new float[]{4, 1, 2, 1.5f, 2}));
        itemsTable.setWidth(UnitValue.createPercentValue(100)).setMarginTop(20);
        itemsTable.addHeaderCell(createStyledCell("Item", TextAlignment.LEFT, true).setBackgroundColor(accentColor).setFontColor(ColorConstants.WHITE));
        itemsTable.addHeaderCell(createStyledCell("Qty", TextAlignment.CENTER, true).setBackgroundColor(accentColor).setFontColor(ColorConstants.WHITE));
        itemsTable.addHeaderCell(createStyledCell("Rate", TextAlignment.RIGHT, true).setBackgroundColor(accentColor).setFontColor(ColorConstants.WHITE));
        itemsTable.addHeaderCell(createStyledCell("Discount", TextAlignment.RIGHT, true).setBackgroundColor(accentColor).setFontColor(ColorConstants.WHITE));
        itemsTable.addHeaderCell(createStyledCell("Amount", TextAlignment.RIGHT, true).setBackgroundColor(accentColor).setFontColor(ColorConstants.WHITE));

        for (QuoteItem item : quote.getQuoteItems()) {
            BigDecimal lineSubtotal = item.getPricePerUnit().multiply(BigDecimal.valueOf(item.getQuantity()));
            itemsTable.addCell(createStyledCell(item.getProduct().getName(), TextAlignment.LEFT, false));
            itemsTable.addCell(createStyledCell(String.valueOf(item.getQuantity()), TextAlignment.CENTER, false));
            itemsTable.addCell(createStyledCell("₹" + String.format("%.2f", item.getProduct().getSellingPrice()), TextAlignment.RIGHT, false));
            itemsTable.addCell(createStyledCell(String.format("%.2f", item.getDiscountPercentage()) + "%", TextAlignment.RIGHT, false));
            itemsTable.addCell(createStyledCell("₹" + String.format("%.2f", lineSubtotal), TextAlignment.RIGHT, false));
        }

        Border topBorder = new SolidBorder(ColorConstants.GRAY, 1f);
        itemsTable.addCell(new Cell(1, 3).setBorder(Border.NO_BORDER).setBorderTop(topBorder));
        itemsTable.addCell(createTotalCell("Subtotal:", false).setBorderTop(topBorder));
        itemsTable.addCell(createTotalCell("₹" + String.format("%.2f", quote.getTotalAmount()), false).setBorderTop(topBorder));
        
        itemsTable.addCell(new Cell(1, 3).setBorder(Border.NO_BORDER));
        itemsTable.addCell(createTotalCell("GST:", false));
        itemsTable.addCell(createTotalCell("₹" + String.format("%.2f", quote.getTotalGst()), false));
        
        itemsTable.addCell(new Cell(1, 3).setBorder(Border.NO_BORDER));
        itemsTable.addCell(createTotalCell("Grand Total:", true));
        itemsTable.addCell(createTotalCell("₹" + String.format("%.2f", quote.getTotalAmount().add(quote.getTotalGst())), true));
        
        document.add(itemsTable);
        
        if (shop.getInvoiceFooter() != null && !shop.getInvoiceFooter().isEmpty()) {
            document.add(new Paragraph("Notes / Terms")
                .setBold().setFontSize(10).setMarginTop(30).setMarginBottom(5));
            document.add(new Paragraph(shop.getInvoiceFooter())
                .setFontSize(9).setFontColor(ColorConstants.GRAY));
        }

        document.close();
        return baos.toByteArray();
    }

    public byte[] generateInvoicePdf(Invoice invoice) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf, PageSize.A4);
        document.setMargins(40, 40, 40, 40);

        Shop shop = invoice.getShop();
        Color accentColor = parseHexColor(shop.getInvoiceAccentColor());

        Table header = new Table(UnitValue.createPercentArray(new float[]{1, 1}));
        header.setWidth(UnitValue.createPercentValue(100));
        header.setMarginBottom(20);

        Cell shopInfoCell = new Cell().setPadding(0).setBorder(Border.NO_BORDER);
        if (shop.getLogoPath() != null && !shop.getLogoPath().isEmpty()) {
            try {
                String logoFileName = shop.getLogoPath().substring(shop.getLogoPath().lastIndexOf("/") + 1);
                String fullLogoFilePath = uploadDir + "logos" + File.separator + logoFileName;
                File logoFile = new File(fullLogoFilePath);

                if (logoFile.exists() && logoFile.canRead()) {
                    ImageData data = ImageDataFactory.create(fullLogoFilePath);
                    Image img = new Image(data);
                    img.setWidth(UnitValue.createPercentValue(30));
                    img.setMarginBottom(5);
                    shopInfoCell.add(img);
                } else {
                    shopInfoCell.add(createFallbackLogoText());
                }
            } catch (Exception e) {
                shopInfoCell.add(createFallbackLogoText());
            }
        } else {
            shopInfoCell.add(createFallbackLogoText());
        }
        shopInfoCell.add(new Paragraph(shop.getShopName()).setBold().setFontSize(14));
        shopInfoCell.add(new Paragraph(shop.getAddress()).setFontSize(10));
        shopInfoCell.add(new Paragraph("GSTIN: " + shop.getGstin()).setFontSize(10));
        header.addCell(shopInfoCell);
        
        String invoiceTitle = (shop.getInvoiceTitle() != null && !shop.getInvoiceTitle().isEmpty()) ? shop.getInvoiceTitle().toUpperCase() : "INVOICE";
        Cell invoiceDetailsCell = new Cell().setPadding(0).setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.RIGHT);
        invoiceDetailsCell.add(new Paragraph(invoiceTitle).setBold().setFontSize(24).setMarginBottom(5).setFontColor(accentColor));
        invoiceDetailsCell.add(new Paragraph("Invoice #: " + invoice.getInvoiceNumber()).setFontSize(10));
        invoiceDetailsCell.add(new Paragraph("Status: " + invoice.getStatus().name().replace('_', ' ')).setFontSize(10));
        invoiceDetailsCell.add(new Paragraph("Date: " + invoice.getIssueDate().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))).setFontSize(10).setMarginTop(10));
        header.addCell(invoiceDetailsCell);
        document.add(header);
        
        document.add(new Paragraph("Billed To:").setBold().setFontSize(10).setMarginBottom(2));
        document.add(new Paragraph(invoice.getCustomer().getName()).setFontSize(10));
        if (invoice.getCustomer().getPhoneNumber() != null && !invoice.getCustomer().getPhoneNumber().isEmpty()) {
            document.add(new Paragraph("Phone: " + invoice.getCustomer().getPhoneNumber()).setFontSize(10));
        }
        if (invoice.getCustomer().getEmail() != null && !invoice.getCustomer().getEmail().isEmpty()) {
            document.add(new Paragraph("Email: " + invoice.getCustomer().getEmail()).setFontSize(10));
        }

        if (invoice.getPayments() != null && !invoice.getPayments().isEmpty()) {
            Payment firstPayment = invoice.getPayments().get(0);
            if (firstPayment.getPaymentMethod() != null && !firstPayment.getPaymentMethod().isEmpty()) {
                document.add(new Paragraph("Payment Method: " + firstPayment.getPaymentMethod()).setBold().setFontSize(10).setMarginTop(10));
            }
        }

        Table itemsTable = new Table(UnitValue.createPercentArray(new float[]{4, 1, 2, 1.5f, 2}));
        itemsTable.setWidth(UnitValue.createPercentValue(100)).setMarginTop(20);
        itemsTable.addHeaderCell(createStyledCell("Item", TextAlignment.LEFT, true).setBackgroundColor(accentColor).setFontColor(ColorConstants.WHITE));
        itemsTable.addHeaderCell(createStyledCell("Qty", TextAlignment.CENTER, true).setBackgroundColor(accentColor).setFontColor(ColorConstants.WHITE));
        itemsTable.addHeaderCell(createStyledCell("Rate", TextAlignment.RIGHT, true).setBackgroundColor(accentColor).setFontColor(ColorConstants.WHITE));
        itemsTable.addHeaderCell(createStyledCell("Discount", TextAlignment.RIGHT, true).setBackgroundColor(accentColor).setFontColor(ColorConstants.WHITE));
        itemsTable.addHeaderCell(createStyledCell("Amount", TextAlignment.RIGHT, true).setBackgroundColor(accentColor).setFontColor(ColorConstants.WHITE));

        for (InvoiceItem item : invoice.getInvoiceItems()) {
            BigDecimal lineSubtotal = item.getPricePerUnit().multiply(BigDecimal.valueOf(item.getQuantity()));
            itemsTable.addCell(createStyledCell(item.getProduct().getName(), TextAlignment.LEFT, false));
            itemsTable.addCell(createStyledCell(String.valueOf(item.getQuantity()), TextAlignment.CENTER, false));
            itemsTable.addCell(createStyledCell("₹" + String.format("%.2f", item.getProduct().getSellingPrice()), TextAlignment.RIGHT, false));
            itemsTable.addCell(createStyledCell(String.format("%.2f", item.getDiscountPercentage()) + "%", TextAlignment.RIGHT, false));
            itemsTable.addCell(createStyledCell("₹" + String.format("%.2f", lineSubtotal), TextAlignment.RIGHT, false));
        }

        Border topBorder = new SolidBorder(ColorConstants.GRAY, 1f);
        itemsTable.addCell(new Cell(1, 3).setBorder(Border.NO_BORDER).setBorderTop(topBorder));
        itemsTable.addCell(createTotalCell("Subtotal:", false).setBorderTop(topBorder));
        itemsTable.addCell(createTotalCell("₹" + String.format("%.2f", invoice.getTotalAmount()), false).setBorderTop(topBorder));
        
        itemsTable.addCell(new Cell(1, 3).setBorder(Border.NO_BORDER));
        itemsTable.addCell(createTotalCell("GST:", false));
        itemsTable.addCell(createTotalCell("₹" + String.format("%.2f", invoice.getTotalGst()), false));
        
        itemsTable.addCell(new Cell(1, 3).setBorder(Border.NO_BORDER));
        itemsTable.addCell(createTotalCell("Grand Total:", true));
        itemsTable.addCell(createTotalCell("₹" + String.format("%.2f", invoice.getTotalAmount().add(invoice.getTotalGst())), true));
        
        if (invoice.getPayments() != null && !invoice.getPayments().isEmpty()) {
            for (Payment p : invoice.getPayments()) {
                if (p.getAmount().compareTo(BigDecimal.ZERO) > 0) {
                    String paymentLabel = "Paid by " + p.getPaymentMethod() + ":";
                    if ("CREDIT APPLIED".equalsIgnoreCase(p.getPaymentMethod())) {
                        paymentLabel = "Credit Applied:";
                    }
                    itemsTable.addCell(new Cell(1, 3).setBorder(Border.NO_BORDER));
                    itemsTable.addCell(createTotalCell(paymentLabel, false));
                    itemsTable.addCell(createTotalCell("-₹" + String.format("%.2f", p.getAmount()), false));
                }
            }
        }
        
        if (invoice.getBalanceDue() != null) {
            itemsTable.addCell(new Cell(1, 3).setBorder(Border.NO_BORDER));
            itemsTable.addCell(createTotalCell("Balance Due:", true).setFontColor(accentColor));
            itemsTable.addCell(createTotalCell("₹" + String.format("%.2f", invoice.getBalanceDue()), true).setFontColor(accentColor));
        }
        
        document.add(itemsTable);
        
        if (shop.getInvoiceFooter() != null && !shop.getInvoiceFooter().isEmpty()) {
            document.add(new Paragraph("Notes / Terms")
                .setBold().setFontSize(10).setMarginTop(30).setMarginBottom(5));
            document.add(new Paragraph(shop.getInvoiceFooter())
                .setFontSize(9).setFontColor(ColorConstants.GRAY));
        }

        document.close();
        return baos.toByteArray();
    }
    
    public byte[] generatePaymentReceiptPdf(Payment payment) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf, PageSize.A4);
        document.setMargins(40, 40, 40, 40);

        Invoice invoice = payment.getInvoice();
        Shop shop = invoice.getShop();
        Color accentColor = parseHexColor(shop.getInvoiceAccentColor());

        Table header = new Table(UnitValue.createPercentArray(new float[]{1, 1}));
        header.setWidth(UnitValue.createPercentValue(100)).setMarginBottom(30);

        Cell shopInfoCell = new Cell().setPadding(0).setBorder(Border.NO_BORDER);
        if (shop.getLogoPath() != null && !shop.getLogoPath().isEmpty()) {
             try {
                String logoFileName = shop.getLogoPath().substring(shop.getLogoPath().lastIndexOf("/") + 1);
                String fullLogoFilePath = uploadDir + "logos" + File.separator + logoFileName;
                File logoFile = new File(fullLogoFilePath);

                if (logoFile.exists() && logoFile.canRead()) {
                    ImageData data = ImageDataFactory.create(fullLogoFilePath);
                    Image img = new Image(data);
                    img.setWidth(UnitValue.createPercentValue(30));
                    shopInfoCell.add(img);
                }
            } catch (Exception e) {
                // Ignore if logo fails
            }
        }
        shopInfoCell.add(new Paragraph(shop.getShopName()).setBold().setFontSize(14));
        shopInfoCell.add(new Paragraph(shop.getAddress()).setFontSize(10));
        header.addCell(shopInfoCell);

        Cell titleCell = new Cell().setPadding(0).setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.RIGHT);
        titleCell.add(new Paragraph("PAYMENT RECEIPT").setBold().setFontSize(24).setFontColor(accentColor));
        titleCell.add(new Paragraph("Receipt ID: PAY-" + payment.getId()).setFontSize(10));
        header.addCell(titleCell);
        document.add(header);

        Table details = new Table(UnitValue.createPercentArray(new float[]{1, 2}));
        details.setWidth(UnitValue.createPercentValue(60)).setMarginBottom(40);

        details.addCell(createStyledCell("Payment Date:", TextAlignment.LEFT, true).setBorder(Border.NO_BORDER));
        details.addCell(createStyledCell(payment.getPaymentDate().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")), TextAlignment.LEFT, false).setBorder(Border.NO_BORDER));

        details.addCell(createStyledCell("Payment Method:", TextAlignment.LEFT, true).setBorder(Border.NO_BORDER));
        details.addCell(createStyledCell(payment.getPaymentMethod(), TextAlignment.LEFT, false).setBorder(Border.NO_BORDER));

        details.addCell(createStyledCell("Original Invoice:", TextAlignment.LEFT, true).setBorder(Border.NO_BORDER));
        details.addCell(createStyledCell(invoice.getInvoiceNumber(), TextAlignment.LEFT, false).setBorder(Border.NO_BORDER));

        details.addCell(createStyledCell("Paid By:", TextAlignment.LEFT, true).setBorder(Border.NO_BORDER));
        details.addCell(createStyledCell(invoice.getCustomer().getName(), TextAlignment.LEFT, false).setBorder(Border.NO_BORDER));
        document.add(details);

        document.add(new Paragraph("Amount Paid").setTextAlignment(TextAlignment.CENTER).setFontSize(14).setFontColor(ColorConstants.GRAY));
        document.add(new Paragraph("₹" + String.format("%.2f", payment.getAmount()))
            .setTextAlignment(TextAlignment.CENTER).setFontSize(48).setBold().setMarginBottom(40));
        
        if (shop.getInvoiceFooter() != null && !shop.getInvoiceFooter().isEmpty()) {
            document.add(new Paragraph("Notes")
                .setBold().setFontSize(10).setMarginBottom(5));
            document.add(new Paragraph(shop.getInvoiceFooter())
                .setFontSize(9).setFontColor(ColorConstants.GRAY));
        }

        document.close();
        return baos.toByteArray();
    }

    private Paragraph createFallbackLogoText() {
        return new Paragraph("Shop Logo").setBold().setFontSize(10).setFontColor(ColorConstants.GRAY);
    }

    private Cell createStyledCell(String content, TextAlignment alignment, boolean isBold) {
        Paragraph p = new Paragraph(content).setFontSize(10).setTextAlignment(alignment);
        if (isBold) p.setBold();
        return new Cell().add(p).setPadding(5).setBorder(new SolidBorder(ColorConstants.GRAY, 0.5f));
    }

    private Cell createTotalCell(String content, boolean isBold) {
        Paragraph p = new Paragraph(content).setTextAlignment(TextAlignment.RIGHT);
        if (isBold) {
            p.setBold().setFontSize(12);
        } else {
            p.setFontSize(10);
        }
        return new Cell().add(p).setBorder(Border.NO_BORDER).setPadding(2);
    }
}