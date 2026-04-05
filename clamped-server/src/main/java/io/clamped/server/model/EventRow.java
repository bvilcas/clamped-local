package io.clamped.server.model;

public class EventRow {
    public long id;
    public String timestamp;
    public String appName;
    public String environment;
    public String severity;
    public String tag;
    public String message;
    public String exceptionClass;
    public String stacktrace;
    public String metadata;
    public String sourceFile;
    public Integer sourceLine;
    public String sourceMethod;
    public String threadName;
    public String host;
    public String status;
    public String fingerprint;
    public int occurrenceCount;
    public String firstSeen;
}
