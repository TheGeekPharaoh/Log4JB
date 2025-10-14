package net.odyssi.log4jb.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.util.PsiTreeUtil;
import net.odyssi.log4jb.visitors.DeclareLoggerVisitor;
import net.odyssi.log4jb.visitors.ReapplyMethodLoggingVisitor;
import org.jetbrains.annotations.NotNull;

/**
 * An action that reapplies the correct method signature to existing log statements
 * throughout a class. This is useful after refactoring method signatures.
 */
public class ReapplyMethodLoggingAction extends AnAction {

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
        final Project project = e.getProject();
        final Editor editor = e.getData(CommonDataKeys.EDITOR);
        final PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);

        if (project == null || editor == null || psiFile == null) {
            return;
        }

        // Find the class containing the caret
        int offset = editor.getCaretModel().getOffset();
        PsiClass psiClass = PsiTreeUtil.getParentOfType(psiFile.findElementAt(offset), PsiClass.class);

        if (psiClass == null) {
            return;
        }

        // Execute the visitor in a write-safe context
        WriteCommandAction.runWriteCommandAction(project, () -> {
            // First, ensure the logger is declared.
            psiClass.accept(new DeclareLoggerVisitor(psiClass));
            psiClass.accept(new ReapplyMethodLoggingVisitor(project));
        });
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}