package net.odyssi.log4jb.actions;

import junit.framework.TestCase;

/**
 * Unit tests for the helper methods in GenericLogAction.
 * Tests the log level mapping and variable statement building logic.
 */
public class GenericLogActionTest extends TestCase {

    private final GenericLogAction action = new GenericLogAction();

    public void testFatalMapsToError() {
        assertEquals("FATAL should map to 'error' since SLF4J has no fatal()",
                "error", action.getLogLevelOperation("FATAL"));
    }

    public void testDebugMapsToDebug() {
        assertEquals("debug", action.getLogLevelOperation("DEBUG"));
    }

    public void testInfoMapsToInfo() {
        assertEquals("info", action.getLogLevelOperation("INFO"));
    }

    public void testWarnMapsToWarn() {
        assertEquals("warn", action.getLogLevelOperation("WARN"));
    }

    public void testErrorMapsToError() {
        assertEquals("error", action.getLogLevelOperation("ERROR"));
    }

    public void testTraceMapsToTrace() {
        assertEquals("trace", action.getLogLevelOperation("TRACE"));
    }

    public void testUnknownLevelDefaultsToDebug() {
        assertEquals("Unknown level should default to debug",
                "debug", action.getLogLevelOperation("UNKNOWN"));
    }

    public void testNullLevelDefaultsToDebug() {
        assertEquals("Null level should default to debug",
                "debug", action.getLogLevelOperation(null));
    }
}
