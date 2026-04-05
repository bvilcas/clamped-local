package io.clamped;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Fluent context builder passed to the lambda in Clamped.add(e, ctx -> ctx.tag("...").severity(...)).
 */
public final class EventContext {

    private String tag;
    private Severity severity = Severity.MEDIUM;
    private final Map<String, Object> metadata = new LinkedHashMap<>();

    public EventContext tag(String tag) {
        this.tag = tag;
        return this;
    }

    public EventContext severity(Severity severity) {
        this.severity = severity;
        return this;
    }

    public EventContext meta(String key, Object value) {
        this.metadata.put(key, value);
        return this;
    }

    String getTag() { return tag; }
    Severity getSeverity() { return severity; }
    Map<String, Object> getMetadata() { return Collections.unmodifiableMap(metadata); }
}
