package net.odyssi.log4jb.visitors;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiJavaFile;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;

public class RemoveLoggingVisitorTest extends LightJavaCodeInsightFixtureTestCase {

    public void testRemovesLoggerField() {
        PsiJavaFile file = (PsiJavaFile) myFixture.configureByText("MyClass.java",
                "import org.slf4j.Logger;\n" +
                "import org.slf4j.LoggerFactory;\n" +
                "public class MyClass {\n" +
                "    private static final Logger logger = LoggerFactory.getLogger(MyClass.class);\n" +
                "    public void doSomething() {\n" +
                "    }\n" +
                "}\n"
        );

        PsiClass psiClass = file.getClasses()[0];

        WriteCommandAction.runWriteCommandAction(getProject(), () ->
                psiClass.accept(new RemoveLoggingVisitor()));

        assertNull("Logger field should be removed", psiClass.findFieldByName("logger", false));
    }

    public void testRemovesGuardedLogStatements() {
        PsiJavaFile file = (PsiJavaFile) myFixture.configureByText("MyClass.java",
                "import org.slf4j.Logger;\n" +
                "import org.slf4j.LoggerFactory;\n" +
                "public class MyClass {\n" +
                "    private static final Logger logger = LoggerFactory.getLogger(MyClass.class);\n" +
                "    public void doSomething() {\n" +
                "        if (logger.isDebugEnabled()) {\n" +
                "            logger.debug(\"doSomething() - start\");\n" +
                "        }\n" +
                "        int x = 1;\n" +
                "        if (logger.isDebugEnabled()) {\n" +
                "            logger.debug(\"doSomething() - end\");\n" +
                "        }\n" +
                "    }\n" +
                "}\n"
        );

        PsiClass psiClass = file.getClasses()[0];

        WriteCommandAction.runWriteCommandAction(getProject(), () ->
                psiClass.accept(new RemoveLoggingVisitor()));

        String classText = psiClass.getText();
        assertFalse("Guarded log statements should be removed", classText.contains("logger.debug"));
        assertFalse("If guards should be removed", classText.contains("isDebugEnabled"));
        assertTrue("Non-logging code should remain", classText.contains("int x = 1"));
    }

    public void testRemovesUnguardedLogStatements() {
        PsiJavaFile file = (PsiJavaFile) myFixture.configureByText("MyClass.java",
                "import org.slf4j.Logger;\n" +
                "import org.slf4j.LoggerFactory;\n" +
                "public class MyClass {\n" +
                "    private static final Logger logger = LoggerFactory.getLogger(MyClass.class);\n" +
                "    public void doSomething() {\n" +
                "        logger.error(\"something failed\", new RuntimeException());\n" +
                "        int x = 1;\n" +
                "    }\n" +
                "}\n"
        );

        PsiClass psiClass = file.getClasses()[0];

        WriteCommandAction.runWriteCommandAction(getProject(), () ->
                psiClass.accept(new RemoveLoggingVisitor()));

        String classText = psiClass.getText();
        assertFalse("Unguarded log statements should be removed", classText.contains("logger.error"));
        assertTrue("Non-logging code should remain", classText.contains("int x = 1"));
    }

    public void testDoesNothingWhenNoLoggerExists() {
        PsiJavaFile file = (PsiJavaFile) myFixture.configureByText("MyClass.java",
                "public class MyClass {\n" +
                "    public void doSomething() {\n" +
                "        int x = 1;\n" +
                "    }\n" +
                "}\n"
        );

        PsiClass psiClass = file.getClasses()[0];
        String textBefore = psiClass.getText();

        WriteCommandAction.runWriteCommandAction(getProject(), () ->
                psiClass.accept(new RemoveLoggingVisitor()));

        assertEquals("Class should be unchanged when no logger exists", textBefore, psiClass.getText());
    }
}
