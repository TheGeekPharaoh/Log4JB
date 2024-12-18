package net.odyssi.log4jb.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeclareLoggerAction extends AbstractLoggingAction {

	private static final Logger logger = LoggerFactory.getLogger(DeclareLoggerAction.class);

	@Override
	public void update(@NotNull AnActionEvent e) {
		// Check if a Java class is selected
		if (logger.isDebugEnabled()) {
			logger.debug("update(AnActionEvent) - start");
		}
		boolean isEnabled = isJavaClassSelected(e);

		// Enable or disable the action
		e.getPresentation().setEnabled(isEnabled);
		if (logger.isDebugEnabled()) {
			logger.debug("update(AnActionEvent) - end");
		}
	}

	@Override
	public void actionPerformed(AnActionEvent e) {
		if (logger.isDebugEnabled()) {
			logger.debug("actionPerformed(AnActionEvent) - start");
		}
		Project proj = e.getProject();
		Editor editor = e.getRequiredData(CommonDataKeys.EDITOR);
		PsiClass psiClass = getSelectedCursorClass(proj, editor);

		declareLogger(psiClass);
		if (logger.isDebugEnabled()) {
			logger.debug("actionPerformed(AnActionEvent) - end");
		}
	}

}
