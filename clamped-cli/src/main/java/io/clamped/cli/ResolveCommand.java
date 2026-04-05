package io.clamped.cli;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

/**
 * Handles resolve, ack, and ignore commands.
 *
 * <pre>
 * $ clamped resolve 1042           # mark single event RESOLVED
 * $ clamped resolve --tag payment  # bulk resolve all OPEN events with tag
 * $ clamped ack 1042               # mark as IN_PROGRESS (alias: progress)
 * </pre>
 */
public final class ResolveCommand {

    private ResolveCommand() {}

    public static void run(CliConfig config, String newStatus, String[] rawArgs) throws SQLException {
        List<String> args = CliConfig.mutableArgs(rawArgs);
        String tag = CliConfig.extractFlag(args, "--tag");

        if (tag != null) {
            bulkUpdateByTag(config, newStatus, tag);
        } else if (!args.isEmpty()) {
            updateById(config, newStatus, args.get(0));
        } else {
            System.err.println("Usage: clamped resolve <id>  OR  clamped resolve --tag <tag>");
            System.exit(1);
        }
    }

    private static void updateById(CliConfig config, String status, String idStr) throws SQLException {
        long id;
        try {
            id = Long.parseLong(idStr);
        } catch (NumberFormatException e) {
            System.err.println("Error: id must be a number, got: " + idStr);
            System.exit(1);
            return;
        }

        String sql = "UPDATE clamped_events SET status = ? WHERE id = ?";

        try (Connection conn = config.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setLong(2, id);
            int updated = ps.executeUpdate();
            if (updated == 0) {
                System.out.println("Event #" + id + " not found.");
            } else {
                System.out.println("Event #" + id + " marked as " + ListCommand.toLabel(status) + ".");
            }
        }
    }

    private static void bulkUpdateByTag(CliConfig config, String status, String tag) throws SQLException {
        String sql = "UPDATE clamped_events SET status = ? WHERE tag = ? AND status = 'OPEN'";

        try (Connection conn = config.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setString(2, tag);
            int updated = ps.executeUpdate();
            System.out.printf("%d event(s) with tag '%s' marked as %s.%n", updated, tag, ListCommand.toLabel(status));
        }
    }
}
