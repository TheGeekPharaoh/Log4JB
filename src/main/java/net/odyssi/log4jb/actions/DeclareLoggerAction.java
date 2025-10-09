package net.odyssi.log4jb.actions;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.concurrency.AppExecutorUtil;
import net.odyssi.log4jb.visitors.DeclareLoggerVisitor;
import org.jetbrains.annotations.NotNull;

public class DeclareLoggerAction extends AnAction {

    @Override
    public void update(@NotNull AnActionEvent e) {
        // Determine if the action should be enabled by checking if the cursor is inside a Java class.
        boolean isEnabled = isJavaClassSelected(e);

        // Set the action's visibility and enabled state.
        // Using setEnabledAndVisible is preferred for context menus to reduce UI clutter.
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
        // Get the required data from the event. This is fast and happens on the EDT.
        final Editor editor = e.getData(CommonDataKeys.EDITOR);
        final PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);
        if (editor == null || psiFile == null) {
            return;
        }

        // Use ReadAction.nonBlocking to perform slow PSI analysis on a background thread.
        ReadAction.nonBlocking(() -> {
                    // Find the class at the cursor position. This is the potentially slow part.
                    int offset = editor.getCaretModel().getOffset();
                    return PsiTreeUtil.getParentOfType(psiFile.findElementAt(offset), PsiClass.class);
                })
                .finishOnUiThread(ModalityState.defaultModalityState(), psiClass -> {
                    // This block runs on the EDT after the background task is complete.
                    // It's now safe to perform the write action.
                    if (psiClass != null) {
                        WriteCommandAction.runWriteCommandAction(psiClass.getProject(), () ->
                                psiClass.accept(new DeclareLoggerVisitor(psiClass)));
                    }
                })
                .submit(AppExecutorUtil.getAppExecutorService());
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}