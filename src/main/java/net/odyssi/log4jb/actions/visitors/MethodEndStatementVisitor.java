package net.odyssi.log4jb.actions.visitors;

import com.intellij.psi.*;
import com.intellij.psi.codeStyle.CodeStyleManager;

import java.util.Arrays;

/**
 * A {@link org.intellij.markdown.ast.visitors.Visitor} implementation that adds a logging statement before each return
 * statement in a method
 */
// TODO Add check for existing log statements
public class MethodEndStatementVisitor extends AbstractMethodLoggingVisitor {

	private static final String guardedLogStatementTemplate = "if(%s.isDebugEnabled()) {\n	%s.debug(\"%s\");\n}";

	private static final String logStatementEnd = " - end";

	public static final String loggerObjectName = "logger";

	private boolean hasReturnStatement = false;

	public MethodEndStatementVisitor(PsiMethod method) {
		super(method);
	}

	@Override
	public void visitReturnStatement(PsiReturnStatement element) {
		super.visitReturnStatement(element);
		hasReturnStatement = true;

		// Check if a matching log statement already exists before the return statement
		PsiStatement newStatement = buildLogStatement();
		String newStatementText = newStatement.getText().replaceAll("\\s+", "");
		PsiElement parent = element.getParent();
		boolean logStatementExists = false;
		for (PsiElement child : parent.getChildren()) {
			if(child instanceof PsiStatement) {
				PsiStatement childStatement = (PsiStatement) child;
				String childText = childStatement.getText().replaceAll("\\s+", "");

				if(childText.equals(newStatementText) && Arrays.stream(parent.getChildren()).toList().indexOf(child) < Arrays.stream(parent.getChildren()).toList().indexOf(element)) {
					logStatementExists = true;
					break;
				}
			}
		}

		if (!logStatementExists) {
			insertStatementBefore(element);
		}
	}

	@Override
	public void visitCodeBlock(PsiCodeBlock block) {
		super.visitCodeBlock(block);
		if (!hasReturnStatement && block == getMethod().getBody()) {

			// Check if a matching log statement already exists at the end of the code block
			PsiStatement statement = buildLogStatement();
			String statementText = statement.getText().replaceAll("\\s+", "");
			PsiStatement[] statements = block.getStatements();
			boolean logStatementExists = false;
			for (PsiStatement existingStatement : statements) {
				String existingStatementText = existingStatement.getText().replaceAll("\\s+", "");
				if (existingStatementText.equals(statementText)) {
					logStatementExists = true;
					break;
				}
			}

			if (!logStatementExists) {
				insertStatementAtEnd(block);
			}
		}
	}

	private void insertStatementBefore(PsiReturnStatement element) {
		PsiStatement newStatement = buildLogStatement();
		element.getParent().addBefore(newStatement, element);
		CodeStyleManager.getInstance(getMethod().getProject()).reformat(newStatement);
	}

	private void insertStatementAtEnd(PsiCodeBlock block) {
		PsiStatement statement = buildLogStatement();
		block.add(statement);
		CodeStyleManager.getInstance(getMethod().getProject()).reformat(statement);
	}

	/**
	 * Builds the log statement for the start of the selected method
	 *
	 * @return The log statement
	 */
	private PsiStatement buildLogStatement() {
		String methodDeclaration = getMethodDeclaration(getMethod());
		String startDeclaration = methodDeclaration + logStatementEnd;
		String startMethodStatementStr = guardedLogStatementTemplate.formatted(loggerObjectName, loggerObjectName, startDeclaration);

		PsiElementFactory factory = JavaPsiFacade.getElementFactory(getMethod().getProject());
		PsiStatement statement = factory.createStatementFromText(startMethodStatementStr, null);

		return statement;
	}

}
