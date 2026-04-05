package io.clamped.cli;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Entry point for the Clamped CLI tool.
 *
 * <pre>
 * Usage: clamped [--url <jdbc_url>] [--user <username>] [--password <password>] <command> [flags]
 *
 * Commands:
 *   list      Show events in a table
 *   show      Show full detail for a single event
 *   resolve   Mark event(s) as RESOLVED
 *   progress  Mark event(s) as IN PROGRESS
 *   ack       Mark event(s) as IN PROGRESS (alias)
 *   stats     Summary of event counts and top tags
 *   purge     Delete old resolved events
 *
 * Global flags:
 *   --url       JDBC URL (overrides CLAMPED_JDBC_URL env var / config file)
 *   --user      DB username
 *   --password  DB password
 *
 * list flags:
 *   --status    Filter by status (open, resolved, all) [default: open]
 *   --severity  Filter by severity (low, warn, high, critical)
 *   --tag       Filter by tag
 *   --app       Filter by app name
 *   --since     Time window (e.g. 24h, 7d, 30m)
 *   --limit     Max rows to return [default: 50]
 *
 * resolve/ack/ignore flags:
 *   <id>        Event ID to update
 *   --tag       Bulk-update all OPEN events with this tag
 *
 * purge flags:
 *   --before    Delete events resolved before this duration ago (e.g. 30d) [required]
 *   --all       Also purge IGNORED events
 *   --dry-run   Show count without deleting
 *
 * stats flags:
 *   --app       Filter by app name
 *   --since     Time window
 * </pre>
 */
public final class ClampedCli {

    public static void main(String[] args) {
        List<String> argList = new ArrayList<>(Arrays.asList(args));

        if (argList.isEmpty() || argList.contains("--help") || argList.contains("-h")) {
            printHelp();
            System.exit(argList.isEmpty() ? 1 : 0);
        }

        String command = argList.remove(0);

        if (command.equals("setup")) {
            SetupCommand.run();
            return;
        }

        // Auto-prompt setup if no config file exists and no env vars or flags provided
        boolean hasConfigFile = Files.exists(Paths.get(System.getProperty("user.home"), ".clamped", "config.properties"));
        boolean hasEnvOrFlags = System.getenv("CLAMPED_JDBC_URL") != null || argList.contains("--url");
        if (!hasConfigFile && !hasEnvOrFlags) {
            System.out.println("No configuration found. Running setup...");
            System.out.println();
            SetupCommand.run();
            System.out.println();
        }

        CliConfig config = CliConfig.load(argList);
        String[] remaining = argList.toArray(new String[0]);

        try {
            switch (command) {
                case "list":    ListCommand.run(config, remaining);                       break;
                case "show":    ShowCommand.run(config, remaining);                       break;
                case "resolve": ResolveCommand.run(config, "RESOLVED", remaining);        break;
                case "progress": ResolveCommand.run(config, "IN_PROGRESS", remaining);    break;
                case "ack":      ResolveCommand.run(config, "IN_PROGRESS", remaining);    break;
                case "stats":   StatsCommand.run(config, remaining);                      break;
                case "purge":   PurgeCommand.run(config, remaining);                      break;
                case "revert":  RevertCommand.run(config, remaining);                     break;
                default:
                    System.err.println("Unknown command: " + command);
                    printHelp();
                    System.exit(1);
            }
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        }
    }

    private static void printHelp() {
        System.out.println("Usage: clamped [--url <url>] [--user <u>] [--password <p>] <command> [flags]");
        System.out.println();
        System.out.println("Commands:");
        System.out.println("  list      Show events             (flags: --status --severity --tag --app --since --limit)");
        System.out.println("  show      Show event detail       show <id>");
        System.out.println("  resolve   Mark as resolved        resolve <id>  OR  resolve --tag <tag>");
        System.out.println("  progress  Mark as in progress     progress <id>");
        System.out.println("  revert    Revert back to open     revert <id>");
        System.out.println("  stats     Summary view            (flags: --app --since)");
        System.out.println("  purge     Delete old events       --before <duration> [--all] [--dry-run]");
        System.out.println("  revert    Revert back to OPEN      revert <id>  OR  revert --tag <tag>");
        System.out.println("  setup     Configure connection     creates ~/.clamped/config.properties");
        System.out.println();
        System.out.println("Connection (priority: flags > env vars > ~/.clamped/config.properties):");
        System.out.println("  CLAMPED_JDBC_URL, CLAMPED_USERNAME, CLAMPED_PASSWORD");
    }
}
