package com.softuni.gms.app.repair.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.softuni.gms.app.repair.model.RepairOrder;
import com.softuni.gms.app.repair.model.UsedPart;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;

@Service
public class InvoicePdfService {

    public ByteArrayInputStream generateInvoice(RepairOrder repairOrder) {

        Document document = new Document(PageSize.A4, 40, 40, 60, 40);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, outputStream);
            document.open();

            try {
                ClassPathResource logoResource = new ClassPathResource("static/images/full-logo.png");
                Image logo = Image.getInstance(logoResource.getInputStream().readAllBytes());
                logo.scaleToFit(200, 100);
                logo.setAlignment(Element.ALIGN_CENTER);
                document.add(logo);
                document.add(Chunk.NEWLINE);
            } catch (Exception e) {
                System.err.println("Could not load logo: " + e.getMessage());
            }

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20);
            Paragraph title = new Paragraph("Repair Invoice", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            document.add(new Paragraph("Generated on: " +
                    DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm").format(repairOrder.getCreatedAt())));
            document.add(Chunk.NEWLINE);

            Font infoFont = FontFactory.getFont(FontFactory.HELVETICA, 12);
            document.add(new Paragraph("Customer: " + repairOrder.getUser().getFirstName() + " " + repairOrder.getUser().getLastName(), infoFont));
            document.add(new Paragraph("Phone: " + repairOrder.getUser().getPhoneNumber(), infoFont));
            document.add(new Paragraph("Car: " + repairOrder.getCar().getBrand() + " " + repairOrder.getCar().getModel(), infoFont));
            if (repairOrder.getMechanic() != null) {
                document.add(new Paragraph("Mechanic: " + repairOrder.getMechanic().getFirstName() + " " + repairOrder.getMechanic().getLastName(), infoFont));
            }
            document.add(Chunk.NEWLINE);

            BigDecimal partsTotal = BigDecimal.ZERO;
            if (repairOrder.getUsedParts() != null && !repairOrder.getUsedParts().isEmpty()) {
                PdfPTable table = new PdfPTable(4);
                table.setWidthPercentage(100);
                table.setWidths(new int[]{4, 2, 2, 2});

                addTableHeader(table, "Part", "Quantity", "Unit Price", "Total Price");

                for (UsedPart part : repairOrder.getUsedParts()) {
                    BigDecimal unitPrice = part.getTotalPrice().divide(BigDecimal.valueOf(part.getQuantity()), 2, RoundingMode.HALF_UP);
                    addTableRow(table, part.getPart().getName(),
                            String.valueOf(part.getQuantity()),
                            unitPrice.toPlainString() + " BGN",
                            part.getTotalPrice().toPlainString() + " BGN");
                    partsTotal = partsTotal.add(part.getTotalPrice());
                }

                document.add(table);
                document.add(Chunk.NEWLINE);
            }

            Font totalFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
            
            PdfPTable priceTable = new PdfPTable(2);
            priceTable.setWidthPercentage(60);
            priceTable.setWidths(new int[]{3, 2});
            priceTable.setHorizontalAlignment(Element.ALIGN_RIGHT);
            priceTable.setSpacingBefore(10);

            addPriceRow(priceTable, "Parts Total:", partsTotal.toPlainString() + " BGN", infoFont);

            if (repairOrder.getPrice() != null) {
                addPriceRow(priceTable, "Service Fee:", repairOrder.getPrice().toPlainString() + " BGN", infoFont);
            }

            BigDecimal grandTotal = BigDecimal.ZERO;
            if (repairOrder.getPrice() != null) {
                grandTotal = grandTotal.add(repairOrder.getPrice());
            }
            grandTotal = grandTotal.add(partsTotal);

            addPriceRow(priceTable, "Grand Total:", grandTotal.toPlainString() + " BGN", totalFont);
            priceTable.setSpacingAfter(10);

            document.add(priceTable);
            document.add(Chunk.NEWLINE);

            document.add(Chunk.NEWLINE);
            document.add(new Paragraph("Thank you for choosing our garage!", infoFont));

            document.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return new ByteArrayInputStream(outputStream.toByteArray());
    }

    private void addTableHeader(PdfPTable table, String... headers) {
        Font headFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, headFont));
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(cell);
        }
    }

    private void addTableRow(PdfPTable table, String... cols) {
        for (String c : cols) {
            PdfPCell cell = new PdfPCell(new Phrase(c));
            cell.setPadding(5);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(cell);
        }
    }

    private void addPriceRow(PdfPTable table, String label, String value, Font font) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, font));
        labelCell.setPadding(5);
        labelCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        labelCell.setBorder(0);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, font));
        valueCell.setPadding(5);
        valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        valueCell.setBorder(0);
        table.addCell(valueCell);
    }
}
