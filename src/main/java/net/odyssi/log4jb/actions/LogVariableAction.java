package net.odyssi.log4jb.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.CodeStyleManager;
import org.jetbrains.annotations.NotNull;

/**
 * Generates a log statement for the selected variable
 *
 * @author sdnakhla
 */
public class LogVariableAction extends AbstractLoggingAction {

	private static final String logVariableTemplate = "if(%s.isDebugEnabled()) {\n	%s.debug(\"%s - %s %s={}\", %s);\n}\n";

	public void update(@NotNull AnActionEvent e) {
		// Check if a Java class is selected
		boolean isEnabled = isJavaLocalVariableSelected(e);

		// Enable or disable the action
		e.getPresentation().setEnabled(isEnabled);
	}

	@Override
	public void actionPerformed(AnActionEvent e) {
		Project proj = e.getProject();
		Editor editor = e.getRequiredData(CommonDataKeys.EDITOR);

		PsiClass selectedClass = getSelectedCursorClass(proj, editor);
		PsiMethod selectedMethod = getSelectedCursorMethod(proj, editor);
		PsiLocalVariable selectedVariable = getSelectedCursorLocalVariable(proj, editor);

		WriteCommandAction.runWriteCommandAction(proj, () -> {
			declareLogger(selectedClass);
			this.logVariable(selectedMethod, selectedVariable, editor);

			PsiCodeBlock body = selectedMethod.getBody();
			CodeStyleManager.getInstance(proj).reformat(body);
		});
	}

	/**
	 * Generates a log statement for the selected variable
	 * @param method The method
	 * @param selectedVariable The selected variable
	 */
	public void logVariable(PsiMethod method, PsiLocalVariable selectedVariable, Editor editor) {
		String variableName = selectedVariable.getName();
		String variableType = selectedVariable.getType().getPresentableText();
		String methodDeclaration = getMethodDeclaration(method);

		String logStatementStr = logVariableTemplate.formatted(loggerObjectName, loggerObjectName, methodDeclaration, variableType, variableName, variableName);
		System.out.println("logStatementStr=" + logStatementStr);

		PsiStatement logStatement = this.createExpressionStatement(method.getContainingClass(), logStatementStr);

		PsiFile file = method.getContainingFile();
		int caretOffset = editor.getCaretModel().getOffset();

		file.addAfter(logStatement, file.findElementAt(editor.getDocument().getLineStartOffset(editor.getDocument().getLineNumber(caretOffset) + 1)));
	}
}
