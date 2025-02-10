package net.odyssi.log4jb.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import net.odyssi.log4jb.actions.visitors.*;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

/**
 * Generates beginning and ending log statements for the selected method
 *
 * @author sdnakhla
 */
public class LogMethodAction extends AbstractLoggingAction {

	private static final String guardedLogStatementTemplate = "if(%s.isDebugEnabled()) {\n	%s.debug(\"%s\");\n}";

	private static final String logStatementStart = " - start";

	private static final String logStatementEnd = " - end";

	private static final Logger logger = LoggerFactory.getLogger(LogMethodAction.class);

	public void update(@NotNull AnActionEvent e) {
		if (logger.isDebugEnabled()) {
			logger.debug("update(AnActionEvent) - start");
		}

		boolean methodSelected = isJavaMethodSelected(e);
		boolean enabled;
		if(methodSelected) {
			Project proj = e.getProject();
			Editor editor = e.getRequiredData(CommonDataKeys.EDITOR);
			PsiFile psiFile = PsiDocumentManager.getInstance(proj).getPsiFile(editor.getDocument());

			if (psiFile instanceof PsiJavaFile) {
				PsiMethod method = getSelectedCursorMethod(proj, editor);
				enabled = !method.isConstructor();
			} else {
				enabled = false;
			}
		} else {
			enabled = false;
		}

		e.getPresentation().setEnabled(enabled);

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
		PsiClass selectedClass = getSelectedCursorClass(proj, editor);
		PsiFile psiFile = PsiDocumentManager.getInstance(proj).getPsiFile(editor.getDocument());

		if (psiFile instanceof PsiJavaFile) {
			PsiMethod method = getSelectedCursorMethod(proj, editor);
			WriteCommandAction.runWriteCommandAction(proj, () -> {
				this.addLoggingToMethod(method);
			});
		}
		if (logger.isDebugEnabled()) {
			logger.debug("actionPerformed(AnActionEvent) - end");
		}
	}

	/**
	 * Adds start and end logging statements to the specified {@link PsiMethod}
	 *
	 * @param method The method
	 */
	public void addLoggingToMethod(PsiMethod method) {
		MethodStartStatementVisitor startStatementVisitor = new MethodStartStatementVisitor(method);
		MethodEndStatementVisitor endStatementVisitor = new MethodEndStatementVisitor(method);

		method.accept(startStatementVisitor);
		method.accept(endStatementVisitor);
	}

	/**
	 * Adds import statements and logger declaration to the given class
	 * @param javaFile The java file
	 * @param psiClass The class
	 */
	public void addLoggingToClass(PsiJavaFile javaFile, PsiClass psiClass) {
		LoggerImportVisitor importVisitor = new LoggerImportVisitor((PsiJavaFile) psiClass.getContainingFile(), Arrays.asList(classImports));
		LoggerDeclarationVisitor declarationVisitor = new LoggerDeclarationVisitor(psiClass);

		psiClass.accept(declarationVisitor);
		javaFile.accept(importVisitor);
	}
}
