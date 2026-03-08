package mg.miniframework.persistence.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

public final class ExportPdfUtils {
    private static final float MARGIN = 40f;
    private static final float ROW_HEIGHT = 18f;
    private static final float FONT_SIZE = 10f;

    private ExportPdfUtils() {
    }

    public static ByteArrayInputStream toPdfStream(String title, List<String> headers, List<List<String>> rows) {
        try (PDDocument doc = new PDDocument();
                ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);

            float pageWidth = page.getMediaBox().getWidth();
            float yStart = page.getMediaBox().getHeight() - MARGIN;
            float tableWidth = pageWidth - 2 * MARGIN;
            int colCount = headers != null ? headers.size() : 0;
            if (rows != null) {
                for (List<String> row : rows) {
                    if (row != null && row.size() > colCount) {
                        colCount = row.size();
                    }
                }
            }
            float colWidth = colCount > 0 ? tableWidth / colCount : tableWidth;

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                cs.setFont(PDType1Font.HELVETICA_BOLD, 12f);
                if (title != null && !title.isBlank()) {
                    cs.beginText();
                    cs.newLineAtOffset(MARGIN, yStart);
                    cs.showText(title);
                    cs.endText();
                    yStart -= ROW_HEIGHT;
                }

                float y = yStart;
                if (headers != null && !headers.isEmpty()) {
                    cs.setFont(PDType1Font.HELVETICA_BOLD, FONT_SIZE);
                    y = drawRow(cs, headers, y, colWidth);
                }

                if (rows != null) {
                    cs.setFont(PDType1Font.HELVETICA, FONT_SIZE);
                    for (List<String> row : rows) {
                        if (y - ROW_HEIGHT < MARGIN) {
                            cs.close();
                            page = new PDPage(PDRectangle.A4);
                            doc.addPage(page);
                            y = page.getMediaBox().getHeight() - MARGIN;
                            try (PDPageContentStream newCs = new PDPageContentStream(doc, page)) {
                                newCs.setFont(PDType1Font.HELVETICA, FONT_SIZE);
                                y = drawRow(newCs, row, y, colWidth);
                            }
                            return new ByteArrayInputStream(new byte[0]);
                        }
                        y = drawRow(cs, row, y, colWidth);
                    }
                }
            }

            doc.save(baos);
            return new ByteArrayInputStream(baos.toByteArray());
        } catch (Exception e) {
            return new ByteArrayInputStream(new byte[0]);
        }
    }

    private static float drawRow(PDPageContentStream cs, List<String> cells, float y, float colWidth)
            throws Exception {
        float x = MARGIN;
        if (cells != null) {
            for (String cell : cells) {
                cs.beginText();
                cs.newLineAtOffset(x, y);
                cs.showText(cell != null ? cell : "");
                cs.endText();
                x += colWidth;
            }
        }
        return y - ROW_HEIGHT;
    }
}
