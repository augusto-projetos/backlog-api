package com.augustoprojetos.backlogapi.service;

import com.augustoprojetos.backlogapi.entity.Item;
import com.augustoprojetos.backlogapi.entity.User;
import com.augustoprojetos.backlogapi.repository.ItemRepository;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Service
public class ExportService {

    @Autowired
    private ItemRepository itemRepository;

    // --- 1. GERADOR DE EXCEL (.xlsx) ---
    public ByteArrayInputStream gerarExcel(User user) throws IOException {
        List<Item> itens = itemRepository.findByUser(user);

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Meus Backlog");

            // Estilo do Cabeçalho
            CellStyle headerStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont(); // Fonte do Excel explícita
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // Cabeçalho
            Row headerRow = sheet.createRow(0);
            String[] colunas = {"Título", "Tipo", "Status", "Nota", "Resenha"};

            for (int i = 0; i < colunas.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(colunas[i]);
                cell.setCellStyle(headerStyle);
            }

            // Dados
            int rowIdx = 1;
            for (Item item : itens) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(item.getTitulo());
                row.createCell(1).setCellValue(item.getTipo());
                row.createCell(2).setCellValue(item.getStatus());

                if (item.getNota() != null && item.getNota() > 0) {
                    row.createCell(3).setCellValue(item.getNota());
                } else {
                    row.createCell(3).setCellValue("-");
                }

                row.createCell(4).setCellValue(item.getResenha() != null ? item.getResenha() : "");
            }

            // Ajuste automático de largura
            for (int i = 0; i < colunas.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        }
    }

    // --- 2. GERADOR DE PDF ---
    public ByteArrayInputStream gerarPDF(User user) {
        Document document = new Document();
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // Título
            com.lowagie.text.Font fontTitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Paragraph titulo = new Paragraph("Relatório - Meus Backlog", fontTitulo);
            titulo.setAlignment(Element.ALIGN_CENTER);
            document.add(titulo);
            document.add(new Paragraph(" "));

            // Tabela
            PdfPTable table = new PdfPTable(5);
            table.setWidthPercentage(100);
            table.setWidths(new int[]{3, 2, 2, 1, 4});

            // Cabeçalho
            String[] headers = {"Título", "Tipo", "Status", "Nota", "Resenha"};
            for (String header : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(header));
                cell.setGrayFill(0.9f);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                table.addCell(cell);
            }

            // Dados
            List<Item> itens = itemRepository.findByUser(user);

            if (itens.isEmpty()) {
                document.add(new Paragraph("Nenhum item encontrado nesta coleção."));
            } else {
                for (Item item : itens) {
                    // Título
                    table.addCell(item.getTitulo() != null ? item.getTitulo() : "Sem Título");

                    // Tipo
                    table.addCell(item.getTipo() != null ? item.getTipo() : "-");

                    // Status
                    table.addCell(item.getStatus() != null ? item.getStatus() : "-");

                    // Nota
                    if (item.getNota() != null) {
                        table.addCell(String.valueOf(item.getNota()));
                    } else {
                        table.addCell("-");
                    }

                    // Resenha
                    String resenha = item.getResenha();
                    if (resenha != null && !resenha.isEmpty()) {
                        table.addCell(resenha);
                    } else {
                        table.addCell(""); // Célula vazia se não tiver resenha
                    }
                }
                document.add(table);
            }

            document.close();

        } catch (Exception e) {
            System.err.println("ERRO AO GERAR PDF: " + e.getMessage());
            e.printStackTrace();
        }

        return new ByteArrayInputStream(out.toByteArray());
    }
}