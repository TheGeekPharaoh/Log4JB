package net.odyssi.log4jb.actions;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.concurrency.AppExecutorUtil;
import net.odyssi.log4jb.visitors.RemoveLoggingVisitor;
import org.jetbrains.annotations.NotNull;

public class RemoveClassLoggingAction extends AnAction {

    @Override
    public void update(@NotNull AnActionEvent e) {
        // This action is enabled if the cursor is inside a Java class.
        boolean isEnabled = isJavaClassSelected(e);
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
        final Editor editor = e.getData(CommonDataKeys.EDITOR);
        final PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);
        if (editor == null || psiFile == null) {
            return;
        }

        ReadAction.nonBlocking(() -> PsiTreeUtil.getParentOfType(psiFile.findElementAt(editor.getCaretModel().getOffset()), PsiClass.class))
                .finishOnUiThread(ModalityState.defaultModalityState(), psiClass -> {
                    // This block runs on the EDT after the analysis is complete.
                    if (psiClass != null) {
                        // Display a confirmation dialog before performing the destructive action.
                        int result = Messages.showYesNoDialog(
                                psiClass.getProject(),
                                "Are you sure you want to remove all logging from the class '" + psiClass.getName() + "'?",
                                "Confirm Logging Removal",
                                Messages.getQuestionIcon()
                        );

                        if (result == Messages.YES) {
                            WriteCommandAction.runWriteCommandAction(psiClass.getProject(), "Log4JB: Remove Class Logging", null, () ->
                                    psiClass.accept(new RemoveLoggingVisitor()));
                        }
                    }
                })
                .submit(AppExecutorUtil.getAppExecutorService());
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}