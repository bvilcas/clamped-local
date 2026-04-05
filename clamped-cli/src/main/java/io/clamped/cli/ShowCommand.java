package io.clamped.cli;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * $ clamped show <id>
 *
 * Prints a detailed view of a single event including full stacktrace and metadata.
 */
public final class ShowCommand {

    private ShowCommand() {}

    public static void run(CliConfig config, String[] args) throws SQLException {
        if (args.length == 0) {
            System.err.println("Usage: clamped show <id>");
            System.exit(1);
        }

        long id;
        try {
            id = Long.parseLong(args[0]);
        } catch (NumberFormatException e) {
            System.err.println("Error: id must be a number, got: " + args[0]);
            System.exit(1);
            return;
        }

        String sql =
            "SELECT id, timestamp, app_name, environment, severity, tag, message, " +
            "       exception_class, stacktrace, metadata, source_file, source_line, " +
            "       source_method, thread_name, host, status, " +
            "       fingerprint, occurrence_count, first_seen " +
            "FROM clamped_events WHERE id = ?";

        try (Connection conn = config.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    System.out.println("Event #" + id + " not found.");
                    return;
                }
                printEvent(rs);
            }
        }
    }

    private static void printEvent(ResultSet rs) throws SQLException {
        String divider = "─".repeat(72);
        System.out.println(divider);
        System.out.printf("Event #%-10d  Status: %s%n",
            rs.getLong("id"), rs.getString("status"));
        System.out.println(divider);

        field("Severity",    rs.getString("severity"));
        field("Tag",         rs.getString("tag"));
        field("App",         rs.getString("app_name") + "  [" + rs.getString("environment") + "]");
        field("Message",     rs.getString("message"));
        field("Exception",   rs.getString("exception_class"));
        field("Source",      formatSource(rs));
        field("Thread",      rs.getString("thread_name"));
        field("Host",        rs.getString("host"));
        field("First seen",  rs.getString("first_seen"));
        field("Occurrences", String.valueOf(rs.getInt("occurrence_count")));
        field("Fingerprint", rs.getString("fingerprint"));
        field("Metadata",    rs.getString("metadata"));

        String stacktrace = rs.getString("stacktrace");
        if (stacktrace != null && !stacktrace.isBlank()) {
            System.out.println();
            System.out.println("Stacktrace:");
            System.out.println(stacktrace.trim());
        }
        System.out.println(divider);
    }

    private static void field(String label, String value) {
        if (value != null && !value.isEmpty() && !value.equals("null")) {
            System.out.printf("  %-14s %s%n", label + ":", value);
        }
    }

    private static String formatSource(ResultSet rs) throws SQLException {
        String file = rs.getString("source_file");
        int line = rs.getInt("source_line");
        String method = rs.getString("source_method");
        if (file == null) return null;
        return file + (line > 0 ? ":" + line : "") + (method != null ? " [" + method + "]" : "");
    }
}
