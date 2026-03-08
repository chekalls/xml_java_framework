package mg.miniframework.persistence.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

public final class ExportCsvUtils {
    private static final char DEFAULT_DELIMITER = ';';

    private ExportCsvUtils() {
    }

    public static ByteArrayInputStream toCsvStream(List<String> headers, List<List<String>> rows) {
        return toCsvStream(headers, rows, DEFAULT_DELIMITER);
    }

    public static ByteArrayInputStream toCsvStream(List<String> headers, List<List<String>> rows, char delimiter) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(baos, StandardCharsets.UTF_8))) {
            if (headers != null && !headers.isEmpty()) {
                writer.println(joinRow(headers, delimiter));
            }
            if (rows != null) {
                for (List<String> row : rows) {
                    writer.println(joinRow(row, delimiter));
                }
            }
            writer.flush();
        }
        return new ByteArrayInputStream(baos.toByteArray());
    }

    private static String joinRow(List<String> cells, char delimiter) {
        if (cells == null || cells.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cells.size(); i++) {
            if (i > 0) {
                sb.append(delimiter);
            }
            sb.append(escape(cells.get(i), delimiter));
        }
        return sb.toString();
    }

    private static String escape(String value, char delimiter) {
        if (value == null) {
            return "";
        }
        String s = value;
        boolean mustQuote = s.indexOf(delimiter) >= 0 || s.contains("\"") || s.contains("\n")
                || s.contains("\r");
        if (s.contains("\"")) {
            s = s.replace("\"", "\"\"");
        }
        if (mustQuote) {
            return "\"" + s + "\"";
        }
        return s;
    }
}
