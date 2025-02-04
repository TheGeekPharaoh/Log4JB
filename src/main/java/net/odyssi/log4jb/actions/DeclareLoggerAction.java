package net.odyssi.log4jb.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import net.odyssi.log4jb.actions.visitors.LoggerDeclarationVisitor;
import net.odyssi.log4jb.actions.visitors.LoggerImportVisitor;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

public class DeclareLoggerAction extends AbstractLoggingAction {

	private static final Logger logger = LoggerFactory.getLogger(DeclareLoggerAction.class);

	private static final String loggerValueTemplate = "LoggerFactory.getLogger(%s.class);";

	public static final String[] classImports = new String[]{"org.slf4j.Logger", "org.slf4j.LoggerFactory"};

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
		Project project = e.getProject();
		Editor editor = e.getData(CommonDataKeys.EDITOR);
		PsiClass psiClass = getSelectedCursorClass(project, editor);

		if (project != null && editor != null) {
			PsiFile psiFile = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
			if (psiFile instanceof PsiJavaFile) {
				WriteCommandAction.runWriteCommandAction(project, () -> {
					PsiJavaFile javaFile = (PsiJavaFile) psiFile;
					LoggerImportVisitor importVisitor = new LoggerImportVisitor((PsiJavaFile) psiClass.getContainingFile(), Arrays.asList(classImports));
					LoggerDeclarationVisitor declarationVisitor = new LoggerDeclarationVisitor(psiClass);

					psiClass.accept(declarationVisitor);
					javaFile.accept(importVisitor);
				});
			}
		}
	}

}
