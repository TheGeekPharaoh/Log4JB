package net.odyssi.log4jb.visitors;

import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A PSI visitor that replaces all calls to {@code System.err.println()} with
 * SLF4J {@code logger.error()} statements.
 * <p>
 * This visitor intelligently converts string literals and concatenations into
 * parameterized SLF4J messages.
 */
public class SystemErrReplacementVisitor extends JavaRecursiveElementVisitor {

    private String currentMethodSignature = "";

    @Override
    public void visitMethod(PsiMethod method) {
        // Store the signature of the current method to use in log messages.
        this.currentMethodSignature = buildMethodSignature(method);
        super.visitMethod(method);
        this.currentMethodSignature = "";
    }

    @Override
    public void visitMethodCallExpression(PsiMethodCallExpression expression) {
        super.visitMethodCallExpression(expression);

        // Check if this is a System.err.println() call.
        if (!isSystemErrPrintln(expression)) {
            return;
        }

        final PsiExpressionList argumentList = expression.getArgumentList();
        if (argumentList.getExpressionCount() != 1) {
            return;
        }

        final PsiExpression originalArgument = argumentList.getExpressions()[0];
        final PsiElementFactory factory = JavaPsiFacade.getElementFactory(expression.getProject());

        final StringBuilder messageFormat = new StringBuilder();
        final List<String> logArguments = new ArrayList<>();

        // Prepend the method signature to the log message.
        messageFormat.append(currentMethodSignature).append(" - ");

        if (originalArgument instanceof PsiPolyadicExpression polyadicExpression) {
            // Handle string concatenations (e.g., "Error: " + code).
            for (PsiExpression operand : polyadicExpression.getOperands()) {
                if (operand instanceof PsiLiteralExpression literal && literal.getValue() instanceof String) {
                    messageFormat.append(literal.getValue());
                } else {
                    messageFormat.append("{}");
                    logArguments.add(operand.getText());
                }
            }
        } else {
            // Handle single arguments (literals, variables, etc.).
            if (originalArgument instanceof PsiLiteralExpression literal && literal.getValue() instanceof String) {
                messageFormat.append(literal.getValue());
            } else {
                messageFormat.append("{}");
                logArguments.add(originalArgument.getText());
            }
        }

        // Construct the final logger call. No guard statement is needed for the error level.
        final String argsString = logArguments.isEmpty() ? "" : ", " + String.join(", ", logArguments);
        final String newStatementText = String.format("logger.error(\"%s\"%s);", messageFormat, argsString);

        // Replace the old statement with the new one.
        final PsiStatement newStatement = factory.createStatementFromText(newStatementText, expression);
        final PsiStatement parentStatement = PsiTreeUtil.getParentOfType(expression, PsiStatement.class);
        if (parentStatement != null) {
            parentStatement.replace(newStatement);
        }
    }

    private boolean isSystemErrPrintln(PsiMethodCallExpression expression) {
        final PsiReferenceExpression methodExpression = expression.getMethodExpression();
        if (!"println".equals(methodExpression.getReferenceName())) {
            return false;
        }
        final PsiExpression qualifier = methodExpression.getQualifierExpression();
        return qualifier != null && "System.err".equals(qualifier.getText());
    }

    private String buildMethodSignature(PsiMethod method) {
        final String methodName = method.getName();
        final String parameterTypes = Arrays.stream(method.getParameterList().getParameters())
                .map(PsiParameter::getType)
                .map(PsiType::getPresentableText)
                .collect(Collectors.joining(","));
        return String.format("%s(%s)", methodName, parameterTypes);
    }
}