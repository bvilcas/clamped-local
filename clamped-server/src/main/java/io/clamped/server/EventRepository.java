package io.clamped.server;

import io.clamped.server.model.EventRow;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Repository
public class EventRepository {

    private final JdbcTemplate jdbc;

    public EventRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<EventRow> findAll(String status, String severity, String tag, String app, String since, int limit) {
        StringBuilder sql = new StringBuilder(
            "SELECT id, timestamp, app_name, environment, severity, tag, message, " +
            "exception_class, stacktrace, metadata, source_file, source_line, source_method, " +
            "thread_name, host, status, fingerprint, occurrence_count, first_seen " +
            "FROM clamped_events WHERE 1=1");

        List<Object> params = new ArrayList<>();

        if (status != null && !"all".equalsIgnoreCase(status)) {
            sql.append(" AND UPPER(status) = ?");
            params.add(status.toUpperCase());
        }
        if (severity != null) {
            sql.append(" AND UPPER(severity) = ?");
            params.add(severity.toUpperCase());
        }
        if (tag != null) {
            sql.append(" AND tag ILIKE ?");
            params.add("%" + tag + "%");
        }
        if (app != null) {
            sql.append(" AND app_name = ?");
            params.add(app);
        }
        if (since != null) {
            sql.append(" AND first_seen > NOW() - INTERVAL '").append(parseDuration(since)).append("'");
        }

        sql.append(" ORDER BY id DESC LIMIT ?");
        params.add(limit);

        return jdbc.query(sql.toString(), ROW_MAPPER, params.toArray());
    }

    public EventRow findById(long id) {
        List<EventRow> rows = jdbc.query(
            "SELECT id, timestamp, app_name, environment, severity, tag, message, " +
            "exception_class, stacktrace, metadata, source_file, source_line, source_method, " +
            "thread_name, host, status, fingerprint, occurrence_count, first_seen " +
            "FROM clamped_events WHERE id = ?",
            ROW_MAPPER, id);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public int updateStatus(long id, String status) {
        return jdbc.update("UPDATE clamped_events SET status = ? WHERE id = ?", status, id);
    }

    public int updateEvent(long id, String message, String status, String severity) {
        return jdbc.update(
            "UPDATE clamped_events SET message = ?, status = ?, severity = ? WHERE id = ?",
            message, status, severity, id);
    }

    public int deleteById(long id) {
        return jdbc.update("DELETE FROM clamped_events WHERE id = ?", id);
    }

    public int purgeResolved(int days) {
        return jdbc.update(
            "DELETE FROM clamped_events WHERE status = 'RESOLVED' AND first_seen < NOW() - (? || ' days')::INTERVAL",
            days);
    }

    public int updateStatusByTag(String tag, String status) {
        return jdbc.update("UPDATE clamped_events SET status = ? WHERE tag = ?", status, tag);
    }

    private static String parseDuration(String s) {
        s = s.trim().toLowerCase();
        if (s.endsWith("d")) return s.replace("d", " days");
        if (s.endsWith("h")) return s.replace("h", " hours");
        if (s.endsWith("m")) return s.replace("m", " minutes");
        return s;
    }

    private static final RowMapper<EventRow> ROW_MAPPER = (rs, rowNum) -> {
        EventRow e = new EventRow();
        e.id              = rs.getLong("id");
        e.timestamp       = str(rs, "timestamp");
        e.appName         = str(rs, "app_name");
        e.environment     = str(rs, "environment");
        e.severity        = str(rs, "severity");
        e.tag             = str(rs, "tag");
        e.message         = str(rs, "message");
        e.exceptionClass  = str(rs, "exception_class");
        e.stacktrace      = str(rs, "stacktrace");
        e.metadata        = str(rs, "metadata");
        e.sourceFile      = str(rs, "source_file");
        e.sourceLine      = rs.getObject("source_line") != null ? rs.getInt("source_line") : null;
        e.sourceMethod    = str(rs, "source_method");
        e.threadName      = str(rs, "thread_name");
        e.host            = str(rs, "host");
        e.status          = str(rs, "status");
        e.fingerprint     = str(rs, "fingerprint");
        e.occurrenceCount = rs.getInt("occurrence_count");
        e.firstSeen       = str(rs, "first_seen");
        return e;
    };

    private static String str(ResultSet rs, String col) throws SQLException {
        Object v = rs.getObject(col);
        return v != null ? v.toString() : null;
    }
}
