package net.odyssi.log4jb.actions;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import net.odyssi.log4jb.visitors.DeclareLoggerVisitor;
import net.odyssi.log4jb.visitors.LogMethodVisitor;
import org.jetbrains.annotations.NotNull;

public class LogMethodAction extends AnAction {

    @Override
    public void update(@NotNull AnActionEvent e) {
        // Determine if the action should be enabled.
        boolean isEnabled = isLoggableMethodSelected(e);

        // Set the action's visibility and enabled state.
        e.getPresentation().setEnabledAndVisible(isEnabled);
    }

    private boolean isLoggableMethodSelected(AnActionEvent e) {
        final var editor = e.getData(CommonDataKeys.EDITOR);
        final var psiFile = e.getData(CommonDataKeys.PSI_FILE);

        if (editor != null && psiFile instanceof PsiJavaFile) {
            final var elementAt = psiFile.findElementAt(editor.getCaretModel().getOffset());
            final var method = PsiTreeUtil.getParentOfType(elementAt, PsiMethod.class);

            // The action is enabled only if the cursor is inside a method and that method is NOT a constructor.
            return method != null && !method.isConstructor();
        }
        return false;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        final var editor = e.getData(CommonDataKeys.EDITOR);
        final var psiFile = e.getData(CommonDataKeys.PSI_FILE);
        if (editor == null || psiFile == null) return;

        final var elementAt = psiFile.findElementAt(editor.getCaretModel().getOffset());
        final var psiMethod = PsiTreeUtil.getParentOfType(elementAt, PsiMethod.class);
        final var containingClass = (psiMethod != null) ? psiMethod.getContainingClass() : null;

        if (psiMethod != null && containingClass != null) {
            WriteCommandAction.runWriteCommandAction(psiMethod.getProject(), "Log4JB: Log Method", null, () -> {
                // First, ensure the logger is declared.
                containingClass.accept(new DeclareLoggerVisitor(containingClass));
                // Then, instrument the method.
                psiMethod.accept(new LogMethodVisitor(psiMethod));
            });
        }
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}