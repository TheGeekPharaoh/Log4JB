package net.odyssi.log4jb.visitors;

import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A PSI visitor that finds logger calls within methods and ensures the method signature
 * in the log message matches the actual signature of the enclosing method.
 */
public class ReapplyMethodLoggingVisitor extends JavaRecursiveElementVisitor {

    private static final List<String> LOG_METHODS = Arrays.asList("trace", "debug", "info", "warn", "error");
    private final PsiElementFactory factory;

    public ReapplyMethodLoggingVisitor(Project project) {
        this.factory = JavaPsiFacade.getElementFactory(project);
    }

    @Override
    public void visitMethod(PsiMethod method) {
        super.visitMethod(method);

        String correctSignature = buildMethodSignature(method);

        method.accept(new JavaRecursiveElementVisitor() {
            @Override
            public void visitMethodCallExpression(@NotNull PsiMethodCallExpression expression) {
                super.visitMethodCallExpression(expression);

                if (isLoggerCall(expression)) {
                    PsiExpression[] arguments = expression.getArgumentList().getExpressions();
                    if (arguments.length > 0 && arguments[0] instanceof PsiLiteralExpression) {
                        updateLogMessage((PsiLiteralExpression) arguments[0], correctSignature);
                    }
                }
            }
        });
    }

    /**
     * Checks if a method call is a recognized logging method invoked on the "logger" field.
     * This verifies both the method name (debug, info, etc.) and that the qualifier
     * is the "logger" identifier, preventing false positives on unrelated methods.
     */
    private boolean isLoggerCall(PsiMethodCallExpression call) {
        String methodName = call.getMethodExpression().getReferenceName();
        if (!LOG_METHODS.contains(methodName)) {
            return false;
        }

        // Verify the receiver is the "logger" field
        PsiExpression qualifier = call.getMethodExpression().getQualifierExpression();
        if (!(qualifier instanceof PsiReferenceExpression qualifierRef)) {
            return false;
        }

        // Check the qualifier resolves to a field named "logger"
        String qualifierName = qualifierRef.getReferenceName();
        if (!"logger".equals(qualifierName)) {
            return false;
        }

        // Optionally verify it resolves to a field (not a local variable with the same name)
        PsiElement resolved = qualifierRef.resolve();
        return resolved instanceof PsiField;
    }

    /**
     * Updates the log message in a PsiLiteralExpression if its signature part is incorrect.
     */
    private void updateLogMessage(PsiLiteralExpression logMessageExpr, String correctSignature) {
        Object value = logMessageExpr.getValue();
        if (!(value instanceof String)) {
            return;
        }

        String logMessage = (String) value;
        int separatorIndex = logMessage.indexOf(" - ");

        if (separatorIndex != -1) {
            String loggedSignature = logMessage.substring(0, separatorIndex);
            if (!loggedSignature.equals(correctSignature)) {
                String messageBody = logMessage.substring(separatorIndex); // e.g., " - Hello, world!"
                String newLogMessage = correctSignature + messageBody;

                PsiExpression newLogMessageExpr = factory.createExpressionFromText("\"" + newLogMessage + "\"", null);
                logMessageExpr.replace(newLogMessageExpr);
            }
        }
    }

    /**
     * Constructs the expected method signature string from a PsiMethod.
     * Example: "myMethod(String,int)"
     */
    private String buildMethodSignature(PsiMethod method) {
        String methodName = method.getName();
        String params = Arrays.stream(method.getParameterList().getParameters())
                .map(this::getParameterType)
                .collect(Collectors.joining(","));
        return String.format("%s(%s)", methodName, params);
    }

    /**
     * Gets the simple name of a parameter's type.
     */
    private String getParameterType(PsiParameter p) {
        // getPresentableText() gives the simple name, e.g., "String" instead of "java.lang.String"
        return p.getType().getPresentableText();
    }
}