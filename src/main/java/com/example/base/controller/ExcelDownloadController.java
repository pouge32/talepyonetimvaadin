package com.example.base.controller;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.base.repository.RequestRepository;
import com.example.base.service.ExcelExportService;

@RestController
public class ExcelDownloadController {

    private final ExcelExportService excelExportService;
    private final RequestRepository requestRepository;

    public ExcelDownloadController(ExcelExportService excelExportService, RequestRepository requestRepository) {
        this.excelExportService = excelExportService;
        this.requestRepository = requestRepository;
    }

    @GetMapping("/api/download/excel")
    public ResponseEntity<InputStreamResource> downloadExcel() {
        ByteArrayInputStream bis = excelExportService.exportRequestsToExcel(requestRepository.findAll());

        String filename = "Talep_Raporu_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm")) + ".xlsx";

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=" + filename);

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(new InputStreamResource(bis));
    }
}