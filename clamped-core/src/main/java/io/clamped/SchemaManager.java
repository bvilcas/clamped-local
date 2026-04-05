package io.clamped;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Ensures the clamped_events database and table exist. Called once during Clamped.init() when
 * autoCreateTables is true.
 */
public final class SchemaManager {

    private SchemaManager() {}

    /**
     * Ensures the target database exists, creating it if necessary, then creates the table.
     *
     * If the database doesn't exist yet (SQL state 3D000), connects to the built-in "postgres"
     * maintenance database — which is present on every PostgreSQL server — runs CREATE DATABASE,
     * then reconnects to the target database to create the table.
     */
    public static void ensureDatabaseAndTablesExist(String jdbcUrl, String username, String password)
            throws SQLException {
        try (Connection conn = DriverManager.getConnection(jdbcUrl, username, password)) {
            createTablesIfNeeded(conn);
            return;
        } catch (SQLException e) {
            if (!"3D000".equals(e.getSQLState())) {
                throw e; // not a "database does not exist" error — rethrow as-is
            }
        }

        // Database doesn't exist — bootstrap via the postgres maintenance database
        String dbName = extractDbName(jdbcUrl);
        String adminUrl = replaceDbName(jdbcUrl, "postgres");

        System.out.println("[Clamped] Database '" + dbName + "' not found, creating it...");
        try (Connection adminConn = DriverManager.getConnection(adminUrl, username, password);
             Statement stmt = adminConn.createStatement()) {
            stmt.execute("CREATE DATABASE \"" + dbName.replace("\"", "\"\"") + "\"");
            System.out.println("[Clamped] Created database: " + dbName);
        } catch (SQLException e) {
            throw new SQLException(
                "[Clamped] Could not create database '" + dbName + "'. " +
                "Either create it manually (CREATE DATABASE " + dbName + ") " +
                "or grant CREATEDB to the user. Cause: " + e.getMessage(), e);
        }

        try (Connection conn = DriverManager.getConnection(jdbcUrl, username, password)) {
            createTablesIfNeeded(conn);
        }
    }

    static String extractDbName(String jdbcUrl) {
        // jdbc:postgresql://host:port/dbname[?params]
        String tail = jdbcUrl.substring(jdbcUrl.lastIndexOf('/') + 1);
        int q = tail.indexOf('?');
        return q >= 0 ? tail.substring(0, q) : tail;
    }

    static String replaceDbName(String jdbcUrl, String newDb) {
        int lastSlash = jdbcUrl.lastIndexOf('/');
        String base = jdbcUrl.substring(0, lastSlash + 1);
        String tail = jdbcUrl.substring(lastSlash + 1);
        String params = tail.contains("?") ? tail.substring(tail.indexOf('?')) : "";
        return base + newDb + params;
    }

    public static void createTablesIfNeeded(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(CREATE_TABLE_SQL);
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_clamped_status ON clamped_events(status)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_clamped_severity ON clamped_events(severity)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_clamped_tag ON clamped_events(tag)");
            // UNIQUE index required for ON CONFLICT (fingerprint) upsert
            stmt.execute("CREATE UNIQUE INDEX IF NOT EXISTS idx_clamped_fingerprint ON clamped_events(fingerprint)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_clamped_app_env ON clamped_events(app_name, environment)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_clamped_first_seen ON clamped_events(first_seen DESC)");
        }
    }

    private static final String CREATE_TABLE_SQL =
        "CREATE TABLE IF NOT EXISTS clamped_events (\n" +
        "    id               BIGSERIAL PRIMARY KEY,\n" +
        "    timestamp        TIMESTAMPTZ NOT NULL DEFAULT NOW(),\n" +
        "    app_name         VARCHAR(255) NOT NULL,\n" +
        "    environment      VARCHAR(50) NOT NULL,\n" +
        "    severity         VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',\n" +
        "    tag              VARCHAR(255),\n" +
        "    message          TEXT NOT NULL,\n" +
        "    exception_class  VARCHAR(500),\n" +
        "    stacktrace       TEXT,\n" +
        "    metadata         JSONB DEFAULT '{}',\n" +
        "    source_file      VARCHAR(500),\n" +
        "    source_line      INTEGER,\n" +
        "    source_method    VARCHAR(255),\n" +
        "    thread_name      VARCHAR(255),\n" +
        "    host             VARCHAR(255),\n" +
        "    status           VARCHAR(20) NOT NULL DEFAULT 'OPEN',\n" +
        "    fingerprint      VARCHAR(64) NOT NULL,\n" +
        "    occurrence_count INTEGER NOT NULL DEFAULT 1,\n" +
        "    first_seen       TIMESTAMPTZ NOT NULL DEFAULT NOW()\n" +
        ")";
}
