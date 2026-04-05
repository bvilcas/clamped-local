package io.clamped;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Background daemon thread that drains the EventQueue and batch-upserts events to PostgreSQL.
 *
 * Uses a persistent JDBC connection that reconnects automatically on failure.
 * On fingerprint conflict, increments occurrence_count rather than inserting a duplicate row.
 */
public final class EventFlusher implements Runnable {

    private static final String UPSERT_SQL =
        "INSERT INTO clamped_events " +
        "  (app_name, environment, severity, tag, message, exception_class, stacktrace, metadata," +
        "   source_file, source_line, source_method, thread_name, host, fingerprint) " +
        "VALUES (?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?, ?, ?, ?, ?, ?) " +
        "ON CONFLICT (fingerprint) DO UPDATE SET " +
        "  occurrence_count = clamped_events.occurrence_count + 1";

    private final EventQueue queue;
    private final ClampedConfig config;
    private volatile boolean running = true;
    private Connection connection;

    public EventFlusher(EventQueue queue, ClampedConfig config) {
        this.queue = queue;
        this.config = config;
    }

    public void stop() {
        running = false;
    }

    @Override
    public void run() {
        while (running) {
            try {
                Thread.sleep(config.getFlushIntervalSeconds() * 1000L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
            safeFlush();
        }
        // Final flush on shutdown
        safeFlush();
        closeConnection();
    }

    /**
     * Flush all queued events synchronously. Called by EventFlusher's own thread and also
     * directly by Clamped.shutdown() for the final drain.
     */
    public void flush() {
        if (queue.isEmpty()) return;

        List<ClampedEvent> batch = new ArrayList<>(config.getBatchSize());
        ClampedEvent event;
        while (batch.size() < config.getBatchSize() && (event = queue.poll()) != null) {
            batch.add(event);
        }
        if (batch.isEmpty()) return;

        try {
            Connection conn = getConnection();
            try (PreparedStatement ps = conn.prepareStatement(UPSERT_SQL)) {
                for (ClampedEvent e : batch) {
                    ps.setString(1, e.appName);
                    ps.setString(2, e.environment);
                    ps.setString(3, e.severity.name());
                    ps.setString(4, e.tag);
                    ps.setString(5, e.message);
                    ps.setString(6, e.exceptionClass);
                    ps.setString(7, e.stacktrace);
                    ps.setString(8, toJson(e.metadata));
                    ps.setString(9, e.sourceFile);
                    if (e.sourceLine > 0) {
                        ps.setInt(10, e.sourceLine);
                    } else {
                        ps.setNull(10, Types.INTEGER);
                    }
                    ps.setString(11, e.sourceMethod);
                    ps.setString(12, e.threadName);
                    ps.setString(13, e.host);
                    ps.setString(14, e.fingerprint);
                    ps.addBatch();
                }
                ps.executeBatch();
            }
        } catch (SQLException e) {
            System.err.println("[Clamped] DB flush error: " + e.getMessage());
            closeConnection(); // Force reconnect on next flush
        }
    }

    private void safeFlush() {
        try {
            flush();
        } catch (Exception e) {
            System.err.println("[Clamped] Unexpected flush error: " + e.getMessage());
        }
    }

    private Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(
                config.getJdbcUrl(), config.getUsername(), config.getPassword());
        }
        return connection;
    }

    private void closeConnection() {
        if (connection != null) {
            try { connection.close(); } catch (SQLException ignored) {}
            connection = null;
        }
    }

    /**
     * Serializes a Map to a JSON object string without any external library dependency.
     * Handles String, Number, Boolean, and null values.
     */
    static String toJson(Map<String, Object> map) {
        if (map == null || map.isEmpty()) return "{}";
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!first) sb.append(',');
            first = false;
            sb.append('"').append(escapeJson(entry.getKey())).append('"').append(':');
            Object val = entry.getValue();
            if (val == null) {
                sb.append("null");
            } else if (val instanceof Number || val instanceof Boolean) {
                sb.append(val);
            } else {
                sb.append('"').append(escapeJson(val.toString())).append('"');
            }
        }
        sb.append('}');
        return sb.toString();
    }

    private static String escapeJson(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
