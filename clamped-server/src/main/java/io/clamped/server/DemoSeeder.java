package io.clamped.server;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Seeds the database with realistic sample events when DEMO_MODE=true and the table is empty.
 * Picks a random 10–20 events from the shared SeedData pool.
 *
 * Enable with: DEMO_MODE=true java -jar clamped-server.jar
 */
@Component
public class DemoSeeder implements ApplicationRunner {

    private final JdbcTemplate jdbc;
    private final Random random = new Random();

    @Value("${demo.mode:false}")
    private boolean demoMode;

    public DemoSeeder(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!demoMode) return;

        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM clamped_events", Integer.class);
        if (count != null && count > 0) return;

        System.out.println("[demo] Seeding sample data...");

        List<Object[]> pool = new ArrayList<>(List.of(SeedData.POOL));
        Collections.shuffle(pool, random);

        int target = 10 + random.nextInt(11); // 10–20
        List<Object[]> selected = pool.subList(0, Math.min(target, pool.size()));

        int inserted = 0;
        for (Object[] row : selected) {
            try {
                String interval = (String) row[16];
                String sql =
                    "INSERT INTO clamped_events " +
                    "(app_name, environment, severity, tag, message, exception_class, stacktrace, " +
                    " metadata, source_file, source_line, source_method, thread_name, host, " +
                    " status, fingerprint, occurrence_count, first_seen, timestamp) VALUES " +
                    "(?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?, ?, ?, ?, ?, ?, md5(?), ?, NOW() - INTERVAL '" + interval + "', NOW())";
                Object[] params = new Object[row.length - 1];
                System.arraycopy(row, 0, params, 0, params.length);
                jdbc.update(sql, params);
                inserted++;
            } catch (Exception e) {
                System.err.println("[demo] Failed to insert row: " + e.getMessage());
            }
        }

        System.out.println("[demo] Seeded " + inserted + " sample events.");
    }
}
