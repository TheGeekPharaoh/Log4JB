package net.odyssi.log4jb.visitors;

import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import net.odyssi.log4jb.logging.LoggingStrategy;
import net.odyssi.log4jb.logging.Slf4jLoggingStrategy;
import net.odyssi.log4jb.util.MethodSignatureBuilder;

/**
 * A PSI visitor that instruments a Java method with start/end and exception log statements.
 * <p>
 * This visitor adds:
 * - Guarded "start" and "end" log statements at the debug level.
 * - "warn" or "error" log statements to all catch blocks.
 * <p>
 * It avoids adding duplicate log statements if they already appear to exist.
 */
public class LogMethodVisitor extends JavaRecursiveElementVisitor {

    private final PsiMethod psiMethod;
    private final LoggingStrategy strategy;
    private final String methodSignature;
    private final String startMessage;
    private final String endMessage;

    public LogMethodVisitor(PsiMethod psiMethod) {
        this(psiMethod, new Slf4jLoggingStrategy());
    }

    public LogMethodVisitor(PsiMethod psiMethod, LoggingStrategy strategy) {
        this.psiMethod = psiMethod;
        this.strategy = strategy;
        this.methodSignature = MethodSignatureBuilder.build(psiMethod);
        this.startMessage = String.format("%s - start", this.methodSignature);
        this.endMessage = String.format("%s - end", this.methodSignature);
    }

    @Override
    public void visitMethod(PsiMethod method) {
        // Only visit the target method and do not log constructors
        if (!method.equals(psiMethod) || method.isConstructor() || method.getBody() == null) {
            return;
        }

        final var body = method.getBody();
        final var factory = JavaPsiFacade.getElementFactory(method.getProject());
        final var bodyText = body.getText();

        // 1. Add "start" log statement (check by message content, not exact statement text)
        final var startLogStatementText = strategy.getGuardedDebugStatement(startMessage, "");
        if (!bodyText.contains(startMessage)) {
            final var startLogStatement = factory.createStatementFromText(startLogStatementText, method);
            body.addAfter(startLogStatement, body.getLBrace());
        }

        // 2. Add "end" log statements before all return statements (excluding those in lambdas/anonymous classes)
        final var endLogStatementText = strategy.getGuardedDebugStatement(endMessage, "");
        final var returnStatements = PsiTreeUtil.findChildrenOfType(body, PsiReturnStatement.class);
        for (PsiReturnStatement returnStatement : returnStatements) {
            // Skip return statements that belong to nested lambdas or anonymous classes
            if (!belongsDirectlyToMethod(returnStatement, method)) {
                continue;
            }
            // Check if the previous sibling already contains the end message
            final var prevStatement = PsiTreeUtil.getPrevSiblingOfType(returnStatement, PsiStatement.class);
            if (prevStatement != null && prevStatement.getText().contains(endMessage)) {
                continue;
            }
            final var endLogStatement = factory.createStatementFromText(endLogStatementText, returnStatement);
            returnStatement.getParent().addBefore(endLogStatement, returnStatement);
        }

        // 3. Add "end" log statement at the very end of the method if it can "fall through"
        if (!bodyText.contains(endMessage)) {
            final var statements = body.getStatements();
            if (statements.length == 0) {
                addFinalEndLog(body, factory);
            } else {
                final var lastStatement = statements[statements.length - 1];
                if (!(lastStatement instanceof PsiReturnStatement) && !(lastStatement instanceof PsiThrowStatement)) {
                    addFinalEndLog(body, factory);
                }
            }
        }

        // 4. Add logging to all catch blocks.
        instrumentCatchBlocks(method, factory);
    }

    private void instrumentCatchBlocks(PsiMethod method, PsiElementFactory factory) {
        for (PsiCatchSection catchSection : PsiTreeUtil.findChildrenOfType(method, PsiCatchSection.class)) {
            final PsiCodeBlock catchBlock = catchSection.getCatchBlock();
            final PsiParameter exceptionParameter = catchSection.getParameter();

            if (catchBlock == null || exceptionParameter == null) {
                continue;
            }

            final String exceptionName = exceptionParameter.getName();
            final String logStatementText;

            // Check if the catch block is empty (contains no statements).
            if (catchBlock.getStatementCount() == 0) {
                // Empty block: log a warning that the exception is ignored.
                logStatementText = strategy.getWarnStatement(
                        this.methodSignature + " - exception ignored", exceptionName);
            } else {
                // Block has statements: log an error.
                logStatementText = strategy.getErrorStatement(
                        this.methodSignature + " - caught exception", exceptionName);
            }

            // Avoid adding a duplicate — check for the presence of an existing logger call.
            final String catchBlockText = catchBlock.getText();
            final String loggerName = strategy.getLoggerFieldName();
            if (catchBlockText.contains(loggerName + ".warn(") || catchBlockText.contains(loggerName + ".error(")) {
                continue;
            }

            final PsiStatement logStatement = factory.createStatementFromText(logStatementText, catchBlock);
            catchBlock.addAfter(logStatement, catchBlock.getLBrace());
        }
    }

    /**
     * Adds the final "end" log statement before the closing brace of a code block.
     */
    private void addFinalEndLog(PsiCodeBlock body, PsiElementFactory factory) {
        final var endLogStatementText = strategy.getGuardedDebugStatement(endMessage, "");
        final var endLogStatement = factory.createStatementFromText(endLogStatementText, body);
        body.addBefore(endLogStatement, body.getRBrace());
    }

    /**
     * Checks whether a return statement belongs directly to the given method,
     * as opposed to a nested lambda expression or anonymous class.
     */
    private boolean belongsDirectlyToMethod(PsiReturnStatement returnStatement, PsiMethod method) {
        PsiElement parent = returnStatement.getParent();
        while (parent != null) {
            if (parent == method) {
                return true;
            }
            if (parent instanceof PsiLambdaExpression || parent instanceof PsiClass) {
                return false;
            }
            parent = parent.getParent();
        }
        return false;
    }
}
