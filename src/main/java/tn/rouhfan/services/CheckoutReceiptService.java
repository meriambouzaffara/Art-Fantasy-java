package tn.rouhfan.services;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import tn.rouhfan.entities.Article;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class CheckoutReceiptService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public ReceiptFiles createReceipt(String orderReference, String customerEmail, List<PanierItem> items, double total)
            throws Exception {
        Path receiptDir = Files.createDirectories(Path.of(System.getProperty("java.io.tmpdir"), "rouhfan-checkout"));
        File qrFile = receiptDir.resolve(orderReference + "-qr.png").toFile();
        File invoiceFile = receiptDir.resolve(orderReference + "-facture.pdf").toFile();

        String paidAt = LocalDateTime.now().format(DATE_FORMAT);
        String qrPayload = buildPrivateQrPayload(orderReference, customerEmail, items, total, paidAt);
        writeQrCode(qrPayload, qrFile);
        writeInvoice(orderReference, customerEmail, items, total, paidAt, qrFile, invoiceFile);

        return new ReceiptFiles(invoiceFile, qrFile, qrPayload);
    }

    private void writeQrCode(String payload, File target) throws WriterException, IOException {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix matrix = qrCodeWriter.encode(payload, BarcodeFormat.QR_CODE, 320, 320);
        MatrixToImageWriter.writeToPath(matrix, "PNG", target.toPath());
    }

    private void writeInvoice(String orderReference, String customerEmail, List<PanierItem> items,
                              double total, String paidAt, File qrFile, File invoiceFile) throws Exception {
        Document document = new Document();
        PdfWriter.getInstance(document, new FileOutputStream(invoiceFile));
        document.open();

        Font titleFont = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD);
        Font boldFont = new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD);
        Font normalFont = new Font(Font.FontFamily.HELVETICA, 10);

        Paragraph title = new Paragraph("Facture Rouh el Fann", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);
        document.add(new Paragraph("Reference: " + orderReference, normalFont));
        document.add(new Paragraph("Client: " + customerEmail, normalFont));
        document.add(new Paragraph("Paiement confirme le: " + paidAt, normalFont));
        document.add(new Paragraph(" "));

        PdfPTable table = new PdfPTable(new float[]{4, 1, 1.5f, 1.5f});
        table.setWidthPercentage(100);
        addHeaderCell(table, "Article", boldFont);
        addHeaderCell(table, "Qte", boldFont);
        addHeaderCell(table, "Prix", boldFont);
        addHeaderCell(table, "Sous-total", boldFont);

        for (PanierItem item : items) {
            Article article = item.getArticle();
            table.addCell(new Phrase(article != null ? safe(article.getTitre()) : "-", normalFont));
            table.addCell(new Phrase(String.valueOf(item.getQuantity()), normalFont));
            table.addCell(new Phrase(String.format("%.2f DT", article != null ? article.getPrix() : 0), normalFont));
            table.addCell(new Phrase(String.format("%.2f DT", item.getSubtotal()), normalFont));
        }

        PdfPCell totalLabel = new PdfPCell(new Phrase("Total", boldFont));
        totalLabel.setColspan(3);
        totalLabel.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(totalLabel);
        table.addCell(new Phrase(String.format("%.2f DT", total), boldFont));
        document.add(table);

        document.add(new Paragraph(" "));
        document.add(new Paragraph("Le QR code contient les informations privees du paiement.", normalFont));
        Image qrImage = Image.getInstance(qrFile.getAbsolutePath());
        qrImage.scaleAbsolute(120, 120);
        qrImage.setAlignment(Element.ALIGN_CENTER);
        document.add(qrImage);
        document.close();
    }

    private void addHeaderCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cell);
    }

    private String buildPrivateQrPayload(String orderReference, String customerEmail,
                                         List<PanierItem> items, double total, String paidAt) {
        StringBuilder articles = new StringBuilder();
        for (PanierItem item : items) {
            if (articles.length() > 0) {
                articles.append("; ");
            }
            Article article = item.getArticle();
            articles.append(article != null ? safe(article.getTitre()) : "Article")
                    .append(" x")
                    .append(item.getQuantity());
        }

        return "Rouh el Fann payment\n"
                + "Reference: " + orderReference + "\n"
                + "Email: " + customerEmail + "\n"
                + "Total: " + String.format("%.2f DT", total) + "\n"
                + "Paid at: " + paidAt + "\n"
                + "Articles: " + articles;
    }

    private String safe(String value) {
        return value != null ? value : "";
    }

    public static class ReceiptFiles {
        private final File invoicePdf;
        private final File qrCode;
        private final String qrPayload;

        public ReceiptFiles(File invoicePdf, File qrCode, String qrPayload) {
            this.invoicePdf = invoicePdf;
            this.qrCode = qrCode;
            this.qrPayload = qrPayload;
        }

        public File getInvoicePdf() {
            return invoicePdf;
        }

        public File getQrCode() {
            return qrCode;
        }

        public String getQrPayload() {
            return qrPayload;
        }
    }
}
