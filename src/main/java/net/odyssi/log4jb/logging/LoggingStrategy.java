package net.odyssi.log4jb.logging;

/**
 * Abstraction for generating logging framework-specific code.
 * <p>
 * Implementations provide the specific syntax for declaring logger fields,
 * generating guarded/unguarded log statements, and producing error-level logging.
 * This allows the plugin to support multiple logging frameworks (SLF4J, Log4j2,
 * java.util.logging, etc.) without modifying the visitor logic.
 */
public interface LoggingStrategy {

    /**
     * Returns the fully-qualified logger field declaration text.
     * Example: "private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(MyClass.class);"
     *
     * @param className The simple name of the class
     * @return The field declaration text using fully-qualified names
     */
    String getLoggerFieldDeclaration(String className);

    /**
     * Returns the name of the logger field (e.g., "logger", "LOG").
     */
    String getLoggerFieldName();

    /**
     * Returns the fully-qualified type name of the logger (e.g., "org.slf4j.Logger").
     */
    String getLoggerTypeName();

    /**
     * Generates a guarded debug-level log statement.
     * Example: "if(logger.isDebugEnabled()) { logger.debug("msg"); }"
     *
     * @param message The log message (may contain {} placeholders)
     * @param args    Comma-separated argument expressions, or empty string if none
     * @return The complete statement text
     */
    String getGuardedDebugStatement(String message, String args);

    /**
     * Generates an unguarded error-level log statement.
     * Example: "logger.error("msg", exception);"
     *
     * @param message The log message
     * @param args    Comma-separated argument expressions, or empty string if none
     * @return The complete statement text
     */
    String getErrorStatement(String message, String args);

    /**
     * Generates a warn-level log statement.
     * Example: "logger.warn("msg", exception);"
     *
     * @param message The log message
     * @param args    Comma-separated argument expressions, or empty string if none
     * @return The complete statement text
     */
    String getWarnStatement(String message, String args);

    /**
     * Generates a log statement at the specified level, with or without a guard.
     *
     * @param level   The log level (e.g., "debug", "info", "warn", "error")
     * @param message The log message (may contain {} placeholders)
     * @param args    Comma-separated argument expressions, or empty string if none
     * @return The complete statement text
     */
    String getLogStatement(String level, String message, String args);

    /**
     * Returns whether the given log level should be guarded with an isXxxEnabled() check.
     *
     * @param level The log level (e.g., "debug", "info", "warn", "error")
     * @return true if the level should be guarded
     */
    boolean shouldGuard(String level);
}
