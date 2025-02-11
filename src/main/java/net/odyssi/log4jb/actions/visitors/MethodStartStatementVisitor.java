package net.odyssi.log4jb.actions.visitors;

import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiStatement;
import com.intellij.psi.codeStyle.CodeStyleManager;
import net.odyssi.log4jb.actions.LogMethodAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link org.intellij.markdown.ast.visitors.Visitor} implementation that can be used to add a logging statement
 * to the beginning of the selected method
 */
public class MethodStartStatementVisitor extends AbstractMethodLoggingVisitor {

	public static final String loggerObjectName = "logger";
	private static final String guardedLogStatementTemplate = "if(%s.isDebugEnabled()) {\n	%s.debug(\"%s\");\n}";
	private static final String logStatementStart = " - start";
	private static final Logger logger = LoggerFactory.getLogger(MethodStartStatementVisitor.class);

	public MethodStartStatementVisitor(PsiMethod method) {
		super(method);
	}

	@Override
	public void visitMethod(PsiMethod method) {
		if (logger.isDebugEnabled()) {
			logger.debug("visitMethod(PsiMethod) - start");
		}
		if (method.isConstructor()) {
			if (logger.isDebugEnabled()) {
				logger.debug("visitMethod(PsiMethod) - end");
			}
			return;
		}

		super.visitMethod(method);

		if (method.getBody() != null) {
			PsiStatement logStatement = buildLogStatement();
			PsiStatement[] statements = method.getBody().getStatements();

			// Check if a matching log statement already exists
			boolean logStatementExists = false;
			for (PsiStatement statement : statements) {
				boolean match = areStatementsEqual(statement, logStatement);
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
		if (logger.isDebugEnabled()) {
			logger.debug("buildLogStatement() - start");
		}
		String methodDeclaration = getMethodDeclaration(getMethod());
		String startDeclaration = methodDeclaration + logStatementStart;
		String startMethodStatementStr = guardedLogStatementTemplate.formatted(loggerObjectName, loggerObjectName, startDeclaration);

		PsiElementFactory factory = JavaPsiFacade.getElementFactory(getMethod().getProject());
		PsiStatement statement = factory.createStatementFromText(startMethodStatementStr, null);

		if (logger.isDebugEnabled()) {
			logger.debug("buildLogStatement() - end");
		}
		return statement;
	}

}
