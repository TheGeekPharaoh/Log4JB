package net.odyssi.log4jb.logging;

/**
 * SLF4J implementation of {@link LoggingStrategy}.
 * <p>
 * Generates SLF4J-style logging statements using parameterized messages ({} placeholders)
 * and guarded debug/trace/info statements. Error and warn levels are not guarded.
 */
public class Slf4jLoggingStrategy implements LoggingStrategy {

    private static final String LOGGER_FIELD_NAME = "logger";
    private static final String LOGGER_TYPE = "org.slf4j.Logger";
    private static final String LOGGER_FACTORY = "org.slf4j.LoggerFactory";

    @Override
    public String getLoggerFieldDeclaration(String className) {
        return String.format(
                "private static final %s %s = %s.getLogger(%s.class);",
                LOGGER_TYPE, LOGGER_FIELD_NAME, LOGGER_FACTORY, className
        );
    }

    @Override
    public String getLoggerFieldName() {
        return LOGGER_FIELD_NAME;
    }

    @Override
    public String getLoggerTypeName() {
        return LOGGER_TYPE;
    }

    @Override
    public String getGuardedDebugStatement(String message, String args) {
        String argsStr = args.isEmpty() ? "" : ", " + args;
        return String.format(
                "if(%s.isDebugEnabled()) { %s.debug(\"%s\"%s); }",
                LOGGER_FIELD_NAME, LOGGER_FIELD_NAME, message, argsStr
        );
    }

    @Override
    public String getErrorStatement(String message, String args) {
        String argsStr = args.isEmpty() ? "" : ", " + args;
        return String.format("%s.error(\"%s\"%s);", LOGGER_FIELD_NAME, message, argsStr);
    }

    @Override
    public String getWarnStatement(String message, String args) {
        String argsStr = args.isEmpty() ? "" : ", " + args;
        return String.format("%s.warn(\"%s\"%s);", LOGGER_FIELD_NAME, message, argsStr);
    }

    @Override
    public String getLogStatement(String level, String message, String args) {
        if (shouldGuard(level)) {
            String capitalizedLevel = level.substring(0, 1).toUpperCase() + level.substring(1);
            String argsStr = args.isEmpty() ? "" : ", " + args;
            return String.format(
                    "if(%s.is%sEnabled()) {\n\t%s.%s(\"%s\"%s);\n}",
                    LOGGER_FIELD_NAME, capitalizedLevel, LOGGER_FIELD_NAME, level, message, argsStr
            );
        }
        String argsStr = args.isEmpty() ? "" : ", " + args;
        return String.format("%s.%s(\"%s\"%s);", LOGGER_FIELD_NAME, level, message, argsStr);
    }

    @Override
    public boolean shouldGuard(String level) {
        return switch (level) {
            case "debug", "trace", "info" -> true;
            case "warn", "error" -> false;
            default -> true;
        };
    }
}
