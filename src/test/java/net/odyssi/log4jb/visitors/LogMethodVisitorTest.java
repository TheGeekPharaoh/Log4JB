package net.odyssi.log4jb.visitors;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiMethod;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;

public class LogMethodVisitorTest extends LightJavaCodeInsightFixtureTestCase {

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

    public void testAddsStartAndEndToVoidMethod() {
        PsiClass psiClass = setupClassWithLogger(
                "    public void doSomething() {\n" +
                "        int x = 1;\n" +
                "    }"
        );

        PsiMethod method = psiClass.findMethodsByName("doSomething", false)[0];

        WriteCommandAction.runWriteCommandAction(getProject(), () ->
                method.accept(new LogMethodVisitor(method)));

        String bodyText = method.getBody().getText();
        assertTrue("Should contain start message", bodyText.contains("doSomething() - start"));
        assertTrue("Should contain end message", bodyText.contains("doSomething() - end"));
    }

    public void testAddsEndBeforeReturnStatement() {
        PsiClass psiClass = setupClassWithLogger(
                "    public int getValue() {\n" +
                "        return 42;\n" +
                "    }"
        );

        PsiMethod method = psiClass.findMethodsByName("getValue", false)[0];

        WriteCommandAction.runWriteCommandAction(getProject(), () ->
                method.accept(new LogMethodVisitor(method)));

        String bodyText = method.getBody().getText();
        assertTrue("Should contain start message", bodyText.contains("getValue() - start"));
        assertTrue("Should contain end message before return", bodyText.contains("getValue() - end"));

        // The end message should appear before the return
        int endIndex = bodyText.indexOf("getValue() - end");
        int returnIndex = bodyText.indexOf("return 42");
        assertTrue("End message should appear before return statement", endIndex < returnIndex);
    }

    public void testIncludesParameterTypesInSignature() {
        PsiClass psiClass = setupClassWithLogger(
                "    public void process(String name, int count) {\n" +
                "        // work\n" +
                "    }"
        );

        PsiMethod method = psiClass.findMethodsByName("process", false)[0];

        WriteCommandAction.runWriteCommandAction(getProject(), () ->
                method.accept(new LogMethodVisitor(method)));

        String bodyText = method.getBody().getText();
        assertTrue("Should include parameter types in signature",
                bodyText.contains("process(String,int) - start"));
    }

    public void testDoesNotDuplicateOnSecondInvocation() {
        PsiClass psiClass = setupClassWithLogger(
                "    public void doSomething() {\n" +
                "        int x = 1;\n" +
                "    }"
        );

        PsiMethod method = psiClass.findMethodsByName("doSomething", false)[0];

        // Invoke twice
        WriteCommandAction.runWriteCommandAction(getProject(), () -> {
            method.accept(new LogMethodVisitor(method));
        });
        WriteCommandAction.runWriteCommandAction(getProject(), () -> {
            method.accept(new LogMethodVisitor(method));
        });

        String bodyText = method.getBody().getText();
        // Count occurrences of start message
        int startCount = countOccurrences(bodyText, "doSomething() - start");
        int endCount = countOccurrences(bodyText, "doSomething() - end");

        assertEquals("Start message should appear exactly once", 1, startCount);
        assertEquals("End message should appear exactly once", 1, endCount);
    }

    public void testDoesNotLogConstructors() {
        PsiClass psiClass = setupClassWithLogger(
                "    public MyClass() {\n" +
                "        // constructor body\n" +
                "    }"
        );

        PsiMethod constructor = psiClass.getConstructors()[0];

        WriteCommandAction.runWriteCommandAction(getProject(), () ->
                constructor.accept(new LogMethodVisitor(constructor)));

        String bodyText = constructor.getBody().getText();
        assertFalse("Should not add logging to constructors", bodyText.contains("- start"));
    }

    public void testInstrumentsCatchBlocks() {
        PsiClass psiClass = setupClassWithLogger(
                "    public void riskyMethod() {\n" +
                "        try {\n" +
                "            int x = 1;\n" +
                "        } catch (Exception e) {\n" +
                "            e.printStackTrace();\n" +
                "        }\n" +
                "    }"
        );

        PsiMethod method = psiClass.findMethodsByName("riskyMethod", false)[0];

        WriteCommandAction.runWriteCommandAction(getProject(), () ->
                method.accept(new LogMethodVisitor(method)));

        String bodyText = method.getBody().getText();
        assertTrue("Should add error logging in catch block",
                bodyText.contains("logger.error(\"riskyMethod() - caught exception\""));
    }

    public void testEmptyCatchBlockGetsWarning() {
        PsiClass psiClass = setupClassWithLogger(
                "    public void riskyMethod() {\n" +
                "        try {\n" +
                "            int x = 1;\n" +
                "        } catch (Exception e) {\n" +
                "        }\n" +
                "    }"
        );

        PsiMethod method = psiClass.findMethodsByName("riskyMethod", false)[0];

        WriteCommandAction.runWriteCommandAction(getProject(), () ->
                method.accept(new LogMethodVisitor(method)));

        String bodyText = method.getBody().getText();
        assertTrue("Empty catch block should get a warn-level message",
                bodyText.contains("logger.warn(\"riskyMethod() - exception ignored\""));
    }

    public void testHandlesMultipleReturnStatements() {
        PsiClass psiClass = setupClassWithLogger(
                "    public int compute(boolean flag) {\n" +
                "        if (flag) {\n" +
                "            return 1;\n" +
                "        }\n" +
                "        return 0;\n" +
                "    }"
        );

        PsiMethod method = psiClass.findMethodsByName("compute", false)[0];

        WriteCommandAction.runWriteCommandAction(getProject(), () ->
                method.accept(new LogMethodVisitor(method)));

        String bodyText = method.getBody().getText();
        assertTrue("Should contain start", bodyText.contains("compute(boolean) - start"));
        assertTrue("Should contain end", bodyText.contains("compute(boolean) - end"));

        // End message should appear before at least the first return
        int endIndex = bodyText.indexOf("compute(boolean) - end");
        int firstReturnIndex = bodyText.indexOf("return");
        assertTrue("End message should appear before a return statement", endIndex < firstReturnIndex);
    }

    private int countOccurrences(String text, String substring) {
        int count = 0;
        int idx = 0;
        while ((idx = text.indexOf(substring, idx)) != -1) {
            count++;
            idx += substring.length();
        }
        return count;
    }
}
