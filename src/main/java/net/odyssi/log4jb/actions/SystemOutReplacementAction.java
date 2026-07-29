package net.odyssi.log4jb.actions;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.concurrency.AppExecutorUtil;
import net.odyssi.log4jb.visitors.DeclareLoggerVisitor;
import net.odyssi.log4jb.visitors.SystemOutReplacementVisitor;
import org.jetbrains.annotations.NotNull;

public class SystemOutReplacementAction extends AnAction {

    @Override
    public void update(@NotNull AnActionEvent e) {
        // This action is enabled if the cursor is inside a Java class.
        boolean isEnabled = isJavaMethodSelected(e);
        e.getPresentation().setEnabledAndVisible(isEnabled);
    }

    private boolean isJavaMethodSelected(AnActionEvent e) {
        final var editor = e.getData(CommonDataKeys.EDITOR);
        final var psiFile = e.getData(CommonDataKeys.PSI_FILE);

        if (psiFile instanceof PsiJavaFile && editor != null) {
            final var elementAt = psiFile.findElementAt(editor.getCaretModel().getOffset());
            return PsiTreeUtil.getParentOfType(elementAt, PsiMethod.class) != null;
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

        // Perform PSI analysis on a background thread to keep the UI responsive.
        ReadAction.nonBlocking(() -> PsiTreeUtil.getParentOfType(psiFile.findElementAt(editor.getCaretModel().getOffset()), PsiMethod.class))
                .finishOnUiThread(ModalityState.defaultModalityState(), psiMethod -> {
                    // This block runs on the EDT after the analysis is complete.
                    if (psiMethod != null && psiMethod.getContainingClass() != null) {
                        WriteCommandAction.runWriteCommandAction(psiMethod.getProject(), "Log4JB: Replace System.out.println()", null, () -> {
                            // 1. Ensure the logger is declared.
                            psiMethod.getContainingClass().accept(new DeclareLoggerVisitor(psiMethod.getContainingClass()));
                            // 2. Run the replacement visitor on the current method.
                            psiMethod.accept(new SystemOutReplacementVisitor());
                        });
                    }
                })
                .submit(AppExecutorUtil.getAppExecutorService());
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}