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
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

/**
 * Generates a log statement for the selected variable
 *
 * @author sdnakhla
 */
public class LogVariableAction extends AbstractLoggingAction {

	private static final String logVariableTemplate = "if(%s.isDebugEnabled()) {\n	%s.debug(\"%s - %s %s={}\", %s);\n}\n";
	private static final Logger logger = LoggerFactory.getLogger(LogVariableAction.class);

	public void update(@NotNull AnActionEvent e) {
		// Check if a Java class is selected
		if (logger.isDebugEnabled()) {
			logger.debug("update(AnActionEvent) - start");
		}
		boolean isEnabled = isJavaLocalVariableSelected(e);

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
		PsiMethod selectedMethod = getSelectedCursorMethod(proj, editor);
		PsiLocalVariable selectedVariable = getSelectedCursorLocalVariable(proj, editor);

		PsiFile psiFile = PsiDocumentManager.getInstance(proj).getPsiFile(editor.getDocument());

		if (psiFile instanceof PsiJavaFile) {
			WriteCommandAction.runWriteCommandAction(proj, () -> {
				LoggerImportVisitor importVisitor = new LoggerImportVisitor((PsiJavaFile) selectedClass.getContainingFile(), Arrays.asList(classImports));
				LoggerDeclarationVisitor declarationVisitor = new LoggerDeclarationVisitor(selectedClass);

				selectedClass.accept(declarationVisitor);
				psiFile.accept(importVisitor);

				this.logVariable(selectedMethod, selectedVariable, editor);

				PsiCodeBlock body = selectedMethod.getBody();
				CodeStyleManager.getInstance(proj).reformat(body);
			});
		}
		if (logger.isDebugEnabled()) {
			logger.debug("actionPerformed(AnActionEvent) - end");
		}
	}

	/**
	 * Generates a log statement for the selected variable
	 * @param method The method
	 * @param selectedVariable The selected variable
	 */
	public void logVariable(PsiMethod method, PsiLocalVariable selectedVariable, Editor editor) {
		if (logger.isDebugEnabled()) {
			logger.debug("logVariable(PsiMethod,PsiLocalVariable,Editor) - start");
		}
		String variableName = selectedVariable.getName();
		String variableType = selectedVariable.getType().getPresentableText();
		String methodDeclaration = getMethodDeclaration(method);

		String logStatementStr = logVariableTemplate.formatted(loggerObjectName, loggerObjectName, methodDeclaration, variableType, variableName, variableName);
		System.out.println("logStatementStr=" + logStatementStr);

		PsiStatement logStatement = this.createExpressionStatement(method.getContainingClass(), logStatementStr);

		PsiFile file = method.getContainingFile();
		int caretOffset = editor.getCaretModel().getOffset();

		file.addAfter(logStatement, file.findElementAt(editor.getDocument().getLineStartOffset(editor.getDocument().getLineNumber(caretOffset) + 1)));
		if (logger.isDebugEnabled()) {
			logger.debug("logVariable(PsiMethod,PsiLocalVariable,Editor) - end");
		}
	}
}
