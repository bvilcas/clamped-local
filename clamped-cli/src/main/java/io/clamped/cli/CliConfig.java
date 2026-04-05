package io.clamped.cli;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Resolves the PostgreSQL connection configuration for the CLI.
 *
 * Priority (highest to lowest):
 *   1. CLI flags: --url, --user, --password
 *   2. Environment variables: CLAMPED_JDBC_URL, CLAMPED_USERNAME, CLAMPED_PASSWORD
 *   3. Config file: ~/.clamped/config.properties
 *
 * Config file format:
 * <pre>
 *   jdbcUrl=jdbc:postgresql://localhost:5432/clamped_db
 *   username=app_user
 *   password=secret
 * </pre>
 */
public final class CliConfig {

    final String jdbcUrl;
    final String username;
    final String password;

    private CliConfig(String jdbcUrl, String username, String password) {
        this.jdbcUrl = jdbcUrl;
        this.username = username;
        this.password = password;
    }

    /**
     * Parses and removes global flags (--url, --user, --password) from the mutable arg list,
     * then resolves the connection config via env vars and config file as fallbacks.
     */
    public static CliConfig load(List<String> args) {
        String url = extractFlag(args, "--url");
        String user = extractFlag(args, "--user");
        String password = extractFlag(args, "--password");

        // Fallback to env vars
        if (url == null) url = System.getenv("CLAMPED_JDBC_URL");
        if (user == null) user = System.getenv("CLAMPED_USERNAME");
        if (password == null) password = System.getenv("CLAMPED_PASSWORD");

        // Fallback to config file
        if (url == null || user == null) {
            Properties props = loadConfigFile();
            if (url == null) url = props.getProperty("jdbcUrl");
            if (user == null) user = props.getProperty("username");
            if (password == null) password = props.getProperty("password");
        }

        if (url == null) {
            System.err.println("Error: No database URL configured.");
            System.err.println("Set CLAMPED_JDBC_URL, use --url, or create ~/.clamped/config.properties");
            System.exit(1);
        }

        return new CliConfig(url, user, password);
    }

    public Connection connect() throws SQLException {
        return DriverManager.getConnection(jdbcUrl, username, password);
    }

    private static Properties loadConfigFile() {
        Properties props = new Properties();
        Path configPath = Paths.get(System.getProperty("user.home"), ".clamped", "config.properties");
        if (configPath.toFile().exists()) {
            try (FileInputStream fis = new FileInputStream(configPath.toFile())) {
                props.load(fis);
            } catch (IOException ignored) {}
        }
        return props;
    }

    static String extractFlag(List<String> args, String flag) {
        for (int i = 0; i < args.size() - 1; i++) {
            if (args.get(i).equals(flag)) {
                String val = args.get(i + 1);
                args.remove(i + 1);
                args.remove(i);
                return val;
            }
        }
        return null;
    }

    static boolean extractBoolFlag(List<String> args, String flag) {
        for (int i = 0; i < args.size(); i++) {
            if (args.get(i).equals(flag)) {
                args.remove(i);
                return true;
            }
        }
        return false;
    }

    static List<String> mutableArgs(String[] args) {
        return new ArrayList<>(java.util.Arrays.asList(args));
    }
}
