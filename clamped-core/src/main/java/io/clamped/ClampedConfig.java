package io.clamped;

/**
 * Configuration for the Clamped SDK. Build with ClampedConfig.builder().
 *
 * <pre>{@code
 * Clamped.init(ClampedConfig.builder()
 *     .jdbcUrl("jdbc:postgresql://localhost:5432/clamped_db")
 *     .username("app_user")
 *     .password("secret")
 *     .appName("my-service")
 *     .environment("production")
 *     .build());
 * }</pre>
 */
public final class ClampedConfig {

    private final String jdbcUrl;
    private final String username;
    private final String password;
    private final String appName;
    private final String environment;
    private final boolean autoCreateTables;
    private final boolean captureUncaughtExceptions;
    private final int flushIntervalSeconds;
    private final int maxQueueSize;
    private final int batchSize;

    private ClampedConfig(Builder b) {
        this.jdbcUrl = b.jdbcUrl;
        this.username = b.username;
        this.password = b.password;
        this.appName = b.appName;
        this.environment = b.environment;
        this.autoCreateTables = b.autoCreateTables;
        this.captureUncaughtExceptions = b.captureUncaughtExceptions;
        this.flushIntervalSeconds = b.flushIntervalSeconds;
        this.maxQueueSize = b.maxQueueSize;
        this.batchSize = b.batchSize;
    }

    public static Builder builder() { return new Builder(); }

    public String getJdbcUrl() { return jdbcUrl; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getAppName() { return appName; }
    public String getEnvironment() { return environment; }
    public boolean isAutoCreateTables() { return autoCreateTables; }
    public boolean isCaptureUncaughtExceptions() { return captureUncaughtExceptions; }
    public int getFlushIntervalSeconds() { return flushIntervalSeconds; }
    public int getMaxQueueSize() { return maxQueueSize; }
    public int getBatchSize() { return batchSize; }

    public static final class Builder {
        private String jdbcUrl;
        private String username;
        private String password;
        private String appName = "default";
        private String environment = "production";
        private boolean autoCreateTables = true;
        private boolean captureUncaughtExceptions = true;
        private int flushIntervalSeconds = 5;
        private int maxQueueSize = 10_000;
        private int batchSize = 100;

        public Builder jdbcUrl(String jdbcUrl) { this.jdbcUrl = jdbcUrl; return this; }
        public Builder username(String username) { this.username = username; return this; }
        public Builder password(String password) { this.password = password; return this; }
        public Builder appName(String appName) { this.appName = appName; return this; }
        public Builder environment(String environment) { this.environment = environment; return this; }
        public Builder autoCreateTables(boolean v) { this.autoCreateTables = v; return this; }
        public Builder captureUncaughtExceptions(boolean v) { this.captureUncaughtExceptions = v; return this; }
        public Builder flushIntervalSeconds(int seconds) { this.flushIntervalSeconds = seconds; return this; }
        public Builder maxQueueSize(int size) { this.maxQueueSize = size; return this; }
        public Builder batchSize(int size) { this.batchSize = size; return this; }

        public ClampedConfig build() {
            if (jdbcUrl == null || jdbcUrl.isBlank()) {
                throw new IllegalStateException("jdbcUrl is required");
            }
            if (appName == null || appName.isBlank()) {
                throw new IllegalStateException("appName is required");
            }
            return new ClampedConfig(this);
        }
    }
}
