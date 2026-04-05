package io.clamped.server;

import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;

@RestController
@RequestMapping("/api/seed")
public class SeedController {

    private final JdbcTemplate jdbc;
    private final Random random = new Random();

    public SeedController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @PostMapping
    public ResponseEntity<Map<String, Integer>> seed() {
        jdbc.update("TRUNCATE TABLE clamped_events RESTART IDENTITY");

        List<Object[]> pool = new ArrayList<>(List.of(SeedData.POOL));
        Collections.shuffle(pool, random);

        int count = 10 + random.nextInt(11); // 10–20
        List<Object[]> selected = pool.subList(0, Math.min(count, pool.size()));

        int inserted = 0;
        for (Object[] row : selected) {
            try {
                String interval = (String) row[16]; // last element is the interval string
                String sql =
                    "INSERT INTO clamped_events " +
                    "(app_name, environment, severity, tag, message, exception_class, stacktrace, " +
                    " metadata, source_file, source_line, source_method, thread_name, host, " +
                    " status, fingerprint, occurrence_count, first_seen, timestamp) VALUES " +
                    "(?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?, ?, ?, ?, ?, ?, md5(?), ?, NOW() - INTERVAL '" + interval + "', NOW())";
                // pass all columns except the interval (last element)
                Object[] params = new Object[row.length - 1];
                System.arraycopy(row, 0, params, 0, params.length);
                jdbc.update(sql, params);
                inserted++;
            } catch (Exception e) {
                System.err.println("[seed] Failed: " + e.getMessage());
            }
        }

        return ResponseEntity.ok(Map.of("seeded", inserted));
    }
}
