package net.odyssi.log4jb.actions.visitors;

import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiStatement;
import com.intellij.psi.codeStyle.CodeStyleManager;

/**
 * A {@link org.intellij.markdown.ast.visitors.Visitor} implementation that can be used to add a logging statement
 * to the beginning of the selected method
 */
public class MethodStartStatementVisitor extends AbstractMethodLoggingVisitor {

	public static final String loggerObjectName = "logger";
	private static final String guardedLogStatementTemplate = "if(%s.isDebugEnabled()) {\n	%s.debug(\"%s\");\n}";
	private static final String logStatementStart = " - start";

	public MethodStartStatementVisitor(PsiMethod method) {
		super(method);
	}

	@Override
	public void visitMethod(PsiMethod method) {
		if (method.isConstructor()) {
			return;
		}

		super.visitMethod(method);
		if (method.getBody() != null) {
			PsiStatement logStatement = buildLogStatement();
			String logStatementText = logStatement.getText().replaceAll("\\s+", "");
			PsiStatement[] statements = method.getBody().getStatements();

			// Check if a matching log statement already exists
			boolean logStatementExists = false;
			for (PsiStatement statement : statements) {
				String statementText = statement.getText().replaceAll("\\s+", "");
				boolean match = logStatementText.equals(statementText);
				if (match) {
					logStatementExists = true;
					break;
				}
			}

			if (!logStatementExists) {
				if (statements.length > 0) {
					method.getBody().addBefore(logStatement, statements[0]);
				} else {
					method.getBody().add(logStatement);
				}
				CodeStyleManager.getInstance(method.getProject()).reformat(logStatement);
			}
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
