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
 * Generates beginning and ending log statements for the selected method
 *
 * @author sdnakhla
 */
public class LogMethodAction extends AbstractLoggingAction {

	private static final String guardedLogStatementTemplate = "if(%s.isDebugEnabled()) {\n	%s.debug(\"%s\");\n}";

	private static final String logStatementStart = " - start";

	private static final String logStatementEnd = " - end";

	public void update(@NotNull AnActionEvent e) {
		// Check if a Java class is selected
		boolean isEnabled = isJavaMethodSelected(e);

		// Enable or disable the action
		e.getPresentation().setEnabled(isEnabled);
	}

	@Override
	public void actionPerformed(AnActionEvent e) {
		Project proj = e.getProject();
		Editor editor = e.getRequiredData(CommonDataKeys.EDITOR);
		PsiClass selectedClass = getSelectedCursorClass(proj, editor);

		PsiMethod method = getSelectedCursorMethod(proj, editor);
		WriteCommandAction.runWriteCommandAction(proj, () -> {
			declareLogger(selectedClass);
			this.addLoggingToMethod(selectedClass, method);
		});
	}

	/**
	 * Adds start and end logging statements to the specified {@link PsiMethod}
	 *
	 * @param psiClass The PSI class
	 * @param method   The method
	 */
	public void addLoggingToMethod(PsiClass psiClass, PsiMethod method) {
		Project proj = psiClass.getProject();
		PsiCodeBlock body = method.getBody();

		PsiStatement startStatement = this.createMethodStartLoggingStatement(proj, method);
		if (hasExistingStatement(body, startStatement, true)) {
			System.out.println("START ALREADY EXISTS");
		} else {
			addStatement(method, startStatement, true);
		}

		PsiStatement endStatement = this.createMethodEndLoggingStatement(proj, method);
		if (hasExistingStatement(body, endStatement, false)) {
			System.out.println("END ALREADY EXISTS");
		} else {
			addStatement(method, endStatement, false);
		}

		CodeStyleManager.getInstance(method.getProject()).reformat(body);
	}

	/**
	 * Adds the given {@link PsiStatement} to a {@link PsiMethod}
	 *
	 * @param method    The method
	 * @param statement The statement
	 * @param prepend   True, if the statement should be at the start of the method.  False, otherwise.
	 */
	private void addStatement(PsiMethod method, PsiStatement statement, boolean prepend) {
		PsiCodeBlock body = method.getBody();
		if (body != null) {
			PsiDocumentManager.getInstance(method.getProject()).commitAllDocuments();
			if (prepend) {
				PsiStatement[] statements = body.getStatements();
				if (statements.length > 0) {
					body.addBefore(statement, statements[0]);
				} else {
					body.add(statement);
				}
			} else {
				body.add(statement);
			}
		}
	}

	/**
	 * Creates the logging statement applied to the start of a {@link PsiMethod}
	 *
	 * @param project The project
	 * @param method  The method
	 * @return The log statement
	 */
	private PsiStatement createMethodStartLoggingStatement(Project project, PsiMethod method) {
		String methodDeclaration = getMethodDeclaration(method);
		String startDeclaration = methodDeclaration + logStatementStart;
		String startMethodStatementStr = guardedLogStatementTemplate.formatted(loggerObjectName, loggerObjectName, startDeclaration);
		System.out.println("startMethodStatementStr=" + startMethodStatementStr);

		PsiStatement methodStartStatement = this.createExpressionStatement(method.getContainingClass(), startMethodStatementStr);

		return methodStartStatement;
	}

	/**
	 * Creates the logging statement applied to the end a {@link PsiMethod}
	 *
	 * @param project The project
	 * @param method  The method
	 * @return The log statement
	 */
	private PsiStatement createMethodEndLoggingStatement(Project project, PsiMethod method) {
		String methodDeclaration = getMethodDeclaration(method);
		String endDeclaration = methodDeclaration + logStatementEnd;
		String endMethodStatementStr = guardedLogStatementTemplate.formatted(loggerObjectName, loggerObjectName, endDeclaration);
		System.out.println("endMethodStatementStr=" + endMethodStatementStr);

		PsiStatement methodEndStatement = this.createExpressionStatement(method.getContainingClass(), endMethodStatementStr);

		return methodEndStatement;
	}

	/**
	 * Returns true if the given statement is already declared within the method
	 *
	 * @return The status
	 */
	private boolean hasExistingStatement(PsiCodeBlock body, PsiStatement statement, boolean prepend) {
		System.out.println("CHECKING FOR STATEMENT");
		String statementText = statement.getText().replaceAll("\\s+", "");
		System.out.println("statementText=" + statementText);
		PsiStatement[] statements = body.getStatements();
		if (prepend) {
			// Check if the statement exists at the beginning
			String evalStatementText = statements[0].getText().replaceAll("\\s+", "");
			System.out.println("evalStatementText=" + evalStatementText);
			boolean equal = evalStatementText.equals(statementText);
			System.out.println("equal=" + equal);
			if (statements.length > 0 && statements[0].getText().replaceAll("\\s+", "").equals(statement.getText().replaceAll("\\s+", ""))) {
				return true;
			}
		} else {
			// Check if the statement exists at the end
			if (statements.length > 0 && statements[statements.length - 1].getText().replaceAll("\\s+", "").equals(statement.getText().replaceAll("\\s+", ""))) {
				return true;
			}
		}
		// If the statement is not found at the beginning or end, check the entire code block
		for (PsiStatement existingStatement : statements) {
			if (existingStatement.getText().equals(statement.getText())) {
				return true;
			}
		}
		return false;
	}

}
