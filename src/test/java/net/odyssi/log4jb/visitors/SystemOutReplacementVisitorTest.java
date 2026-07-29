package net.odyssi.log4jb.visitors;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiMethod;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;

public class SystemOutReplacementVisitorTest extends LightJavaCodeInsightFixtureTestCase {

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
                "        System.out.println(\"Hello world\");\n" +
                "    }"
        );

        PsiMethod method = psiClass.findMethodsByName("doWork", false)[0];

        WriteCommandAction.runWriteCommandAction(getProject(), () ->
                method.accept(new SystemOutReplacementVisitor()));

        String bodyText = method.getBody().getText();
        assertFalse("System.out.println should be removed", bodyText.contains("System.out.println"));
        assertTrue("Should contain logger.debug", bodyText.contains("logger.debug"));
        assertTrue("Should contain the message", bodyText.contains("Hello world"));
        assertTrue("Should be guarded", bodyText.contains("isDebugEnabled"));
    }

    public void testReplacesVariablePrintln() {
        PsiClass psiClass = setupClassWithLogger(
                "    public void doWork(String name) {\n" +
                "        System.out.println(name);\n" +
                "    }"
        );

        PsiMethod method = psiClass.findMethodsByName("doWork", false)[0];

        WriteCommandAction.runWriteCommandAction(getProject(), () ->
                method.accept(new SystemOutReplacementVisitor()));

        String bodyText = method.getBody().getText();
        assertFalse("System.out.println should be removed", bodyText.contains("System.out.println"));
        assertTrue("Should use parameterized logging with {}",
                bodyText.contains("{}") && bodyText.contains("name"));
    }

    public void testReplacesConcatenatedPrintln() {
        PsiClass psiClass = setupClassWithLogger(
                "    public void doWork(String name) {\n" +
                "        System.out.println(\"User: \" + name);\n" +
                "    }"
        );

        PsiMethod method = psiClass.findMethodsByName("doWork", false)[0];

        WriteCommandAction.runWriteCommandAction(getProject(), () ->
                method.accept(new SystemOutReplacementVisitor()));

        String bodyText = method.getBody().getText();
        assertFalse("System.out.println should be removed", bodyText.contains("System.out.println"));
        assertTrue("Should convert concatenation to parameterized format",
                bodyText.contains("User: ") && bodyText.contains("{}"));
        assertTrue("Variable should be an argument", bodyText.contains(", name"));
    }

    public void testIncludesMethodSignatureInMessage() {
        PsiClass psiClass = setupClassWithLogger(
                "    public void process(int count) {\n" +
                "        System.out.println(\"done\");\n" +
                "    }"
        );

        PsiMethod method = psiClass.findMethodsByName("process", false)[0];

        WriteCommandAction.runWriteCommandAction(getProject(), () ->
                method.accept(new SystemOutReplacementVisitor()));

        String bodyText = method.getBody().getText();
        assertTrue("Should include method signature in log message",
                bodyText.contains("process(int)"));
    }

    public void testDoesNotReplaceSystemErr() {
        PsiClass psiClass = setupClassWithLogger(
                "    public void doWork() {\n" +
                "        System.err.println(\"error message\");\n" +
                "    }"
        );

        PsiMethod method = psiClass.findMethodsByName("doWork", false)[0];

        WriteCommandAction.runWriteCommandAction(getProject(), () ->
                method.accept(new SystemOutReplacementVisitor()));

        String bodyText = method.getBody().getText();
        assertTrue("System.err.println should remain unchanged",
                bodyText.contains("System.err.println"));
    }

    public void testReplacesSystemOutPrint() {
        PsiClass psiClass = setupClassWithLogger(
                "    public void doWork() {\n" +
                "        System.out.print(\"partial\");\n" +
                "    }"
        );

        PsiMethod method = psiClass.findMethodsByName("doWork", false)[0];

        WriteCommandAction.runWriteCommandAction(getProject(), () ->
                method.accept(new SystemOutReplacementVisitor()));

        String bodyText = method.getBody().getText();
        assertFalse("System.out.print should be removed", bodyText.contains("System.out.print"));
        assertTrue("Should contain logger.debug", bodyText.contains("logger.debug"));
        assertTrue("Should contain the message", bodyText.contains("partial"));
    }

    public void testReplacesNoArgPrintln() {
        PsiClass psiClass = setupClassWithLogger(
                "    public void doWork() {\n" +
                "        System.out.println();\n" +
                "    }"
        );

        PsiMethod method = psiClass.findMethodsByName("doWork", false)[0];

        WriteCommandAction.runWriteCommandAction(getProject(), () ->
                method.accept(new SystemOutReplacementVisitor()));

        String bodyText = method.getBody().getText();
        assertFalse("System.out.println() should be removed", bodyText.contains("System.out.println"));
        assertTrue("Should contain logger.debug", bodyText.contains("logger.debug"));
        assertTrue("Should contain empty line marker", bodyText.contains("(empty line)"));
    }

    public void testReplacesNumericLiteralAsParameter() {
        PsiClass psiClass = setupClassWithLogger(
                "    public void doWork() {\n" +
                "        System.out.println(42);\n" +
                "    }"
        );

        PsiMethod method = psiClass.findMethodsByName("doWork", false)[0];

        WriteCommandAction.runWriteCommandAction(getProject(), () ->
                method.accept(new SystemOutReplacementVisitor()));

        String bodyText = method.getBody().getText();
        assertFalse("System.out.println should be removed", bodyText.contains("System.out.println"));
        assertTrue("Numeric literal should be a {} argument, not inlined",
                bodyText.contains("{}") && bodyText.contains("42"));
    }
}
