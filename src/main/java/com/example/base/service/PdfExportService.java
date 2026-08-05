package com.example.base.service;

import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.stereotype.Service;

import com.example.base.entity.RequestEntity;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

@Service
public class PdfExportService {

    public ByteArrayInputStream exportRequestsToPdf(List<RequestEntity> requests) {
        Document document = new Document(PageSize.A4.rotate());
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Color.BLACK);
            Paragraph title = new Paragraph("Talep Yonetim Sistemi - Sistem Raporu", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);

            PdfPTable table = new PdfPTable(6);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{1f, 3f, 3f, 2f, 2f, 1.5f});

            Font tableHeaderFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.WHITE);
            String[] headers = {"ID", "Baslik", "Musteri E-Posta", "Durum", "Tarih", "Puan"};
            
            for (String header : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(header, tableHeaderFont));
                cell.setBackgroundColor(Color.DARK_GRAY);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setPadding(8);
                table.addCell(cell);
            }

            Font tableBodyFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.BLACK);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

            for (RequestEntity req : requests) {
                table.addCell(createCell(String.valueOf(req.getRequestId()), tableBodyFont));
                
                String reqTitle = req.getTitle() != null ? req.getTitle() : "-";
                table.addCell(createCell(normalizeTR(reqTitle), tableBodyFont));
                
                String email = "Bilinmiyor";
                try {
                    if (req.getCustomer() != null && req.getCustomer().getEmail() != null) {
                        email = req.getCustomer().getEmail();
                    }
                } catch (Exception ignored) {}
                table.addCell(createCell(email, tableBodyFont));
                
                String status = req.getStatus() != null ? req.getStatus() : "-";
                table.addCell(createCell(normalizeTR(status), tableBodyFont));
                
                String dateStr = req.getCreatedAt() != null ? req.getCreatedAt().format(formatter) : "-";
                table.addCell(createCell(dateStr, tableBodyFont));
                
                String score = req.getSatisfactionScore() != null ? req.getSatisfactionScore() + "/5" : "-";
                table.addCell(createCell(score, tableBodyFont));
            }

            document.add(table);
            document.close();

        } catch (Exception e) {
            throw new RuntimeException("PDF raporu oluşturulurken sistem hatası: " + e.getMessage());
        }

        return new ByteArrayInputStream(out.toByteArray());
    }

    private PdfPCell createCell(String content, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(content, font));
        cell.setPadding(6);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        return cell;
    }

    private String normalizeTR(String text) {
        if (text == null) return "";
        return text.replace("ı", "i").replace("ğ", "g").replace("ü", "u").replace("ş", "s").replace("ö", "o").replace("ç", "c")
                   .replace("İ", "I").replace("Ğ", "G").replace("Ü", "U").replace("Ş", "S").replace("Ö", "O").replace("Ç", "C");
    }
}