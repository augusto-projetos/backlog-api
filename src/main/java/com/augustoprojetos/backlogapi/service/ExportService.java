package com.augustoprojetos.backlogapi.service;

import com.augustoprojetos.backlogapi.entity.Item;
import com.augustoprojetos.backlogapi.entity.User;
import com.augustoprojetos.backlogapi.repository.ItemRepository;
import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Service
public class ExportService {

    @Autowired
    private ItemRepository itemRepository;

    // --- GERADOR DE EXCEL ESTILIZADO ---
    public ByteArrayInputStream gerarExcel(User user) throws IOException {
        List<Item> itens = itemRepository.findByUser(user);

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Meus Backlog");

            // --- ESTILOS ---

            // 1. Estilo do Cabeçalho (Fundo Azul Escuro, Texto Branco)
            CellStyle headerStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerFont.setFontHeightInPoints((short) 12);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_TEAL.getIndex()); // Cor chique
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            // 2. Estilo das Células Comuns (Borda fina)
            CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setBorderBottom(BorderStyle.THIN);
            dataStyle.setBorderTop(BorderStyle.THIN);
            dataStyle.setBorderRight(BorderStyle.THIN);
            dataStyle.setBorderLeft(BorderStyle.THIN);
            dataStyle.setVerticalAlignment(VerticalAlignment.TOP); // Texto no topo
            dataStyle.setWrapText(true); // Quebra linha se for grande

            // 3. Estilo Centralizado (Para Notas e Status)
            CellStyle centerStyle = workbook.createCellStyle();
            centerStyle.cloneStyleFrom(dataStyle);
            centerStyle.setAlignment(HorizontalAlignment.CENTER);

            // --- CABEÇALHO ---
            String[] colunas = {"Título", "Tipo", "Status", "Nota", "Resenha"};
            Row headerRow = sheet.createRow(0);
            headerRow.setHeightInPoints(25); // Altura maior no header

            for (int i = 0; i < colunas.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(colunas[i]);
                cell.setCellStyle(headerStyle);
            }

            // --- DADOS ---
            int rowIdx = 1;
            for (Item item : itens) {
                Row row = sheet.createRow(rowIdx++);

                // Título
                createCell(row, 0, item.getTitulo(), dataStyle);
                // Tipo
                createCell(row, 1, item.getTipo(), centerStyle);
                // Status
                createCell(row, 2, item.getStatus(), centerStyle);

                // Nota
                String nota = (item.getNota() != null && item.getNota() > 0) ? String.valueOf(item.getNota()) : "-";
                createCell(row, 3, nota, centerStyle);

                // Resenha
                createCell(row, 4, item.getResenha(), dataStyle);
            }

            // Ajuste de Largura das Colunas
            sheet.setColumnWidth(0, 25 * 256); // Título
            sheet.setColumnWidth(1, 12 * 256); // Tipo
            sheet.setColumnWidth(2, 15 * 256); // Status
            sheet.setColumnWidth(3, 8 * 256);  // Nota
            sheet.setColumnWidth(4, 50 * 256); // Resenha

            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        }
    }

    // Auxiliar para criar célula no Excel
    private void createCell(Row row, int column, String value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value != null ? value : "");
        cell.setCellStyle(style);
    }


    // --- GERADOR DE PDF ESTILIZADO ---
    public ByteArrayInputStream gerarPDF(User user) {
        Document document = new Document(PageSize.A4, 20, 20, 20, 20); // Margens menores
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // --- CORES DO TEMA ---
            Color corPrincipal = new Color(233, 69, 96); // Vermelho/Rosa
            Color corFundoHeader = new Color(44, 62, 80); // Azul Escuro
            Color corTextoHeader = Color.WHITE;
            Color corLinhaPar = new Color(240, 240, 240); // Cinza Claro

            // --- TÍTULO ---
            Font fontTitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, corPrincipal);
            Paragraph titulo = new Paragraph("Relatório - Meus Backlog", fontTitulo);
            titulo.setAlignment(Element.ALIGN_CENTER);
            titulo.setSpacingAfter(20);
            document.add(titulo);

            // --- TABELA ---
            PdfPTable table = new PdfPTable(5);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{3.5f, 2f, 2f, 1.5f, 5f});

            // Cabeçalho
            String[] headers = {"Título", "Tipo", "Status", "Nota", "Resenha"};
            Font fontHeader = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, corTextoHeader);

            for (String header : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(header, fontHeader));
                cell.setBackgroundColor(corFundoHeader);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                cell.setPadding(8); // Espaçamento interno
                table.addCell(cell);
            }

            // Dados
            List<Item> itens = itemRepository.findByUser(user);
            Font fontDados = FontFactory.getFont(FontFactory.HELVETICA, 10);

            boolean linhaPar = false; // Para fazer o efeito zebrado

            if (itens.isEmpty()) {
                PdfPCell cell = new PdfPCell(new Phrase("Nenhum item encontrado."));
                cell.setColspan(5);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setPadding(10);
                table.addCell(cell);
            } else {
                for (Item item : itens) {
                    linhaPar = !linhaPar; // Inverte a cada linha
                    Color corFundo = linhaPar ? corLinhaPar : Color.WHITE;

                    addCellPDF(table, item.getTitulo(), fontDados, corFundo, Element.ALIGN_LEFT);
                    addCellPDF(table, item.getTipo(), fontDados, corFundo, Element.ALIGN_CENTER);
                    addCellPDF(table, item.getStatus(), fontDados, corFundo, Element.ALIGN_CENTER);

                    String notaStr = "-";
                    if (item.getNota() != null && item.getNota() > 0) {
                        if (item.getNota() % 1 == 0) {
                            notaStr = String.valueOf(item.getNota().intValue()); // Inteiro
                        } else {
                            notaStr = String.valueOf(item.getNota()); // Quebrado (Ex: 9.5)
                        }
                    }
                    addCellPDF(table, notaStr, fontDados, corFundo, Element.ALIGN_CENTER);

                    addCellPDF(table, item.getResenha(), fontDados, corFundo, Element.ALIGN_LEFT);
                }
            }

            document.add(table);

            // Rodapé
            Font fontFooter = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, Color.GRAY);
            Paragraph footer = new Paragraph("Gerado automaticamente pelo sistema Meus Backlog", fontFooter);
            footer.setAlignment(Element.ALIGN_RIGHT);
            footer.setSpacingBefore(10);
            document.add(footer);

            document.close();

        } catch (Exception e) {
            System.err.println("ERRO AO GERAR PDF: " + e.getMessage());
            e.printStackTrace();
        }

        return new ByteArrayInputStream(out.toByteArray());
    }

    // Auxiliar para adicionar células no PDF
    private void addCellPDF(PdfPTable table, String text, Font font, Color background, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "", font));
        cell.setBackgroundColor(background);
        cell.setHorizontalAlignment(alignment);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(6); // Espaçamento para o texto não colar na borda
        cell.setBorderColor(Color.LIGHT_GRAY);
        table.addCell(cell);
    }
}