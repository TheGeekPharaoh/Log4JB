package net.odyssi.log4jb.visitors;

import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * A PSI visitor that instruments a Java method with start and end log statements.
 * <p>
 * This visitor adds guarded log statements at the debug level.
 * - A "start" log statement is added at the beginning of the method body.
 * - An "end" log statement is added before every return statement and at the end of the method body.
 * <p>
 * It avoids adding duplicate log statements if they already appear to exist.
 */
public class LogMethodVisitor extends JavaRecursiveElementVisitor {

    private final PsiMethod psiMethod;
    private final String startMessage;
    private final String endMessage;

    public LogMethodVisitor(PsiMethod psiMethod) {
        this.psiMethod = psiMethod;
        this.startMessage = buildMessage(psiMethod, "start");
        this.endMessage = buildMessage(psiMethod, "end");
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
        if (!bodyText.contains(startMessage)) {
            final var startLogStatementText = String.format("if(logger.isDebugEnabled()) { logger.debug(\"%s\"); }", startMessage);
            final var startLogStatement = factory.createStatementFromText(startLogStatementText, method);
            body.addAfter(startLogStatement, body.getLBrace());
        }

        // 2. Add "end" log statements before all return statements
        // We only need to check for the end message once, as we will add it to all returns if it's missing anywhere.
        if (!bodyText.contains(endMessage)) {
            final var returnStatements = PsiTreeUtil.findChildrenOfType(body, PsiReturnStatement.class);
            for (PsiReturnStatement returnStatement : returnStatements) {
                final var endLogStatementText = String.format("if(logger.isDebugEnabled()) { logger.debug(\"%s\"); }", endMessage);
                final var endLogStatement = factory.createStatementFromText(endLogStatementText, returnStatement);
                // Use addBefore to place the log statement just before the return
                returnStatement.getParent().addBefore(endLogStatement, returnStatement);
            }
        }

        // 3. Add "end" log statement at the very end of the method if it can "fall through"
        // (i.e., it doesn't end with a return or throw)
        final var statements = body.getStatements();
        if (statements.length == 0) {
            // Handle empty method body
            if (!body.getText().contains(endMessage)) {
                addFinalEndLog(body, factory);
            }
        } else {
            final var lastStatement = statements[statements.length - 1];
            // If the last statement is not a return, we need a final log statement.
            // Also check if the log message is already present to avoid duplicates.
            if (!(lastStatement instanceof PsiReturnStatement) && !lastStatement.getText().contains(endMessage)) {
                addFinalEndLog(body, factory);
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
     * Builds the log message string.
     * Example: "myMethod(String,int,List) - start"
     */
    private String buildMessage(PsiMethod method, String suffix) {
        final var methodName = method.getName();
        final var parameterTypes = Arrays.stream(method.getParameterList().getParameters())
                .map(PsiParameter::getType)
                .map(PsiType::getPresentableText)
                .collect(Collectors.joining(","));

        return String.format("%s(%s) - %s", methodName, parameterTypes, suffix);
    }
}