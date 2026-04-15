package io.clamped.server;

import io.clamped.server.model.EventRow;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import java.util.List;

/**
 * REST boundary between the Vue dashboard and Postgres.
 * All event mutations from the UI pass through here before reaching EventRepository.
 *
 * Endpoints: GET /api/events, GET/PUT/DELETE /api/events/{id},
 * POST /api/events/{id}/resolve|ack|revert, POST /api/events/bulk/{action},
 * DELETE /api/events/purge
 */
@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventRepository repo;

    public EventController(EventRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public List<EventRow> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) String app,
            @RequestParam(required = false) String since,
            @RequestParam(defaultValue = "50") int limit) {
        return repo.findAll(status, severity, tag, app, since, limit);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventRow> get(@PathVariable long id) {
        EventRow row = repo.findById(id);
        return row != null ? ResponseEntity.ok(row) : ResponseEntity.notFound().build();
    }

    // Body is optional so the UI can resolve with or without a resolution note
    @PostMapping("/{id}/resolve")
    public ResponseEntity<Void> resolve(
            @PathVariable long id,
            @RequestBody(required = false) Map<String, String> body) {
        String notes = (body != null) ? body.get("notes") : null;
        int updated = repo.resolve(id, notes);
        return updated > 0 ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    @PostMapping("/{id}/ack")
    public ResponseEntity<Void> ack(@PathVariable long id) {
        return updateStatus(id, "IN_PROGRESS");
    }

@PostMapping("/{id}/revert")
    public ResponseEntity<Void> revert(@PathVariable long id) {
        return updateStatus(id, "OPEN");
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> update(@PathVariable long id, @RequestBody Map<String, String> body) {
        int updated = repo.updateEvent(id, body.get("message"), body.get("status"), body.get("severity"));
        return updated > 0 ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable long id) {
        int updated = repo.deleteById(id);
        return updated > 0 ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    @DeleteMapping("/purge")
    public ResponseEntity<Map<String, Integer>> purge(
            @RequestParam(defaultValue = "30") int days) {
        int deleted = repo.purgeResolved(days);
        return ResponseEntity.ok(Map.of("deleted", deleted));
    }

    @PostMapping("/bulk/{action}")
    public ResponseEntity<Map<String, Integer>> bulkByTag(
            @PathVariable String action,
            @RequestParam String tag) {
        String status;
        switch (action) {
            case "resolve": status = "RESOLVED";      break;
            case "ack":     status = "IN_PROGRESS";    break;
            case "revert":  status = "OPEN";          break;
            default: throw new IllegalArgumentException("Unknown action: " + action);
        }
        int updated = repo.updateStatusByTag(tag, status);
        return ResponseEntity.ok(Map.of("updated", updated));
    }

    private ResponseEntity<Void> updateStatus(long id, String status) {
        int updated = repo.updateStatus(id, status);
        return updated > 0 ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }
}
