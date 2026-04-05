package io.clamped.cli;

import java.util.ArrayList;
import java.util.List;

/**
 * Renders data as an ASCII table with auto-sized columns.
 *
 * <pre>
 * +------+---------------------+-----------+------------------+-----------------------------+--------+
 * | ID   | Timestamp           | Severity  | Tag              | Message                     | Status |
 * +------+---------------------+-----------+------------------+-----------------------------+--------+
 * | 1042 | 2026-02-27 14:23:01 | HIGH      | payment-flow     | PaymentException: Card d... | OPEN   |
 * +------+---------------------+-----------+------------------+-----------------------------+--------+
 * </pre>
 */
public final class TableFormatter {

    private final String[] headers;
    private final int[] maxWidths;
    private final List<String[]> rows = new ArrayList<>();

    public TableFormatter(String... headers) {
        this.headers = headers;
        this.maxWidths = new int[headers.length];
        for (int i = 0; i < headers.length; i++) {
            maxWidths[i] = headers[i].length();
        }
    }

    public void addRow(String... values) {
        String[] row = new String[headers.length];
        for (int i = 0; i < headers.length; i++) {
            row[i] = (i < values.length && values[i] != null) ? values[i] : "";
            if (row[i].length() > maxWidths[i]) {
                maxWidths[i] = row[i].length();
            }
        }
        rows.add(row);
    }

    public void print() {
        if (rows.isEmpty()) {
            System.out.println("No results.");
            return;
        }
        String separator = buildSeparator();
        System.out.println(separator);
        System.out.println(buildRow(headers));
        System.out.println(separator);
        for (String[] row : rows) {
            System.out.println(buildRow(row));
        }
        System.out.println(separator);
    }

    private String buildSeparator() {
        StringBuilder sb = new StringBuilder("+");
        for (int w : maxWidths) {
            sb.append("-".repeat(w + 2)).append("+");
        }
        return sb.toString();
    }

    private String buildRow(String[] values) {
        StringBuilder sb = new StringBuilder("|");
        for (int i = 0; i < headers.length; i++) {
            String val = (i < values.length && values[i] != null) ? values[i] : "";
            sb.append(" ").append(pad(val, maxWidths[i])).append(" |");
        }
        return sb.toString();
    }

    private static String pad(String s, int width) {
        if (s.length() >= width) return s;
        return s + " ".repeat(width - s.length());
    }

    /**
     * Truncates a string to maxLen chars, appending "..." if truncated.
     */
    public static String truncate(String s, int maxLen) {
        if (s == null) return "";
        if (s.length() <= maxLen) return s;
        return s.substring(0, maxLen - 3) + "...";
    }
}
