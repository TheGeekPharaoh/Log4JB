package net.odyssi.log4jb.visitors;

import com.intellij.psi.*;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import net.odyssi.log4jb.logging.LoggingStrategy;
import net.odyssi.log4jb.logging.Slf4jLoggingStrategy;

/**
 * A PSI visitor that adds a logger field to a Java class using the configured {@link LoggingStrategy}.
 * <p>
 * The visitor checks if a logger field already exists (by name or by type). If not, it creates
 * and adds the field declaration provided by the strategy.
 */
public class DeclareLoggerVisitor extends JavaRecursiveElementVisitor {

    private final PsiClass psiClass;
    private final LoggingStrategy strategy;

    public DeclareLoggerVisitor(PsiClass psiClass) {
        this(psiClass, new Slf4jLoggingStrategy());
    }

    public DeclareLoggerVisitor(PsiClass psiClass, LoggingStrategy strategy) {
        this.psiClass = psiClass;
        this.strategy = strategy;
    }

    @Override
    public void visitClass(PsiClass aClass) {
        // We only want to process the top-level class we are visiting, not inner classes.
        if (!aClass.equals(psiClass)) {
            return;
        }

        // Check if a field with the logger name already exists.
        if (aClass.findFieldByName(strategy.getLoggerFieldName(), false) != null) {
            return;
        }

        // Also check if any field is typed as the logger type to avoid duplicates
        // when the logger field has a non-standard name (e.g., LOG, LOGGER).
        final String loggerTypeName = strategy.getLoggerTypeName();
        final String simpleTypeName = loggerTypeName.contains(".")
                ? loggerTypeName.substring(loggerTypeName.lastIndexOf('.') + 1)
                : loggerTypeName;

        for (PsiField field : aClass.getFields()) {
            String canonicalText = field.getType().getCanonicalText();
            if (loggerTypeName.equals(canonicalText)) {
                return;
            }
            // When the type cannot be fully resolved (e.g., in some environments),
            // fall back to checking the simple name against the file's imports.
            if (simpleTypeName.equals(canonicalText)) {
                PsiFile containingFile = aClass.getContainingFile();
                if (containingFile instanceof PsiJavaFile javaFile) {
                    for (PsiImportStatement importStatement : javaFile.getImportList().getImportStatements()) {
                        if (loggerTypeName.equals(importStatement.getQualifiedName())) {
                            return;
                        }
                    }
                }
            }
        }

        final var project = aClass.getProject();
        final var factory = JavaPsiFacade.getElementFactory(project);
        final var codeStyleManager = JavaCodeStyleManager.getInstance(project);

        // Create the logger field from text using fully qualified names.
        final var loggerFieldText = strategy.getLoggerFieldDeclaration(aClass.getName());
        final var loggerField = factory.createFieldFromText(loggerFieldText, aClass);

        // Add the field to the class
        final var newField = (PsiField) aClass.add(loggerField);
        // The `shortenClassReferences` method will automatically find the fully qualified names,
        // add the required import statements, and replace them with simple names.
        codeStyleManager.shortenClassReferences(newField);
    }
}
