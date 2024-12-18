package net.odyssi.log4jb.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

/**
 * Generates log statements for all methods in the selected class
 *
 * @author sdnakhla
 */
public class LogClassAction extends LogMethodAction {

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
		PsiClass selectedClass = getSelectedCursorClass(proj, editor);

		List<PsiMethod> methods = Arrays.asList(selectedClass.getMethods());
		WriteCommandAction.runWriteCommandAction(proj, () -> {
			declareLogger(selectedClass);

			methods.forEach(m -> {
				this.addLoggingToMethod(selectedClass, m);
			});
		});
	}
}
