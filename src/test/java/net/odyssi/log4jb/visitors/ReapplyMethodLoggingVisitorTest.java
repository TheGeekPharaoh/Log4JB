package net.odyssi.log4jb.visitors;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiMethod;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;

public class ReapplyMethodLoggingVisitorTest extends LightJavaCodeInsightFixtureTestCase {

    public void testUpdatesMethodSignatureInLogMessage() {
        PsiJavaFile file = (PsiJavaFile) myFixture.configureByText("MyClass.java",
                "import org.slf4j.Logger;\n" +
                "import org.slf4j.LoggerFactory;\n" +
                "public class MyClass {\n" +
                "    private static final Logger logger = LoggerFactory.getLogger(MyClass.class);\n" +
                "    public void process(String name, int count) {\n" +
                "        if (logger.isDebugEnabled()) {\n" +
                "            logger.debug(\"oldMethod(String) - start\");\n" +
                "        }\n" +
                "        if (logger.isDebugEnabled()) {\n" +
                "            logger.debug(\"oldMethod(String) - end\");\n" +
                "        }\n" +
                "    }\n" +
                "}\n"
        );

        PsiClass psiClass = file.getClasses()[0];

        WriteCommandAction.runWriteCommandAction(getProject(), () ->
                psiClass.accept(new ReapplyMethodLoggingVisitor(getProject())));

        PsiMethod method = psiClass.findMethodsByName("process", false)[0];
        String bodyText = method.getBody().getText();

        assertTrue("Should update signature to current method name",
                bodyText.contains("process(String,int) - start"));
        assertTrue("Should update end signature too",
                bodyText.contains("process(String,int) - end"));
        assertFalse("Old signature should be gone",
                bodyText.contains("oldMethod(String)"));
    }

    public void testDoesNotModifyNonLoggerCalls() {
        PsiJavaFile file = (PsiJavaFile) myFixture.configureByText("MyClass.java",
                "import org.slf4j.Logger;\n" +
                "import org.slf4j.LoggerFactory;\n" +
                "public class MyClass {\n" +
                "    private static final Logger logger = LoggerFactory.getLogger(MyClass.class);\n" +
                "    private Debugger debugger;\n" +
                "    public void doWork() {\n" +
                "        debugger.debug(\"someOtherMethod() - important info\");\n" +
                "        if (logger.isDebugEnabled()) {\n" +
                "            logger.debug(\"oldName() - start\");\n" +
                "        }\n" +
                "    }\n" +
                "}\n" +
                "class Debugger { public void debug(String msg) {} }\n"
        );

        PsiClass psiClass = file.getClasses()[0];

        WriteCommandAction.runWriteCommandAction(getProject(), () ->
                psiClass.accept(new ReapplyMethodLoggingVisitor(getProject())));

        PsiMethod method = psiClass.findMethodsByName("doWork", false)[0];
        String bodyText = method.getBody().getText();

        assertTrue("Non-logger calls should be unchanged",
                bodyText.contains("debugger.debug(\"someOtherMethod() - important info\")"));
        assertTrue("Logger call should be updated",
                bodyText.contains("logger.debug(\"doWork() - start\")"));
    }

    public void testHandlesMethodWithNoLogStatements() {
        PsiJavaFile file = (PsiJavaFile) myFixture.configureByText("MyClass.java",
                "import org.slf4j.Logger;\n" +
                "import org.slf4j.LoggerFactory;\n" +
                "public class MyClass {\n" +
                "    private static final Logger logger = LoggerFactory.getLogger(MyClass.class);\n" +
                "    public void emptyMethod() {\n" +
                "        int x = 1;\n" +
                "    }\n" +
                "}\n"
        );

        PsiClass psiClass = file.getClasses()[0];
        PsiMethod method = psiClass.findMethodsByName("emptyMethod", false)[0];
        String textBefore = method.getText();

        WriteCommandAction.runWriteCommandAction(getProject(), () ->
                psiClass.accept(new ReapplyMethodLoggingVisitor(getProject())));

        assertEquals("Method with no log statements should be unchanged", textBefore, method.getText());
    }
}
