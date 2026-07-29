package net.odyssi.log4jb.actions;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.concurrency.AppExecutorUtil;
import net.odyssi.log4jb.util.MethodSignatureBuilder;
import net.odyssi.log4jb.visitors.DeclareLoggerVisitor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class LogSelectedVariableAction extends AnAction {

    @Override
    public void update(@NotNull AnActionEvent e) {
        // This action is enabled only if the cursor is on a variable inside a method.
        boolean isEnabled = findVariableAtCaret(e) != null;
        e.getPresentation().setEnabledAndVisible(isEnabled);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        final PsiVariable variable = findVariableAtCaret(e);
        if (variable == null) {
            return;
        }

        // Perform PSI analysis on a background thread to keep the UI responsive.
        ReadAction.nonBlocking(() -> {
                    // Find the statement containing the variable to insert the log after it.
                    final PsiStatement containingStatement = PsiTreeUtil.getParentOfType(variable, PsiStatement.class);
                    final PsiMethod containingMethod = PsiTreeUtil.getParentOfType(variable, PsiMethod.class);
                    return new LoggingContext(containingMethod, variable, containingStatement);
                })
                .finishOnUiThread(ModalityState.defaultModalityState(), context -> {
                    // This block runs on the EDT after the analysis is complete.
                    if (context.isValid()) {
                        WriteCommandAction.runWriteCommandAction(context.method.getProject(), "Log4JB: Log Variable", null, () -> {
                            final PsiClass containingClass = context.method.getContainingClass();
                            if (containingClass == null) return;

                            // 1. Ensure the logger is declared.
                            containingClass.accept(new DeclareLoggerVisitor(containingClass));

                            // 2. Build and insert the log statement.
                            final String methodSignature = MethodSignatureBuilder.build(context.method);
                            final String logStatementText = String.format(
                                    "if(logger.isDebugEnabled()) { logger.debug(\"%s - %s={}\", %s); }",
                                    methodSignature,
                                    context.variable.getName(),
                                    context.variable.getName()
                            );

                            final PsiElementFactory factory = JavaPsiFacade.getElementFactory(context.method.getProject());
                            final PsiStatement logStatement = factory.createStatementFromText(logStatementText, context.method);

                            // Add the new log statement after the statement containing the variable.
                            context.statement.getParent().addAfter(logStatement, context.statement);
                        });
                    }
                })
                .submit(AppExecutorUtil.getAppExecutorService());
    }

    @Nullable
    private PsiVariable findVariableAtCaret(AnActionEvent e) {
        final Editor editor = e.getData(CommonDataKeys.EDITOR);
        final PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);

        if (editor == null || !(psiFile instanceof PsiJavaFile)) {
            return null;
        }

        final PsiElement elementAtCaret = psiFile.findElementAt(editor.getCaretModel().getOffset());
        if (elementAtCaret == null) {
            return null;
        }

        // Check if the element is a variable declaration or a reference to a variable.
        final PsiVariable variable = PsiTreeUtil.getParentOfType(elementAtCaret, PsiVariable.class);
        if (variable != null) return variable;

        if (elementAtCaret.getParent() instanceof PsiReferenceExpression reference) {
            if (reference.resolve() instanceof PsiVariable resolvedVariable) {
                return resolvedVariable;
            }
        }
        return null;
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    /**
     * A simple record to hold the context needed for logging, gathered from the background thread.
     */
    private record LoggingContext(PsiMethod method, PsiVariable variable, PsiStatement statement) {
        public boolean isValid() {
            return method != null && variable != null && statement != null;
        }
    }
}