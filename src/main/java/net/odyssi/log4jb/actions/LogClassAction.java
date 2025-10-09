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

public class LogClassAction extends AnAction {

    @Override
    public void update(@NotNull AnActionEvent e) {
        // Determine if the action should be enabled by checking if the cursor is inside a Java class.
        boolean isEnabled = isJavaClassSelected(e);

        // Set the action's visibility and enabled state.
        e.getPresentation().setEnabledAndVisible(isEnabled);
    }

    private boolean isJavaClassSelected(AnActionEvent e) {
        final var editor = e.getData(CommonDataKeys.EDITOR);
        final var psiFile = e.getData(CommonDataKeys.PSI_FILE);

        if (psiFile instanceof PsiJavaFile && editor != null) {
            final var elementAt = psiFile.findElementAt(editor.getCaretModel().getOffset());
            return PsiTreeUtil.getParentOfType(elementAt, PsiClass.class) != null;
        }
        return false;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        final var psiFile = e.getData(CommonDataKeys.PSI_FILE);
        final var editor = e.getData(CommonDataKeys.EDITOR);
        if (psiFile == null || editor == null) return;

        final var offset = editor.getCaretModel().getOffset();
        final var elementAt = psiFile.findElementAt(offset);
        final var psiClass = PsiTreeUtil.getParentOfType(elementAt, PsiClass.class);

        if (psiClass != null) {
            WriteCommandAction.runWriteCommandAction(psiClass.getProject(), () -> {
                // 1. Ensure the logger is declared in the class.
                psiClass.accept(new DeclareLoggerVisitor(psiClass));

                // 2. Iterate over all non-abstract methods and instrument them.
                for (PsiMethod method : psiClass.getMethods()) {
                    if (method.getBody() != null) { // Only log methods with a body
                        method.accept(new LogMethodVisitor(method));
                    }
                }
            });
        }
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}