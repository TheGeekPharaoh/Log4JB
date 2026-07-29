package net.odyssi.log4jb.visitors;

import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import net.odyssi.log4jb.logging.LoggingStrategy;
import net.odyssi.log4jb.logging.Slf4jLoggingStrategy;
import net.odyssi.log4jb.util.MethodSignatureBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * A PSI visitor that replaces all calls to {@code System.err.println()} and {@code System.err.print()}
 * with error-level log statements using the configured {@link LoggingStrategy}.
 * <p>
 * This visitor intelligently converts string literals and concatenations into
 * parameterized log messages.
 */
public class SystemErrReplacementVisitor extends JavaRecursiveElementVisitor {

    private final LoggingStrategy strategy;
    private String currentMethodSignature = "";

    public SystemErrReplacementVisitor() {
        this(new Slf4jLoggingStrategy());
    }

    public SystemErrReplacementVisitor(LoggingStrategy strategy) {
        this.strategy = strategy;
    }

    @Override
    public void visitMethod(PsiMethod method) {
        this.currentMethodSignature = MethodSignatureBuilder.build(method);
        super.visitMethod(method);
        this.currentMethodSignature = "";
    }

    @Override
    public void visitMethodCallExpression(PsiMethodCallExpression expression) {
        super.visitMethodCallExpression(expression);

        if (!isSystemErrPrintCall(expression)) {
            return;
        }

        final PsiExpressionList argumentList = expression.getArgumentList();
        final PsiElementFactory factory = JavaPsiFacade.getElementFactory(expression.getProject());

        final String newStatementText;

        if (argumentList.getExpressionCount() == 0) {
            String message = currentMethodSignature + " - (empty line)";
            newStatementText = strategy.getErrorStatement(message, "");
        } else if (argumentList.getExpressionCount() == 1) {
            final PsiExpression originalArgument = argumentList.getExpressions()[0];

            final StringBuilder messageFormat = new StringBuilder();
            final List<String> logArguments = new ArrayList<>();

            messageFormat.append(currentMethodSignature).append(" - ");

            if (originalArgument instanceof PsiPolyadicExpression polyadicExpression) {
                for (PsiExpression operand : polyadicExpression.getOperands()) {
                    if (operand instanceof PsiLiteralExpression literal && literal.getValue() instanceof String strValue) {
                        messageFormat.append(strValue);
                    } else {
                        messageFormat.append("{}");
                        logArguments.add(operand.getText());
                    }
                }
            } else {
                if (originalArgument instanceof PsiLiteralExpression literal && literal.getValue() instanceof String strValue) {
                    messageFormat.append(strValue);
                } else {
                    messageFormat.append("{}");
                    logArguments.add(originalArgument.getText());
                }
            }

            String args = String.join(", ", logArguments);
            newStatementText = strategy.getErrorStatement(messageFormat.toString(), args);
        } else {
            return;
        }

        final PsiStatement newStatement = factory.createStatementFromText(newStatementText, expression);
        final PsiStatement parentStatement = PsiTreeUtil.getParentOfType(expression, PsiStatement.class);
        if (parentStatement != null) {
            parentStatement.replace(newStatement);
        }
    }

    private boolean isSystemErrPrintCall(PsiMethodCallExpression expression) {
        final PsiReferenceExpression methodExpression = expression.getMethodExpression();
        final String methodName = methodExpression.getReferenceName();
        if (!"println".equals(methodName) && !"print".equals(methodName)) {
            return false;
        }
        final PsiExpression qualifier = methodExpression.getQualifierExpression();
        return qualifier != null && "System.err".equals(qualifier.getText());
    }
}
