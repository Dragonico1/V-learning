package com.elearning.platform.util;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

/** Genera PDF (OpenPDF) y Excel (POI). Los archivos no se cifran: se protegen con TLS en tránsito. */
public final class ExportadorInforme {

    private ExportadorInforme() {}

    public static byte[] aExcel(TablaInforme t) {
        try (Workbook libro = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet hoja = libro.createSheet("Informe");
            CellStyle negrita = libro.createCellStyle();
            org.apache.poi.ss.usermodel.Font f = libro.createFont();
            f.setBold(true);
            negrita.setFont(f);
            CellStyle cabecera = libro.createCellStyle();
            cabecera.cloneStyleFrom(negrita);
            cabecera.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            cabecera.setFillPattern(FillPatternType.SOLID);

            int r = 0;
            Row titulo = hoja.createRow(r++);
            Cell ct = titulo.createCell(0);
            ct.setCellValue(t.titulo());
            ct.setCellStyle(negrita);
            for (String linea : t.contexto()) hoja.createRow(r++).createCell(0).setCellValue(linea);
            r++;
            Row cab = hoja.createRow(r++);
            for (int c = 0; c < t.columnas().size(); c++) {
                Cell cell = cab.createCell(c);
                cell.setCellValue(t.columnas().get(c));
                cell.setCellStyle(cabecera);
            }
            hoja.createFreezePane(0, r);
            for (java.util.List<String> fila : t.filas()) {
                Row row = hoja.createRow(r++);
                for (int c = 0; c < fila.size(); c++) row.createCell(c).setCellValue(fila.get(c));
            }
            r++;
            for (String nota : t.notas()) hoja.createRow(r++).createCell(0).setCellValue(nota);
            for (int c = 0; c < t.columnas().size(); c++) {
                hoja.autoSizeColumn(c);
                if (hoja.getColumnWidth(c) > 12000) hoja.setColumnWidth(c, 12000);
            }
            libro.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static byte[] aPdf(TablaInforme t) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4.rotate(), 36, 36, 36, 36);
        try {
            PdfWriter.getInstance(doc, out);
            doc.addTitle(t.titulo());
            doc.open();
            Font fTitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
            Font fTexto = FontFactory.getFont(FontFactory.HELVETICA, 10);
            Font fCab = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);
            Font fCelda = FontFactory.getFont(FontFactory.HELVETICA, 9);
            doc.add(new Paragraph(t.titulo(), fTitulo));
            for (String linea : t.contexto()) doc.add(new Paragraph(linea, fTexto));
            doc.add(new Paragraph(" "));
            PdfPTable tabla = new PdfPTable(t.columnas().size());
            tabla.setWidthPercentage(100);
            tabla.setHeaderRows(1);
            for (String c : t.columnas()) {
                PdfPCell cell = new PdfPCell(new Phrase(c, fCab));
                cell.setBackgroundColor(new Color(0x5B, 0x21, 0xB6));
                cell.setPadding(4);
                cell.setHorizontalAlignment(Element.ALIGN_LEFT);
                tabla.addCell(cell);
            }
            for (java.util.List<String> fila : t.filas()) {
                for (String v : fila) {
                    PdfPCell cell = new PdfPCell(new Phrase(v == null ? "" : v, fCelda));
                    cell.setPadding(3);
                    tabla.addCell(cell);
                }
            }
            doc.add(tabla);
            doc.add(new Paragraph(" "));
            for (String nota : t.notas()) doc.add(new Paragraph(nota, fTexto));
            doc.close();
            return out.toByteArray();
        } catch (DocumentException e) {
            throw new IllegalStateException("No se pudo generar el PDF", e);
        }
    }
}
