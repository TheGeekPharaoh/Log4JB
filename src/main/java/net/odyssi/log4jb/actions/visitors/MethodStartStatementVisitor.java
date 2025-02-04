package net.odyssi.log4jb.actions.visitors;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.CodeStyleManager;

/**
 * A {@link org.intellij.markdown.ast.visitors.Visitor} implementation that can be used to add a logging statement
 * to the beginning of the selected method
 */
// TODO Add check for existing log statements
public class MethodStartStatementVisitor extends AbstractMethodLoggingVisitor{

	public MethodStartStatementVisitor(PsiMethod method) {
		super(method);
	}

	@Override
	public void visitMethod(PsiMethod method) {
		if(method.isConstructor()) {
			return;
		}

		super.visitMethod(method);
		if (method.getBody() != null) {
			PsiStatement logStatement = buildLogStatement();
			PsiStatement[] statements = method.getBody().getStatements();
			if (statements.length > 0) {
				method.getBody().addBefore(logStatement, statements[0]);
			} else {
				method.getBody().add(logStatement);
			}
			CodeStyleManager.getInstance(method.getProject()).reformat(logStatement);
		}
	}

	/**
	 * Builds the log statement for the start of the selected method
	 *
	 * @return The log statement
	 */
	private PsiStatement buildLogStatement() {
		// TODO Generate proper log statement
		PsiStatement logStatement = JavaPsiFacade.getElementFactory(getMethod().getProject()).createStatementFromText("System.out.println(\"Method " + getMethod().getName() + " started\");", getMethod());
		return logStatement;
	}

}
