package net.odyssi.log4jb.logging;

import junit.framework.TestCase;

public class Slf4jLoggingStrategyTest extends TestCase {

    private final Slf4jLoggingStrategy strategy = new Slf4jLoggingStrategy();

    public void testGetLoggerFieldDeclaration() {
        String declaration = strategy.getLoggerFieldDeclaration("MyService");
        assertEquals(
                "private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(MyService.class);",
                declaration
        );
    }

    public void testGetLoggerFieldName() {
        assertEquals("logger", strategy.getLoggerFieldName());
    }

    public void testGetLoggerTypeName() {
        assertEquals("org.slf4j.Logger", strategy.getLoggerTypeName());
    }

    public void testGuardedDebugStatementNoArgs() {
        String result = strategy.getGuardedDebugStatement("doWork() - start", "");
        assertEquals("if(logger.isDebugEnabled()) { logger.debug(\"doWork() - start\"); }", result);
    }

    public void testGuardedDebugStatementWithArgs() {
        String result = strategy.getGuardedDebugStatement("doWork() - name={}", "name");
        assertEquals("if(logger.isDebugEnabled()) { logger.debug(\"doWork() - name={}\", name); }", result);
    }

    public void testErrorStatementNoArgs() {
        String result = strategy.getErrorStatement("doWork() - failed", "");
        assertEquals("logger.error(\"doWork() - failed\");", result);
    }

    public void testErrorStatementWithArgs() {
        String result = strategy.getErrorStatement("doWork() - caught exception", "ex");
        assertEquals("logger.error(\"doWork() - caught exception\", ex);", result);
    }

    public void testWarnStatement() {
        String result = strategy.getWarnStatement("doWork() - exception ignored", "ex");
        assertEquals("logger.warn(\"doWork() - exception ignored\", ex);", result);
    }

    public void testShouldGuardDebug() {
        assertTrue("debug should be guarded", strategy.shouldGuard("debug"));
    }

    public void testShouldGuardTrace() {
        assertTrue("trace should be guarded", strategy.shouldGuard("trace"));
    }

    public void testShouldGuardInfo() {
        assertTrue("info should be guarded", strategy.shouldGuard("info"));
    }

    public void testShouldNotGuardWarn() {
        assertFalse("warn should NOT be guarded", strategy.shouldGuard("warn"));
    }

    public void testShouldNotGuardError() {
        assertFalse("error should NOT be guarded", strategy.shouldGuard("error"));
    }

    public void testGetLogStatementDebugIsGuarded() {
        String result = strategy.getLogStatement("debug", "msg()", "");
        assertTrue("debug should produce guarded statement", result.contains("isDebugEnabled"));
        assertTrue("Should contain logger.debug", result.contains("logger.debug"));
    }

    public void testGetLogStatementErrorIsUnguarded() {
        String result = strategy.getLogStatement("error", "msg()", "");
        assertFalse("error should NOT be guarded", result.contains("isErrorEnabled"));
        assertTrue("Should contain logger.error", result.contains("logger.error"));
    }

    public void testGetLogStatementWarnIsUnguarded() {
        String result = strategy.getLogStatement("warn", "msg()", "arg1");
        assertFalse("warn should NOT be guarded", result.contains("isWarnEnabled"));
        assertTrue("Should contain logger.warn", result.contains("logger.warn"));
        assertTrue("Should contain the argument", result.contains("arg1"));
    }
}
