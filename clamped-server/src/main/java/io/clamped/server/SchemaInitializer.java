package io.clamped.server;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

/**
 * Creates the clamped_events table on startup if it doesn't exist.
 * Runs before DemoSeeder (Order 1 vs Order 2).
 */
@Component
@Order(1)
public class SchemaInitializer implements ApplicationRunner {

    private final DataSource dataSource;

    public SchemaInitializer(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute(
                "CREATE TABLE IF NOT EXISTS clamped_events (" +
                "    id               BIGSERIAL PRIMARY KEY," +
                "    timestamp        TIMESTAMPTZ NOT NULL DEFAULT NOW()," +
                "    app_name         VARCHAR(255) NOT NULL," +
                "    environment      VARCHAR(50) NOT NULL," +
                "    severity         VARCHAR(20) NOT NULL DEFAULT 'MEDIUM'," +
                "    tag              VARCHAR(255)," +
                "    message          TEXT NOT NULL," +
                "    exception_class  VARCHAR(500)," +
                "    stacktrace       TEXT," +
                "    metadata         JSONB DEFAULT '{}'," +
                "    source_file      VARCHAR(500)," +
                "    source_line      INTEGER," +
                "    source_method    VARCHAR(255)," +
                "    thread_name      VARCHAR(255)," +
                "    host             VARCHAR(255)," +
                "    status           VARCHAR(20) NOT NULL DEFAULT 'OPEN'," +
                "    fingerprint      VARCHAR(64) NOT NULL," +
                "    occurrence_count INTEGER NOT NULL DEFAULT 1," +
                "    first_seen       TIMESTAMPTZ NOT NULL DEFAULT NOW()" +
                ")"
            );
            stmt.execute("CREATE UNIQUE INDEX IF NOT EXISTS idx_clamped_fingerprint ON clamped_events(fingerprint)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_clamped_status ON clamped_events(status)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_clamped_severity ON clamped_events(severity)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_clamped_tag ON clamped_events(tag)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_clamped_first_seen ON clamped_events(first_seen DESC)");

            // Add resolution_notes column if it doesn't already exist
            java.sql.ResultSet rs = conn.getMetaData().getColumns(null, null, "clamped_events", "resolution_notes");
            if (!rs.next()) {
                stmt.execute("ALTER TABLE clamped_events ADD COLUMN resolution_notes TEXT");
            }
            rs.close();

            System.out.println("[schema] clamped_events table ready.");
        } catch (Exception e) {
            System.err.println("[schema] Failed to initialize schema: " + e.getMessage());
        }
    }
}
