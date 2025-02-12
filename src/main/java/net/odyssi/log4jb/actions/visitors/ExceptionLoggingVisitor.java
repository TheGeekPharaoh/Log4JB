package net.odyssi.log4jb.actions.visitors;

import com.intellij.psi.*;

import java.util.Arrays;

public class ExceptionLoggingVisitor extends AbstractMethodLoggingVisitor {

	public ExceptionLoggingVisitor(PsiMethod method) {
		super(method);
	}

	@Override
	public void visitMethod(PsiMethod method) {
		super.visitMethod(method);

		String logStatementTemplate = "%s.%s(\"%s\", %s);";
		String methodDeclaration = getMethodDeclaration(getMethod());

		// Get the method body
		PsiCodeBlock methodBody = method.getBody();

		// Check if the method has a body
		if (methodBody != null) {
			// Iterate over all statements in the method body
			for (PsiStatement statement : methodBody.getStatements()) {
				// Check if the statement is a try-catch statement
				if (statement instanceof PsiTryStatement) {
					PsiTryStatement tryStatement = (PsiTryStatement) statement;

					// Iterate over all catch blocks
					for (PsiCatchSection catchSection : tryStatement.getCatchSections()) {
						PsiParameter exceptionParameter = catchSection.getParameter();
						String exceptionVariableName = exceptionParameter.getName();

						String errorLogStatementText = logStatementTemplate.formatted("logger", "error", methodDeclaration, exceptionVariableName);
						PsiExpressionStatement errorLogStatement = (PsiExpressionStatement) PsiElementFactory
								.getInstance(method.getProject())
								.createStatementFromText(errorLogStatementText, null);

						String warnLogStatementText = logStatementTemplate.formatted("logger", "warn", methodDeclaration, exceptionVariableName);
						PsiExpressionStatement warnLogStatement = (PsiExpressionStatement) PsiElementFactory
								.getInstance(method.getProject())
								.createStatementFromText(warnLogStatementText, null);

						// Get the catch block body
						PsiCodeBlock catchBody = catchSection.getCatchBlock();

						// Check if the catch block already contains the log statement
						PsiManager psiManager = PsiManager.getInstance(method.getProject());
						boolean printlnStatementExists = Arrays.stream(catchBody.getStatements())
								.anyMatch(s -> (areStatementsEqual(s, errorLogStatement) || areStatementsEqual(s, warnLogStatement)));

						// Add the println statement to the catch block body if it doesn't exist
						if (!printlnStatementExists) {
							PsiStatement[] statements = catchBody.getStatements();
							if(statements.length == 0) {
								PsiExpressionStatement newLogStatement = (PsiExpressionStatement) warnLogStatement.copy();
								catchBody.add(newLogStatement);
							} else {
								PsiExpressionStatement newLogStatement = (PsiExpressionStatement) errorLogStatement.copy();
								PsiStatement lastStatement = statements[statements.length - 1];

								if (lastStatement instanceof PsiReturnStatement) {
									catchBody.addBefore(newLogStatement, lastStatement);
								} else {
									catchBody.add(newLogStatement);
								}
							}
						}
					}
				}
			}
		}
	}
}
