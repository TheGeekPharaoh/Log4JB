package net.odyssi.log4jb.actions.visitors;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.CodeStyleManager;

/**
 * A {@link org.intellij.markdown.ast.visitors.Visitor} implementation that can be used to add a logging statement
 * to the beginning of the selected method
 */
public class MethodStartStatementVisitor extends JavaRecursiveElementVisitor {

	private final PsiMethod method;

	public MethodStartStatementVisitor(PsiMethod method) {
		this.method = method;
	}

	@Override
	public void visitMethod(PsiMethod method) {
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

	private PsiStatement buildLogStatement() {
		// TODO Generate proper log statement
		PsiStatement logStatement = JavaPsiFacade.getElementFactory(method.getProject()).createStatementFromText("System.out.println(\"Method " + method.getName() + " started\");", method);
		return logStatement;
	}

}
