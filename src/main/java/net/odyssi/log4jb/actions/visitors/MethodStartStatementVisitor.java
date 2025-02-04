package net.odyssi.log4jb.actions.visitors;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.CodeStyleManager;

/**
 * A {@link org.intellij.markdown.ast.visitors.Visitor} implementation that can be used to add a logging statement
 * to the beginning of the selected method
 */
public class MethodStartStatementVisitor extends AbstractMethodLoggingVisitor{

	private static final String guardedLogStatementTemplate = "if(%s.isDebugEnabled()) {\n	%s.debug(\"%s\");\n}";

	private static final String logStatementStart = " - start";

	public static final String loggerObjectName = "logger";

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
				// TODO Add check for existing log statements
				method.getBody().addBefore(logStatement, statements[0]);
			} else {
				// TODO Add check for existing log statements
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
		String methodDeclaration = getMethodDeclaration(getMethod());
		String startDeclaration = methodDeclaration + logStatementStart;
		String startMethodStatementStr = guardedLogStatementTemplate.formatted(loggerObjectName, loggerObjectName, startDeclaration);

		PsiElementFactory factory = JavaPsiFacade.getElementFactory(getMethod().getProject());
		PsiStatement statement = factory.createStatementFromText(startMethodStatementStr, null);

		return statement;
	}

}
