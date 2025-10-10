package net.odyssi.log4jb.visitors;

import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A PSI visitor that replaces all calls to {@code System.out.println()} with
 * guarded SLF4J {@code logger.debug()} statements.
 * <p>
 * This visitor intelligently converts string literals and concatenations into
 * parameterized SLF4J messages.
 */
public class SystemOutReplacementVisitor extends JavaRecursiveElementVisitor {

    private String currentMethodSignature = "";

    @Override
    public void visitMethod(PsiMethod method) {
        // Store the signature of the current method to use in log messages.
        this.currentMethodSignature = buildMethodSignature(method);
        // Continue traversing into the method's body.
        super.visitMethod(method);
        // Clear the signature after leaving the method.
        this.currentMethodSignature = "";
    }

    @Override
    public void visitMethodCallExpression(PsiMethodCallExpression expression) {
        super.visitMethodCallExpression(expression);

        // Check if this is a System.out.println() call.
        if (!isSystemOutPrintln(expression)) {
            return;
        }

        final PsiExpressionList argumentList = expression.getArgumentList();
        if (argumentList.getExpressionCount() != 1) {
            return;
        }

        final PsiExpression originalArgument = argumentList.getExpressions()[0];
        final PsiElementFactory factory = JavaPsiFacade.getElementFactory(expression.getProject());

        // Build the new log message and arguments.
        final StringBuilder messageFormat = new StringBuilder();
        final List<String> logArguments = new ArrayList<>();

        // Prepend the method signature to the log message.
        messageFormat.append(currentMethodSignature).append(" - ");

        if (originalArgument instanceof PsiPolyadicExpression polyadicExpression) {
            // Handle string concatenations (e.g., "Hello " + name).
            for (PsiExpression operand : polyadicExpression.getOperands()) {
                if (operand instanceof PsiLiteralExpression literal) {
                    // Append string literal content directly to the format string.
                    messageFormat.append(literal.getValue());
                } else {
                    // For variables or other expressions, add a placeholder and the expression text as an argument.
                    messageFormat.append("{}");
                    logArguments.add(operand.getText());
                }
            }
        } else {
            // Handle single arguments (literals, variables, etc.).
            if (originalArgument instanceof PsiLiteralExpression literal) {
                messageFormat.append(literal.getValue());
            } else {
                messageFormat.append("{}");
                logArguments.add(originalArgument.getText());
            }
        }

        // Construct the final logger call.
        final String argsString = logArguments.isEmpty() ? "" : ", " + String.join(", ", logArguments);
        final String newStatementText = String.format("if(logger.isDebugEnabled()) { logger.debug(\"%s\"%s); }", messageFormat, argsString);

        // Replace the old statement with the new one.
        final PsiStatement newStatement = factory.createStatementFromText(newStatementText, expression);
        final PsiStatement parentStatement = PsiTreeUtil.getParentOfType(expression, PsiStatement.class);
        if (parentStatement != null) {
            parentStatement.replace(newStatement);
        }
    }

    private boolean isSystemOutPrintln(PsiMethodCallExpression expression) {
        final PsiReferenceExpression methodExpression = expression.getMethodExpression();
        if (!"println".equals(methodExpression.getReferenceName())) {
            return false;
        }
        final PsiExpression qualifier = methodExpression.getQualifierExpression();
        return qualifier != null && "System.out".equals(qualifier.getText());
    }

    private String buildMethodSignature(PsiMethod method) {
        if (method == null) {
            return "unknown()";
        }
        final String methodName = method.getName();
        final String parameterTypes = Arrays.stream(method.getParameterList().getParameters())
                .map(PsiParameter::getType)
                .map(PsiType::getPresentableText)
                .collect(Collectors.joining(","));

        return String.format("%s(%s)", methodName, parameterTypes);
    }
}