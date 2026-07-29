package net.odyssi.log4jb.visitors;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiMethod;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;

public class SystemErrReplacementVisitorTest extends LightJavaCodeInsightFixtureTestCase {

    private PsiClass setupClassWithLogger(String methodBody) {
        PsiJavaFile file = (PsiJavaFile) myFixture.configureByText("MyClass.java",
                "import org.slf4j.Logger;\n" +
                "import org.slf4j.LoggerFactory;\n" +
                "public class MyClass {\n" +
                "    private static final Logger logger = LoggerFactory.getLogger(MyClass.class);\n" +
                methodBody + "\n" +
                "}\n"
        );
        return file.getClasses()[0];
    }

    public void testReplacesStringLiteralPrintln() {
        PsiClass psiClass = setupClassWithLogger(
                "    public void doWork() {\n" +
                "        System.err.println(\"Something failed\");\n" +
                "    }"
        );

        PsiMethod method = psiClass.findMethodsByName("doWork", false)[0];

        WriteCommandAction.runWriteCommandAction(getProject(), () ->
                method.accept(new SystemErrReplacementVisitor()));

        String bodyText = method.getBody().getText();
        assertFalse("System.err.println should be removed", bodyText.contains("System.err.println"));
        assertTrue("Should contain logger.error", bodyText.contains("logger.error"));
        assertTrue("Should contain the message", bodyText.contains("Something failed"));
        // Error-level logging should NOT be guarded
        assertFalse("Should NOT be guarded for error level", bodyText.contains("isErrorEnabled"));
    }

    public void testReplacesVariablePrintln() {
        PsiClass psiClass = setupClassWithLogger(
                "    public void doWork(String errorMsg) {\n" +
                "        System.err.println(errorMsg);\n" +
                "    }"
        );

        PsiMethod method = psiClass.findMethodsByName("doWork", false)[0];

        WriteCommandAction.runWriteCommandAction(getProject(), () ->
                method.accept(new SystemErrReplacementVisitor()));

        String bodyText = method.getBody().getText();
        assertFalse("System.err.println should be removed", bodyText.contains("System.err.println"));
        assertTrue("Should use parameterized logging with {}",
                bodyText.contains("{}") && bodyText.contains("errorMsg"));
        assertTrue("Should use logger.error", bodyText.contains("logger.error"));
    }

    public void testReplacesConcatenatedPrintln() {
        PsiClass psiClass = setupClassWithLogger(
                "    public void doWork(int code) {\n" +
                "        System.err.println(\"Error code: \" + code);\n" +
                "    }"
        );

        PsiMethod method = psiClass.findMethodsByName("doWork", false)[0];

        WriteCommandAction.runWriteCommandAction(getProject(), () ->
                method.accept(new SystemErrReplacementVisitor()));

        String bodyText = method.getBody().getText();
        assertFalse("System.err.println should be removed", bodyText.contains("System.err.println"));
        assertTrue("Should convert concatenation to parameterized format",
                bodyText.contains("Error code: ") && bodyText.contains("{}"));
        assertTrue("Variable should be an argument", bodyText.contains(", code"));
    }

    public void testIncludesMethodSignatureInMessage() {
        PsiClass psiClass = setupClassWithLogger(
                "    public void handleError(String msg) {\n" +
                "        System.err.println(\"fatal\");\n" +
                "    }"
        );

        PsiMethod method = psiClass.findMethodsByName("handleError", false)[0];

        WriteCommandAction.runWriteCommandAction(getProject(), () ->
                method.accept(new SystemErrReplacementVisitor()));

        String bodyText = method.getBody().getText();
        assertTrue("Should include method signature in log message",
                bodyText.contains("handleError(String)"));
    }

    public void testDoesNotReplaceSystemOut() {
        PsiClass psiClass = setupClassWithLogger(
                "    public void doWork() {\n" +
                "        System.out.println(\"info message\");\n" +
                "    }"
        );

        PsiMethod method = psiClass.findMethodsByName("doWork", false)[0];

        WriteCommandAction.runWriteCommandAction(getProject(), () ->
                method.accept(new SystemErrReplacementVisitor()));

        String bodyText = method.getBody().getText();
        assertTrue("System.out.println should remain unchanged",
                bodyText.contains("System.out.println"));
    }
}
