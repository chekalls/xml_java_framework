package mg.miniframework.persistence.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public final class ExportXlsxUtils {
    private ExportXlsxUtils() {
    }

    public static ByteArrayInputStream toXlsxStream(String sheetName, List<String> headers,
            List<List<String>> rows) {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
                ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            XSSFSheet sheet = workbook.createSheet(sheetName != null ? sheetName : "Export");

            int rowIndex = 0;
            if (headers != null && !headers.isEmpty()) {
                Row headerRow = sheet.createRow(rowIndex++);
                CellStyle headerStyle = createHeaderStyle(workbook);
                for (int i = 0; i < headers.size(); i++) {
                    Cell cell = headerRow.createCell(i);
                    cell.setCellValue(headers.get(i));
                    cell.setCellStyle(headerStyle);
                }
            }

            if (rows != null) {
                for (List<String> rowData : rows) {
                    Row row = sheet.createRow(rowIndex++);
                    if (rowData == null) {
                        continue;
                    }
                    for (int i = 0; i < rowData.size(); i++) {
                        Cell cell = row.createCell(i);
                        cell.setCellValue(rowData.get(i));
                    }
                }
            }

            int colCount = headers != null ? headers.size() : 0;
            if (rows != null) {
                for (List<String> rowData : rows) {
                    if (rowData != null && rowData.size() > colCount) {
                        colCount = rowData.size();
                    }
                }
            }
            for (int i = 0; i < colCount; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(baos);
            return new ByteArrayInputStream(baos.toByteArray());
        } catch (Exception e) {
            return new ByteArrayInputStream(new byte[0]);
        }
    }

    private static CellStyle createHeaderStyle(XSSFWorkbook workbook) {
        Font font = workbook.createFont();
        font.setBold(true);
        CellStyle style = workbook.createCellStyle();
        style.setFont(font);
        return style;
    }
}
