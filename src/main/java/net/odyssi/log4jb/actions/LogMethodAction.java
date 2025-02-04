package net.odyssi.log4jb.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.CodeStyleManager;
import net.odyssi.log4jb.actions.visitors.LoggerDeclarationVisitor;
import net.odyssi.log4jb.actions.visitors.LoggerImportVisitor;
import net.odyssi.log4jb.actions.visitors.MethodEndStatementVisitor;
import net.odyssi.log4jb.actions.visitors.MethodStartStatementVisitor;
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
		// Check if a Java class is selected
		if (logger.isDebugEnabled()) {
			logger.debug("update(AnActionEvent) - start");
		}
		boolean isEnabled = isJavaMethodSelected(e);

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
		PsiClass selectedClass = getSelectedCursorClass(proj, editor);
		PsiFile psiFile = PsiDocumentManager.getInstance(proj).getPsiFile(editor.getDocument());

		if (psiFile instanceof PsiJavaFile) {
			PsiMethod method = getSelectedCursorMethod(proj, editor);
			WriteCommandAction.runWriteCommandAction(proj, () -> {
				declareLogger(selectedClass);
				this.addLoggingToMethod((PsiJavaFile) psiFile, selectedClass, method);
			});
		}
		if (logger.isDebugEnabled()) {
			logger.debug("actionPerformed(AnActionEvent) - end");
		}
	}

	/**
	 * Adds start and end logging statements to the specified {@link PsiMethod}
	 *
	 * @param psiClass The PSI class
	 * @param method   The method
	 */
	public void addLoggingToMethod(PsiJavaFile javaFile, PsiClass psiClass, PsiMethod method) {
		MethodStartStatementVisitor startStatementVisitor = new MethodStartStatementVisitor(method);
		MethodEndStatementVisitor endStatementVisitor = new MethodEndStatementVisitor(method);
		LoggerImportVisitor importVisitor = new LoggerImportVisitor((PsiJavaFile) psiClass.getContainingFile(), Arrays.asList(classImports));
		LoggerDeclarationVisitor declarationVisitor = new LoggerDeclarationVisitor(psiClass);

		method.accept(startStatementVisitor);
		method.accept(endStatementVisitor);
		psiClass.accept(declarationVisitor);
		javaFile.accept(importVisitor);
	}
}
