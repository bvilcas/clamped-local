package io.clamped;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetAddress;
import java.util.function.Consumer;

/**
 * Static entry point for the Clamped SDK.
 *
 * <pre>{@code
 * // Initialize once at startup:
 * Clamped.init(ClampedConfig.builder()
 *         .jdbcUrl("jdbc:postgresql://localhost:5432/clamped_db")
 *         .username("app_user")
 *         .password("secret")
 *         .appName("my-service")
 *         .environment("production")
 *         .build());
 *
 * // Capture exceptions:
 * try {
 *     processPayment(order);
 * } catch (PaymentException e) {
 *     Clamped.add(e, ctx -> ctx
 *             .tag("payment-flow")
 *             .severity(Severity.HIGH)
 *             .meta("orderId", order.getId()));
 * }
 *
 * // Capture warnings:
 * if (inventory.getStock() < 0) {
 *     Clamped.flag("Negative stock detected", ctx -> ctx
 *             .tag("inventory")
 *             .severity(Severity.MEDIUM)
 *             .meta("sku", item.getSku()));
 * }
 * }</pre>
 */
public final class Clamped {

    // Every thread can see new value immediately (not in CPU)
    private static volatile ClampedConfig config;
    private static volatile EventQueue queue;
    private static volatile EventFlusher flusher;
    private static volatile Thread flusherThread;
    private static volatile String cachedHostname;

    private Clamped() {
    }

    /**
     * Initializes the SDK. Must be called once before any add() or flag() calls.
     * Safe to call again to reinitialize (will shut down the existing instance
     * first). Only one thread can etner at a time, preventing collisions.
     */
    public static synchronized void init(ClampedConfig cfg) {
        if (config != null) {
            shutdown();
        }
        config = cfg;
        cachedHostname = resolveHostname();
        queue = new EventQueue(cfg.getMaxQueueSize());
        flusher = new EventFlusher(queue, cfg);
        flusherThread = new Thread(flusher, "clamped-flusher");
        flusherThread.setDaemon(true);
        flusherThread.start();

        if (cfg.isCaptureUncaughtExceptions()) {
            Thread.setDefaultUncaughtExceptionHandler((thread, throwable) ->
                Clamped.add(throwable, ctx -> ctx.tag("uncaught").severity(Severity.CRITICAL))
            );
        }

        if (cfg.isAutoCreateTables()) {
            try {
                SchemaManager.ensureDatabaseAndTablesExist(
                        cfg.getJdbcUrl(), cfg.getUsername(), cfg.getPassword());
            } catch (Exception e) {
                System.err.println("[Clamped] Setup failed: " + e.getMessage());
                e.printStackTrace(System.err);
            }
        }
    }

    /**
     * Captures an exception with default severity (MEDIUM) and no tag.
     */
    public static void add(Throwable e) {
        // Type cast to avoid ambiguity with the add(Throwable, String) overload
        add(e, (Consumer<EventContext>) null);
    }

    /**
     * Captures an exception with a tag/category and default severity (MEDIUM).
     */
    public static void add(Throwable e, String tag) {
        add(e, ctx -> ctx.tag(tag));
    }

    /**
     * Captures an exception with full context via a fluent builder lambda.
     */
    public static void add(Throwable e, Consumer<EventContext> contextBuilder) {
        if (config == null)
            return;
        // Capture caller info first... must be before any other method calls that add
        // stack frames
        CallerInfo caller = CallerInfo.capture();

        EventContext context = new EventContext();
        if (contextBuilder != null)
            contextBuilder.accept(context);

        String stackTrace = getStackTrace(e);
        String fingerprint = Fingerprinter.generate(e.getClass().getName(), stackTrace, null, context.getTag());
        String message;

        if (e.getMessage() != null) {
            message = e.getMessage();
        } else {
            message = e.getClass().getSimpleName();
        }

        ClampedEvent event = new ClampedEvent(
                config.getAppName(), config.getEnvironment(),
                context.getSeverity(), context.getTag(),
                message, e.getClass().getName(), stackTrace,
                context.getMetadata(),
                caller.sourceFile, caller.sourceLine, caller.sourceMethod,
                Thread.currentThread().getName(), cachedHostname,
                fingerprint);

        queue.offer(event);
    }

    /**
     * Captures a warning or informational event (not an exception).
     */
    public static void flag(String message, Consumer<EventContext> contextBuilder) {
        if (config == null)
            return;
        CallerInfo caller = CallerInfo.capture();

        EventContext ctx = new EventContext();
        if (contextBuilder != null)
            contextBuilder.accept(ctx);

        String fingerprint = Fingerprinter.generate(null, null, message, ctx.getTag());

        ClampedEvent event = new ClampedEvent(
                config.getAppName(), config.getEnvironment(),
                ctx.getSeverity(), ctx.getTag(),
                message, null, null,
                ctx.getMetadata(),
                caller.sourceFile, caller.sourceLine, caller.sourceMethod,
                Thread.currentThread().getName(), cachedHostname,
                fingerprint);

        queue.offer(event);
    }

    /**
     * Flushes remaining events and shuts down the background flusher.
     * Blocks up to 10 seconds waiting for the flusher thread to finish.
     */
    public static synchronized void shutdown() {
        if (flusher == null)
            return;
        flusher.stop();
        if (flusherThread != null) {
            flusherThread.interrupt();
            try {
                flusherThread.join(10_000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        // Drain any remaining events synchronously
        flusher.flush();
        config = null;
        queue = null;
        flusher = null;
        flusherThread = null;
        cachedHostname = null;
    }

    /** Returns true if Clamped.init() has been called and the SDK is active. */
    public static boolean isInitialized() {
        return config != null;
    }

    private static String getStackTrace(Throwable e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    private static String resolveHostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "unknown";
        }
    }
}
