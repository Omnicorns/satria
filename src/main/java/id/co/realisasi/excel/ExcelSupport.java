package id.co.realisasi.excel;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/** Helper Excel bersama untuk template/import/export (RKA, mata anggaran). */
final class ExcelSupport {

    private ExcelSupport() {
    }

    static String text(Row row, int col, DataFormatter fmt) {
        Cell cell = row.getCell(col);
        return cell == null ? "" : fmt.formatCellValue(cell).trim();
    }

    static boolean isBlank(Row row, int cols, DataFormatter fmt) {
        for (int c = 0; c < cols; c++) {
            if (!text(row, c, fmt).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    static void header(Workbook wb, Sheet sheet, String[] cols) {
        CellStyle style = wb.createCellStyle();
        Font bold = wb.createFont();
        bold.setBold(true);
        style.setFont(bold);
        Row h = sheet.createRow(0);
        for (int i = 0; i < cols.length; i++) {
            Cell c = h.createCell(i);
            c.setCellValue(cols[i]);
            c.setCellStyle(style);
        }
        sheet.createFreezePane(0, 1);
    }

    static void autosize(Sheet sheet, int cols) {
        for (int i = 0; i < cols; i++) {
            sheet.setColumnWidth(i, 22 * 256);
        }
    }

    static byte[] write(Workbook wb) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        wb.write(out);
        return out.toByteArray();
    }
}
