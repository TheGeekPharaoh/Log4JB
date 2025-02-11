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

		// FIXME Correct log statement text
		PsiExpressionStatement printlnStatement = (PsiExpressionStatement) PsiElementFactory.SERVICE
				.getInstance(method.getProject())
				.createStatementFromText("System.out.println(\"HELLO\");", null);

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
						// Get the catch block body
						PsiCodeBlock catchBody = catchSection.getCatchBlock();

						// Check if the catch block already contains the println statement
						PsiManager psiManager = PsiManager.getInstance(method.getProject());
						boolean printlnStatementExists = Arrays.stream(catchBody.getStatements())
								.anyMatch(s -> psiManager.areElementsEquivalent(s, printlnStatement));

						// Add the println statement to the catch block body if it doesn't exist
						if (!printlnStatementExists) {
							PsiStatement[] statements = catchBody.getStatements();
							PsiExpressionStatement newPrintlnStatement = (PsiExpressionStatement) printlnStatement.copy();
							if(statements.length == 0) {
								catchBody.add(newPrintlnStatement);
							} else {
								PsiStatement lastStatement = statements[statements.length - 1];

								if (lastStatement instanceof PsiReturnStatement) {
									catchBody.addBefore(newPrintlnStatement, lastStatement);
								} else {
									catchBody.add(newPrintlnStatement);
								}
							}
						}
					}
				}
			}
		}
	}
}
