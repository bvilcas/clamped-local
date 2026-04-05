package io.clamped;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
class ClampedIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @BeforeEach
    void init() throws SQLException {
        Clamped.init(ClampedConfig.builder()
            .jdbcUrl(postgres.getJdbcUrl())
            .username(postgres.getUsername())
            .password(postgres.getPassword())
            .appName("test-app")
            .environment("test")
            .flushIntervalSeconds(1)
            .build());
        try (Connection conn = DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
             Statement stmt = conn.createStatement()) {
            stmt.execute("TRUNCATE TABLE clamped_events");
        }
    }

    @AfterEach
    void tearDown() {
        Clamped.shutdown();
    }

    @Test
    void addException_insertsRow() throws Exception {
        RuntimeException ex = new RuntimeException("something went wrong");
        Clamped.add(ex, ctx -> ctx.tag("test-tag").severity(Severity.HIGH).meta("key", "value"));
        Clamped.shutdown(); // flush immediately

        try (Connection conn = DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM clamped_events WHERE tag = 'test-tag'")) {

            assertTrue(rs.next(), "Expected at least one row");
            assertEquals("test-app", rs.getString("app_name"));
            assertEquals("test", rs.getString("environment"));
            assertEquals("HIGH", rs.getString("severity"));
            assertEquals("test-tag", rs.getString("tag"));
            assertEquals("something went wrong", rs.getString("message"));
            assertEquals("java.lang.RuntimeException", rs.getString("exception_class"));
            assertNotNull(rs.getString("stacktrace"));
            assertEquals("OPEN", rs.getString("status"));
            assertEquals(1, rs.getInt("occurrence_count"));
            assertNotNull(rs.getString("fingerprint"));
        }
    }

    @Test
    void addException_deduplicatesOnFingerprint() throws Exception {
        RuntimeException ex = new RuntimeException("dupe error");
        Clamped.add(ex);
        Clamped.add(ex);
        Clamped.shutdown();

        try (Connection conn = DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                "SELECT COUNT(*) as cnt, MAX(occurrence_count) as occ FROM clamped_events " +
                "WHERE exception_class = 'java.lang.RuntimeException'")) {

            assertTrue(rs.next());
            assertEquals(1, rs.getInt("cnt"), "Should be deduplicated into 1 row");
            assertEquals(2, rs.getInt("occ"), "occurrence_count should be 2");
        }
    }

    @Test
    void flag_insertsRowWithoutException() throws Exception {
        Clamped.flag("Negative stock detected", ctx -> ctx
            .tag("inventory")
            .severity(Severity.MEDIUM)
            .meta("sku", "ABC123"));
        Clamped.shutdown();

        try (Connection conn = DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM clamped_events WHERE tag = 'inventory'")) {

            assertTrue(rs.next());
            assertEquals("Negative stock detected", rs.getString("message"));
            assertNull(rs.getString("exception_class"));
            assertNull(rs.getString("stacktrace"));
            assertEquals("MEDIUM", rs.getString("severity"));
        }
    }

    @Test
    void addBeforeInit_doesNotThrow() {
        Clamped.shutdown(); // ensure not initialized
        assertDoesNotThrow(() -> Clamped.add(new RuntimeException("should silently no-op")));
    }

    @Test
    void fingerprintIsStable_forSameException() {
        RuntimeException ex = new RuntimeException("message varies: " + System.currentTimeMillis());
        String fp1 = Fingerprinter.generate(ex.getClass().getName(),
            stackTraceOf(ex), null, null);
        String fp2 = Fingerprinter.generate(ex.getClass().getName(),
            stackTraceOf(ex), null, null);
        assertEquals(fp1, fp2, "Fingerprint should be deterministic");
    }

    @Test
    void addException_capturesCallerInfo() throws Exception {
        Clamped.add(new RuntimeException("caller test"));
        Clamped.shutdown();

        try (Connection conn = DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                "SELECT source_file, source_line, source_method, thread_name, host " +
                "FROM clamped_events WHERE message = 'caller test'")) {

            assertTrue(rs.next());
            assertNotNull(rs.getString("source_file"), "source_file should be captured");
            assertTrue(rs.getInt("source_line") > 0,  "source_line should be captured");
            assertNotNull(rs.getString("source_method"), "source_method should be captured");
            assertNotNull(rs.getString("thread_name"), "thread_name should be captured");
            assertNotNull(rs.getString("host"),        "host should be captured");
        }
    }

    @Test
    void addException_withSimpleTag() throws Exception {
        Clamped.add(new RuntimeException("simple tag test"), "my-tag");
        Clamped.shutdown();

        try (Connection conn = DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                "SELECT tag FROM clamped_events WHERE message = 'simple tag test'")) {

            assertTrue(rs.next());
            assertEquals("my-tag", rs.getString("tag"));
        }
    }

    @Test
    void concurrentAdds_doNotThrow() throws Exception {
        int threadCount = 20;
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            pool.submit(() -> {
                try {
                    Clamped.add(new RuntimeException("concurrent"), "concurrent-tag");
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        pool.shutdown();
        Clamped.shutdown();

        try (Connection conn = DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                "SELECT occurrence_count FROM clamped_events WHERE tag = 'concurrent-tag'")) {

            assertTrue(rs.next());
            assertEquals(threadCount, rs.getInt("occurrence_count"),
                "All concurrent adds should be deduplicated into one row");
        }
    }

    @Test
    void eventStatus_hasCorrectValues() {
        EventStatus[] values = EventStatus.values();
        assertEquals(3, values.length, "Should have exactly 3 statuses: OPEN, IN_PROGRESS, RESOLVED");
        assertEquals(EventStatus.OPEN,        values[0]);
        assertEquals(EventStatus.IN_PROGRESS, values[1]);
        assertEquals(EventStatus.RESOLVED,    values[2]);
    }

    @Test
    void addException_defaultStatusIsOpen() throws Exception {
        Clamped.add(new RuntimeException("default status test"));
        Clamped.shutdown();

        try (Connection conn = DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                "SELECT status FROM clamped_events WHERE message = 'default status test'")) {

            assertTrue(rs.next());
            assertEquals("OPEN", rs.getString("status"), "New events should default to OPEN");
        }
    }

    private static String stackTraceOf(Throwable e) {
        java.io.StringWriter sw = new java.io.StringWriter();
        e.printStackTrace(new java.io.PrintWriter(sw));
        return sw.toString();
    }
}
