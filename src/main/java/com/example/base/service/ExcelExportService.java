package com.example.base.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import com.example.base.entity.RequestEntity;

@Service
public class ExcelExportService {

    public ByteArrayInputStream exportRequestsToExcel(List<RequestEntity> requests) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            
            Sheet sheet = workbook.createSheet("Talep Raporu");

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            String[] columns = {"Talep ID", "Başlık", "Müşteri E-Posta", "Sistem Durumu", "Oluşturulma Tarihi", "Memnuniyet Puanı"};
            Row headerRow = sheet.createRow(0);

            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

            for (RequestEntity req : requests) {
                Row row = sheet.createRow(rowIdx++);

                row.createCell(0).setCellValue(req.getRequestId() != null ? req.getRequestId() : 0);
                row.createCell(1).setCellValue(req.getTitle() != null ? req.getTitle() : "Bilinmiyor");
                
                String email = "Atanmadı";
                try {
                    if (req.getCustomer() != null && req.getCustomer().getEmail() != null) {
                        email = req.getCustomer().getEmail();
                    }
                } catch (Exception ignored) {
                    email = "Bilinmiyor";
                }
                row.createCell(2).setCellValue(email);
                
                row.createCell(3).setCellValue(req.getStatus() != null ? req.getStatus() : "-");
                
                String dateStr = req.getCreatedAt() != null ? req.getCreatedAt().format(formatter) : "-";
                row.createCell(4).setCellValue(dateStr);
                
                String score = req.getSatisfactionScore() != null ? req.getSatisfactionScore() + "/5" : "Puanlanmamış";
                row.createCell(5).setCellValue(score);
            }

            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());

        } catch (Exception e) {
            throw new RuntimeException("Excel raporu oluşturulurken sistem hatası: " + e.getMessage());
        }
    }
}