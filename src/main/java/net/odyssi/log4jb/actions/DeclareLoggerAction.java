package net.odyssi.log4jb.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import org.jetbrains.annotations.NotNull;

public class DeclareLoggerAction extends AbstractLoggingAction {

	@Override
	public void update(@NotNull AnActionEvent e) {
		// Check if a Java class is selected
		boolean isEnabled = isJavaClassSelected(e);

		// Enable or disable the action
		e.getPresentation().setEnabled(isEnabled);
	}

	@Override
	public void actionPerformed(AnActionEvent e) {
		Project proj = e.getProject();
		Editor editor = e.getRequiredData(CommonDataKeys.EDITOR);
		PsiClass psiClass = getSelectedCursorClass(proj, editor);

		declareLogger(psiClass);
	}

}
