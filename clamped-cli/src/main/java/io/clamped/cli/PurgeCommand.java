package io.clamped.cli;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

/**
 * $ clamped purge --before 30d
 *
 * Deletes resolved (and optionally ignored) events older than the given duration.
 * Requires --before to be specified as a safety measure.
 */
public final class PurgeCommand {

    private PurgeCommand() {}

    public static void run(CliConfig config, String[] rawArgs) throws SQLException {
        List<String> args = CliConfig.mutableArgs(rawArgs);
        String before    = CliConfig.extractFlag(args, "--before");
        boolean all      = CliConfig.extractBoolFlag(args, "--all"); // include IN_PROGRESS too
        boolean dryRun   = CliConfig.extractBoolFlag(args, "--dry-run");

        if (before == null) {
            System.err.println("Usage: clamped purge --before <duration>  (e.g. 30d, 7d, 24h)");
            System.exit(1);
        }

        String interval = ListCommand.parseDuration(before);
        String statusCondition = all
            ? "(status = 'RESOLVED' OR status = 'IN_PROGRESS')"
            : "status = 'RESOLVED'";

        String sql = dryRun
            ? "SELECT COUNT(*) FROM clamped_events WHERE " + statusCondition +
              " AND first_seen < NOW() - INTERVAL '" + interval + "'"
            : "DELETE FROM clamped_events WHERE " + statusCondition +
              " AND first_seen < NOW() - INTERVAL '" + interval + "'";

        try (Connection conn = config.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (dryRun) {
                try (java.sql.ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        System.out.printf("[dry-run] Would delete %d event(s) older than %s.%n",
                            rs.getInt(1), before);
                    }
                }
            } else {
                int deleted = ps.executeUpdate();
                System.out.printf("Purged %d event(s) older than %s.%n", deleted, before);
            }
        }
    }
}
