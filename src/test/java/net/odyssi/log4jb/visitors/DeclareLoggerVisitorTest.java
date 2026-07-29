package net.odyssi.log4jb.visitors;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiJavaFile;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;

public class DeclareLoggerVisitorTest extends LightJavaCodeInsightFixtureTestCase {

    public void testDeclaresLoggerFieldInEmptyClass() {
        PsiJavaFile file = (PsiJavaFile) myFixture.configureByText("MyClass.java",
                "public class MyClass {\n" +
                "}\n"
        );

        PsiClass psiClass = file.getClasses()[0];

        WriteCommandAction.runWriteCommandAction(getProject(), () ->
                psiClass.accept(new DeclareLoggerVisitor(psiClass)));

        assertNotNull("Logger field should be added", psiClass.findFieldByName("logger", false));
        String classText = psiClass.getText();
        assertTrue("Field should be private static final",
                classText.contains("private static final") && classText.contains("logger"));
        assertTrue("Field should reference LoggerFactory",
                classText.contains("LoggerFactory.getLogger(MyClass.class)"));
    }

    public void testDoesNotAddDuplicateLoggerByName() {
        PsiJavaFile file = (PsiJavaFile) myFixture.configureByText("MyClass.java",
                "import org.slf4j.Logger;\n" +
                "import org.slf4j.LoggerFactory;\n" +
                "public class MyClass {\n" +
                "    private static final Logger logger = LoggerFactory.getLogger(MyClass.class);\n" +
                "}\n"
        );

        PsiClass psiClass = file.getClasses()[0];
        int fieldCountBefore = psiClass.getFields().length;

        WriteCommandAction.runWriteCommandAction(getProject(), () ->
                psiClass.accept(new DeclareLoggerVisitor(psiClass)));

        assertEquals("No new field should be added", fieldCountBefore, psiClass.getFields().length);
    }

    public void testDoesNotAddDuplicateLoggerByType() {
        PsiJavaFile file = (PsiJavaFile) myFixture.configureByText("MyClass.java",
                "import org.slf4j.Logger;\n" +
                "import org.slf4j.LoggerFactory;\n" +
                "public class MyClass {\n" +
                "    private static final Logger LOG = LoggerFactory.getLogger(MyClass.class);\n" +
                "}\n"
        );

        PsiClass psiClass = file.getClasses()[0];
        int fieldCountBefore = psiClass.getFields().length;

        WriteCommandAction.runWriteCommandAction(getProject(), () ->
                psiClass.accept(new DeclareLoggerVisitor(psiClass)));

        assertEquals("No new field should be added when SLF4J Logger type exists with different name",
                fieldCountBefore, psiClass.getFields().length);
    }

    public void testDoesNotAffectInnerClasses() {
        PsiJavaFile file = (PsiJavaFile) myFixture.configureByText("Outer.java",
                "public class Outer {\n" +
                "    public class Inner {\n" +
                "    }\n" +
                "}\n"
        );

        PsiClass outerClass = file.getClasses()[0];

        WriteCommandAction.runWriteCommandAction(getProject(), () ->
                outerClass.accept(new DeclareLoggerVisitor(outerClass)));

        assertNotNull("Logger should be added to outer class",
                outerClass.findFieldByName("logger", false));

        PsiClass innerClass = outerClass.getInnerClasses()[0];
        assertNull("Logger should NOT be added to inner class",
                innerClass.findFieldByName("logger", false));
    }
}
