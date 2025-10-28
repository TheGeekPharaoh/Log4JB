package net.odyssi.log4jb.visitors;

import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;

import java.util.Arrays;
import java.util.stream.Collectors;

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
    private final String methodSignature;
    private final String startMessage;
    private final String endMessage;

    public LogMethodVisitor(PsiMethod psiMethod) {
        this.psiMethod = psiMethod;
        this.methodSignature = buildMethodSignature(psiMethod);
        this.startMessage = String.format("%s - start", this.methodSignature);
        this.endMessage = String.format("%s - end", this.methodSignature);
    }

    @Override
    public void visitMethod(PsiMethod method) {
        // Only visit the target method
        if (!method.equals(psiMethod) || method.getBody() == null) {
            return;
        }

        final var body = method.getBody();
        final var factory = JavaPsiFacade.getElementFactory(method.getProject());
        final var bodyText = body.getText();

        // 1. Add "start" log statement
        final var startLogStatementText = String.format("if(logger.isDebugEnabled()) { logger.debug(\"%s\"); }", startMessage);
        if (!bodyText.contains(startLogStatementText)) {
            final var startLogStatement = factory.createStatementFromText(startLogStatementText, method);
            body.addAfter(startLogStatement, body.getLBrace());
        }

        // 2. Add "end" log statements before all return statements
        final var endLogStatementText = String.format("if(logger.isDebugEnabled()) { logger.debug(\"%s\"); }", endMessage);
        final var returnStatements = PsiTreeUtil.findChildrenOfType(body, PsiReturnStatement.class);
        for (PsiReturnStatement returnStatement : returnStatements) {
            final var prevStatement = PsiTreeUtil.getPrevSiblingOfType(returnStatement, PsiStatement.class);
            if (prevStatement != null && prevStatement.getText().equals(endLogStatementText)) {
                continue;
            }
            final var endLogStatement = factory.createStatementFromText(endLogStatementText, returnStatement);
            returnStatement.getParent().addBefore(endLogStatement, returnStatement);
        }

        // 3. Add "end" log statement at the very end of the method if it can "fall through"
        final var statements = body.getStatements();
        if (statements.length == 0) {
            addFinalEndLog(body, factory);
        } else {
            final var lastStatement = statements[statements.length - 1];
            if (!(lastStatement instanceof PsiReturnStatement) && !(lastStatement instanceof PsiThrowStatement)) {
                if (!lastStatement.getText().equals(endLogStatementText)) {
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
                final String message = String.format("%s - exception ignored", this.methodSignature);
                logStatementText = String.format("logger.warn(\"%s\", %s);", message, exceptionName);
            } else {
                // Block has statements: log an error.
                logStatementText = String.format("logger.error(\"%s\", %s);", this.methodSignature, exceptionName);
            }

            // Avoid adding a duplicate log statement.
            if (!catchBlock.getText().contains(logStatementText)) {
                final PsiStatement logStatement = factory.createStatementFromText(logStatementText, catchBlock);
                // Add the log statement as the first line inside the catch block.
                catchBlock.addAfter(logStatement, catchBlock.getLBrace());
            }
        }
    }

    /**
     * Adds the final "end" log statement before the closing brace of a code block.
     */
    private void addFinalEndLog(PsiCodeBlock body, PsiElementFactory factory) {
        final var endLogStatementText = String.format("if(logger.isDebugEnabled()) { logger.debug(\"%s\"); }", endMessage);
        final var endLogStatement = factory.createStatementFromText(endLogStatementText, body);
        body.addBefore(endLogStatement, body.getRBrace());
    }

    /**
     * Builds the method signature part of the log message.
     * Example: "myMethod(String,int,List)"
     */
    private String buildMethodSignature(PsiMethod method) {
        final var methodName = method.getName();
        final var parameterTypes = Arrays.stream(method.getParameterList().getParameters())
                .map(PsiParameter::getType)
                .map(PsiType::getPresentableText)
                .collect(Collectors.joining(","));

        return String.format("%s(%s)", methodName, parameterTypes);
    }
}
