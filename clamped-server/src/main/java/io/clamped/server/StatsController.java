package io.clamped.server;

import io.clamped.server.model.StatsResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;

/**
 * Serves aggregate data for the Stats dashboard page.
 * All queries run directly against Postgres via JdbcTemplate rather than through EventRepository
 * because these are read-only aggregates that don't map to EventRow.
 */
@RestController
@RequestMapping("/api/stats")
public class StatsController {

    private final JdbcTemplate jdbc;

    public StatsController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping
    public StatsResponse stats() {
        StatsResponse response = new StatsResponse();

        // Count by severity
        response.countBySeverity = new LinkedHashMap<>();
        jdbc.query(
            "SELECT severity, COUNT(*) as cnt FROM clamped_events GROUP BY severity ORDER BY cnt DESC",
            rs -> {
                response.countBySeverity.put(rs.getString("severity"), rs.getInt("cnt"));
            });

        // Count by status
        response.countByStatus = new LinkedHashMap<>();
        jdbc.query(
            "SELECT status, COUNT(*) as cnt FROM clamped_events GROUP BY status ORDER BY cnt DESC",
            rs -> {
                response.countByStatus.put(rs.getString("status"), rs.getInt("cnt"));
            });

        // Top 10 tags
        response.topTags = new ArrayList<>();
        jdbc.query(
            "SELECT tag, COUNT(*) as cnt FROM clamped_events WHERE tag IS NOT NULL GROUP BY tag ORDER BY cnt DESC LIMIT 10",
            rs -> {
                response.topTags.add(new StatsResponse.TagCount(rs.getString("tag"), rs.getInt("cnt")));
            });

        // Top exception classes
        response.topExceptionClasses = new ArrayList<>();
        jdbc.query(
            "SELECT exception_class, COUNT(*) as cnt FROM clamped_events WHERE exception_class IS NOT NULL GROUP BY exception_class ORDER BY cnt DESC LIMIT 10",
            rs -> {
                response.topExceptionClasses.add(new StatsResponse.TagCount(rs.getString("exception_class"), rs.getInt("cnt")));
            });

        // Top source locations
        response.topSourceLocations = new ArrayList<>();
        jdbc.query(
            "SELECT CONCAT(source_file, ':', source_line) as loc, COUNT(*) as cnt FROM clamped_events WHERE source_file IS NOT NULL GROUP BY source_file, source_line ORDER BY cnt DESC LIMIT 10",
            rs -> {
                response.topSourceLocations.add(new StatsResponse.TagCount(rs.getString("loc"), rs.getInt("cnt")));
            });

        // Timeline: events per hour for last 24h
        response.timeline = new ArrayList<>();
        jdbc.query(
            "SELECT TO_CHAR(DATE_TRUNC('hour', first_seen), 'HH24:MI') as hour, COUNT(*) as cnt " +
            "FROM clamped_events WHERE first_seen > NOW() - INTERVAL '24 hours' " +
            "GROUP BY DATE_TRUNC('hour', first_seen) ORDER BY DATE_TRUNC('hour', first_seen) ASC",
            rs -> {
                response.timeline.add(new StatsResponse.TimelinePoint(rs.getString("hour"), rs.getInt("cnt")));
            });

        return response;
    }
}
