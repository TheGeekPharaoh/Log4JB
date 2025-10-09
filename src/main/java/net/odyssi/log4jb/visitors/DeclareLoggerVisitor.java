package net.odyssi.log4jb.visitors;

import com.intellij.psi.*;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;

/**
 * A PSI visitor that adds a private static final SLF4J logger field to a Java class.
 * <p>
 * The visitor checks if a field named "logger" already exists. If not, it creates
 * and adds the following field:
 * {@code private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ClassName.class);}
 */
public class DeclareLoggerVisitor extends JavaRecursiveElementVisitor {

    private final PsiClass psiClass;

    public DeclareLoggerVisitor(PsiClass psiClass) {
        this.psiClass = psiClass;
    }

    @Override
    public void visitClass(PsiClass aClass) {
        // We only want to process the top-level class we are visiting, not inner classes.
        if (!aClass.equals(psiClass)) {
            return;
        }

        // Check if a field named 'logger' already exists.
        if (aClass.findFieldByName("logger", false) != null) {
            return;
        }

        final var project = aClass.getProject();
        final var factory = JavaPsiFacade.getElementFactory(project);
        final var codeStyleManager = JavaCodeStyleManager.getInstance(project);

        // Create the logger field from text using fully qualified names.
        final var loggerFieldText = String.format(
                "private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(%s.class);",
                aClass.getName()
        );
        final var loggerField = factory.createFieldFromText(loggerFieldText, aClass);

        // Add the field to the class
        final var newField = (PsiField) aClass.add(loggerField);
        // The `shortenClassReferences` method will automatically find the fully qualified names,
        // add the required import statements, and replace them with simple names.
        // This is the correct and idiomatic way to handle imports.
        codeStyleManager.shortenClassReferences(newField);
    }
}