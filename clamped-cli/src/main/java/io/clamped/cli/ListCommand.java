package io.clamped.cli;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * $ clamped list [--status open] [--severity high] [--tag payment-flow] [--app my-service] [--since 24h] [--limit 50]
 */
public final class ListCommand {

    private ListCommand() {}

    public static void run(CliConfig config, String[] rawArgs) throws SQLException {
        List<String> args = CliConfig.mutableArgs(rawArgs);

        String status    = CliConfig.extractFlag(args, "--status");
        String severity  = CliConfig.extractFlag(args, "--severity");
        String tag       = CliConfig.extractFlag(args, "--tag");
        String app       = CliConfig.extractFlag(args, "--app");
        String since     = CliConfig.extractFlag(args, "--since");
        String limitStr  = CliConfig.extractFlag(args, "--limit");

        int limit = 50;
        if (limitStr != null) {
            try { limit = Integer.parseInt(limitStr); } catch (NumberFormatException ignored) {}
        }

        // Default to showing OPEN events if no status specified
        if (status == null) status = "OPEN";

        StringBuilder sql = new StringBuilder(
            "SELECT id, first_seen, severity, tag, message, status, occurrence_count " +
            "FROM clamped_events WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (!"all".equalsIgnoreCase(status)) {
            sql.append(" AND UPPER(status) = ?");
            params.add(status.toUpperCase());
        }
        if (severity != null) {
            sql.append(" AND UPPER(severity) = ?");
            params.add(severity.toUpperCase());
        }
        if (tag != null) {
            sql.append(" AND tag = ?");
            params.add(tag);
        }
        if (app != null) {
            sql.append(" AND app_name = ?");
            params.add(app);
        }
        if (since != null) {
            String interval = parseDuration(since);
            sql.append(" AND first_seen > NOW() - INTERVAL '").append(interval).append("'");
        }

        sql.append(" ORDER BY first_seen DESC LIMIT ?");
        params.add(limit);

        TableFormatter table = new TableFormatter("ID", "First Seen", "Severity", "Tag", "Message", "Status", "#");

        try (Connection conn = config.connect();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    table.addRow(
                        String.valueOf(rs.getLong("id")),
                        formatTimestamp(rs.getString("first_seen")),
                        rs.getString("severity"),
                        nullToEmpty(rs.getString("tag")),
                        TableFormatter.truncate(rs.getString("message"), 40),
                        toLabel(rs.getString("status")),
                        String.valueOf(rs.getInt("occurrence_count"))
                    );
                }
            }
        }
        table.print();
    }

    /** Converts CLI duration strings (24h, 7d, 30m) to PostgreSQL interval strings. */
    static String parseDuration(String s) {
        s = s.trim().toLowerCase();
        if (s.endsWith("d")) return s.replace("d", " days");
        if (s.endsWith("h")) return s.replace("h", " hours");
        if (s.endsWith("m")) return s.replace("m", " minutes");
        return s; // pass through unmodified for flexibility
    }

    private static String formatTimestamp(String ts) {
        if (ts == null) return "";
        // Trim microseconds: "2026-02-27 14:23:01.123456+00" → "2026-02-27 14:23:01"
        return ts.length() > 19 ? ts.substring(0, 19) : ts;
    }

    private static String nullToEmpty(String s) {
        return s != null ? s : "";
    }

    static String toLabel(String status) {
        if (status == null) return "";
        switch (status) {
            case "OPEN":         return "Open";
            case "IN_PROGRESS":  return "In Progress";
            case "RESOLVED":     return "Resolved";
            default:             return status;
        }
    }
}
