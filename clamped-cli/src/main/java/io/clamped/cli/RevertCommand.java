package io.clamped.cli;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

/**
 * Sets a resolved/acknowledged/ignored event back to OPEN.
 *
 * <pre>
 * $ clamped revert 1042           # revert single event back to OPEN
 * $ clamped revert --tag payment  # bulk revert all non-OPEN events with tag
 * </pre>
 */
public final class RevertCommand {

    private RevertCommand() {}

    public static void run(CliConfig config, String[] rawArgs) throws SQLException {
        List<String> args = CliConfig.mutableArgs(rawArgs);
        String tag = CliConfig.extractFlag(args, "--tag");

        if (tag != null) {
            bulkRevertByTag(config, tag);
        } else if (!args.isEmpty()) {
            revertById(config, args.get(0));
        } else {
            System.err.println("Usage: clamped revert <id>  OR  clamped revert --tag <tag>");
            System.exit(1);
        }
    }

    private static void revertById(CliConfig config, String idStr) throws SQLException {
        long id;
        try {
            id = Long.parseLong(idStr);
        } catch (NumberFormatException e) {
            System.err.println("Error: id must be a number, got: " + idStr);
            System.exit(1);
            return;
        }

        String sql = "UPDATE clamped_events SET status = 'OPEN' WHERE id = ?";

        try (Connection conn = config.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            int updated = ps.executeUpdate();
            if (updated == 0) {
                System.out.println("Event #" + id + " not found.");
            } else {
                System.out.println("Event #" + id + " reverted to OPEN.");
            }
        }
    }

    private static void bulkRevertByTag(CliConfig config, String tag) throws SQLException {
        String sql = "UPDATE clamped_events SET status = 'OPEN' " +
                     "WHERE tag = ? AND status != 'OPEN'";

        try (Connection conn = config.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tag);
            int updated = ps.executeUpdate();
            System.out.printf("%d event(s) with tag '%s' reverted to OPEN.%n", updated, tag);
        }
    }
}
