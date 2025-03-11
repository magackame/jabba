package org.einsof.jabba.services;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.List;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.einsof.jabba.entities.Domain;
import org.springframework.stereotype.Service;

@Service
public class ExcelService {
  public byte[] export(String sheetName, List<Domain> records) throws IOException {
    final var workbook = new XSSFWorkbook();
    final var sheet = workbook.createSheet(sheetName);

    final var rowOffset = 1;
    final var cellOffset = 1;

    final var header = sheet.createRow(0 + rowOffset);

    final var domainHeading = header.createCell(0 + cellOffset);
    domainHeading.setCellValue("domain");
    domainHeading.getCellStyle().setAlignment(HorizontalAlignment.CENTER);
    domainHeading.getCellStyle().setBorderLeft(BorderStyle.THIN);
    domainHeading.getCellStyle().setBorderTop(BorderStyle.THIN);
    domainHeading.getCellStyle().setBorderBottom(BorderStyle.THIN);
    domainHeading.getCellStyle().setBorderRight(BorderStyle.THIN);

    final var statusHeading = header.createCell(1 + cellOffset);
    statusHeading.setCellValue("status");
    statusHeading.getCellStyle().setAlignment(HorizontalAlignment.CENTER);
    statusHeading.getCellStyle().setBorderLeft(BorderStyle.THIN);
    statusHeading.getCellStyle().setBorderTop(BorderStyle.THIN);
    statusHeading.getCellStyle().setBorderBottom(BorderStyle.THIN);
    statusHeading.getCellStyle().setBorderRight(BorderStyle.THIN);

    final var priceHeading = header.createCell(2 + cellOffset);
    priceHeading.setCellValue("price, USD");
    priceHeading.getCellStyle().setAlignment(HorizontalAlignment.CENTER);
    priceHeading.getCellStyle().setBorderLeft(BorderStyle.THIN);
    priceHeading.getCellStyle().setBorderTop(BorderStyle.THIN);
    priceHeading.getCellStyle().setBorderBottom(BorderStyle.THIN);
    priceHeading.getCellStyle().setBorderRight(BorderStyle.THIN);

    final var renewalPriceHeading = header.createCell(3 + cellOffset);
    renewalPriceHeading.setCellValue("renewal price, USD");
    renewalPriceHeading.getCellStyle().setAlignment(HorizontalAlignment.CENTER);
    renewalPriceHeading.getCellStyle().setBorderLeft(BorderStyle.THIN);
    renewalPriceHeading.getCellStyle().setBorderTop(BorderStyle.THIN);
    renewalPriceHeading.getCellStyle().setBorderBottom(BorderStyle.THIN);
    renewalPriceHeading.getCellStyle().setBorderRight(BorderStyle.THIN);

    final var latestHistoryHeading = header.createCell(4 + cellOffset);
    latestHistoryHeading.setCellValue("latest history");
    latestHistoryHeading.getCellStyle().setAlignment(HorizontalAlignment.CENTER);
    latestHistoryHeading.getCellStyle().setBorderLeft(BorderStyle.THIN);
    latestHistoryHeading.getCellStyle().setBorderTop(BorderStyle.THIN);
    latestHistoryHeading.getCellStyle().setBorderBottom(BorderStyle.THIN);
    latestHistoryHeading.getCellStyle().setBorderRight(BorderStyle.THIN);

    for (var i = 0; i < records.size(); ++i) {
      final var record = records.get(i);
      final var row = sheet.createRow(i + 1 + rowOffset);

      final var domain = row.createCell(0 + cellOffset);
      domain.setCellValue(record.getDomain());
      domain.getCellStyle().setAlignment(HorizontalAlignment.LEFT);
      domain.getCellStyle().setBorderLeft(BorderStyle.THIN);
      domain.getCellStyle().setBorderTop(BorderStyle.THIN);
      domain.getCellStyle().setBorderBottom(BorderStyle.THIN);
      domain.getCellStyle().setBorderRight(BorderStyle.THIN);

      final var state = row.createCell(1 + cellOffset);
      state.setCellValue(record.getState().toString());
      state.getCellStyle().setAlignment(HorizontalAlignment.CENTER);
      state.getCellStyle().setBorderLeft(BorderStyle.THIN);
      state.getCellStyle().setBorderTop(BorderStyle.THIN);
      state.getCellStyle().setBorderBottom(BorderStyle.THIN);
      state.getCellStyle().setBorderRight(BorderStyle.THIN);

      final var price = row.createCell(2 + cellOffset);
      price.setCellValue(record.getPrice().orElse("-"));
      price.getCellStyle().setAlignment(HorizontalAlignment.CENTER);
      price.getCellStyle().setBorderLeft(BorderStyle.THIN);
      price.getCellStyle().setBorderTop(BorderStyle.THIN);
      price.getCellStyle().setBorderBottom(BorderStyle.THIN);
      price.getCellStyle().setBorderRight(BorderStyle.THIN);

      final var renewalPrice = row.createCell(3 + cellOffset);
      renewalPrice.setCellValue(record.getRenewalPrice().orElse("-"));
      renewalPrice.getCellStyle().setAlignment(HorizontalAlignment.CENTER);
      renewalPrice.getCellStyle().setBorderLeft(BorderStyle.THIN);
      renewalPrice.getCellStyle().setBorderTop(BorderStyle.THIN);
      renewalPrice.getCellStyle().setBorderBottom(BorderStyle.THIN);
      renewalPrice.getCellStyle().setBorderRight(BorderStyle.THIN);

      final var dateFormat = new SimpleDateFormat("yyyy-MM-dd");
      final var latestHistory = row.createCell(4 + cellOffset);
      latestHistory.setCellValue(record.getLatestHistory().map(date -> dateFormat.format(date)).orElse("-"));
      latestHistory.getCellStyle().setAlignment(HorizontalAlignment.CENTER);
      latestHistory.getCellStyle().setBorderLeft(BorderStyle.THIN);
      latestHistory.getCellStyle().setBorderTop(BorderStyle.THIN);
      latestHistory.getCellStyle().setBorderBottom(BorderStyle.THIN);
      latestHistory.getCellStyle().setBorderRight(BorderStyle.THIN);
    }

    final var stream = new ByteArrayOutputStream();
    workbook.write(stream);
    workbook.close();

    return stream.toByteArray();
  }
}
