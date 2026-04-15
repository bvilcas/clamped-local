package io.clamped;

/**
 * Captures the source location (file, line, method) of the code that called Clamped.add() or Clamped.flag().
 * Walks up the stack and finds the first frame outside the io.clamped package.
 */
public final class CallerInfo {

    public final String sourceFile;
    public final int sourceLine;
    public final String sourceMethod;

    private CallerInfo(String sourceFile, int sourceLine, String sourceMethod) {
        this.sourceFile = sourceFile;
        this.sourceLine = sourceLine;
        this.sourceMethod = sourceMethod;
    }

    /**
     * Capture caller info from the current thread's stack trace.
     * Must be called as early as possible inside Clamped.add() / Clamped.flag()
     * to ensure the correct frame is identified.
     */
    public static CallerInfo capture() {
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        for (StackTraceElement frame : stack) {
            String className = frame.getClassName();
            // Skip only the SDK's own entry-point classes and JVM thread infrastructure.
            // Use startsWith to also catch lambda/anonymous variants (e.g. Clamped$$Lambda$...).
            // Do NOT skip subpackages like io.clamped.demo — those are user code.
            if (!className.startsWith("io.clamped.Clamped")
                    && !className.startsWith("io.clamped.CallerInfo")
                    && !className.equals("java.lang.Thread")) {
                return new CallerInfo(frame.getFileName(), frame.getLineNumber(), frame.getMethodName());
            }
        }

        // Default fall back
        return new CallerInfo(null, -1, null);
    }
}
