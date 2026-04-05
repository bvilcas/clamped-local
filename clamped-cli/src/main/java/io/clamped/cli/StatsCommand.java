package io.clamped.cli;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * $ clamped stats [--app my-service] [--since 24h]
 *
 * Prints a summary of event counts by severity and the top tags.
 */
public final class StatsCommand {

    private StatsCommand() {}

    public static void run(CliConfig config, String[] rawArgs) throws SQLException {
        List<String> args = CliConfig.mutableArgs(rawArgs);
        String app   = CliConfig.extractFlag(args, "--app");
        String since = CliConfig.extractFlag(args, "--since");

        String whereSince = since != null
            ? " AND first_seen > NOW() - INTERVAL '" + ListCommand.parseDuration(since) + "'"
            : "";
        String whereApp = app != null ? " AND app_name = ?" : "";

        // Overall counts by status
        String overallSql =
            "SELECT " +
            "  COUNT(*) FILTER (WHERE status = 'OPEN') AS open_count," +
            "  COUNT(*) FILTER (WHERE status = 'IN_PROGRESS') AS ack_count," +
            "  COUNT(*) FILTER (WHERE status = 'RESOLVED') AS resolved_count," +
            "  COUNT(*) FILTER (WHERE severity = 'CRITICAL' AND status = 'OPEN') AS critical_open," +
            "  COUNT(*) FILTER (WHERE severity = 'HIGH' AND status = 'OPEN') AS high_open," +
            "  COUNT(*) FILTER (WHERE severity = 'MEDIUM' AND status = 'OPEN') AS medium_open," +
            "  COUNT(*) FILTER (WHERE severity = 'LOW' AND status = 'OPEN') AS low_open " +
            "FROM clamped_events WHERE 1=1" + whereSince + whereApp;

        try (Connection conn = config.connect()) {
            try (PreparedStatement ps = conn.prepareStatement(overallSql)) {
                if (app != null) ps.setString(1, app);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        System.out.println("── Event Summary ──────────────────────────────");
                        System.out.printf("  Open:        %d   (Critical: %d  High: %d  Medium: %d  Low: %d)%n",
                            rs.getInt("open_count"),
                            rs.getInt("critical_open"),
                            rs.getInt("high_open"),
                            rs.getInt("medium_open"),
                            rs.getInt("low_open"));
                        System.out.printf("  In Progress:  %d%n", rs.getInt("ack_count"));
                        System.out.printf("  Resolved:     %d%n", rs.getInt("resolved_count"));
                    }
                }
            }

            // Top 10 tags by open event count
            String tagSql =
                "SELECT tag, COUNT(*) AS cnt FROM clamped_events " +
                "WHERE status = 'OPEN'" + whereSince + whereApp +
                " AND tag IS NOT NULL GROUP BY tag ORDER BY cnt DESC LIMIT 10";

            try (PreparedStatement ps = conn.prepareStatement(tagSql)) {
                if (app != null) ps.setString(1, app);
                try (ResultSet rs = ps.executeQuery()) {
                    System.out.println();
                    System.out.println("── Top Tags (open events) ─────────────────────");
                    boolean any = false;
                    while (rs.next()) {
                        any = true;
                        System.out.printf("  %-30s %d%n",
                            rs.getString("tag"), rs.getInt("cnt"));
                    }
                    if (!any) System.out.println("  (none)");
                }
            }

            // Top 5 most frequent unresolved exceptions
            String excSql =
                "SELECT exception_class, SUM(occurrence_count) AS total " +
                "FROM clamped_events " +
                "WHERE status = 'OPEN' AND exception_class IS NOT NULL" + whereSince + whereApp +
                " GROUP BY exception_class ORDER BY total DESC LIMIT 5";

            try (PreparedStatement ps = conn.prepareStatement(excSql)) {
                if (app != null) ps.setString(1, app);
                try (ResultSet rs = ps.executeQuery()) {
                    System.out.println();
                    System.out.println("── Top Exceptions (open, by occurrence) ───────");
                    boolean any = false;
                    while (rs.next()) {
                        any = true;
                        System.out.printf("  %-50s %d%n",
                            TableFormatter.truncate(rs.getString("exception_class"), 50),
                            rs.getLong("total"));
                    }
                    if (!any) System.out.println("  (none)");
                }
            }
        }
        System.out.println();
    }
}
