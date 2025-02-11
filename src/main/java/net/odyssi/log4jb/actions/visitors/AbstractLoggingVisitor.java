package net.odyssi.log4jb.actions.visitors;

import com.intellij.psi.*;
import net.odyssi.log4jb.actions.LogMethodAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class AbstractLoggingVisitor extends JavaRecursiveElementVisitor {

	private static final String logStatementMethodName = "%s(%s)";

	private static final Logger logger = LoggerFactory.getLogger(AbstractMethodLoggingVisitor.class);

	/**
	 * Returns true if the given statements are equal.  Equality is determined by getting the String contents of the statement and comparing them without any whitespace considered.
	 *
	 * @param statementA The first statement
	 * @param statementB The second statement
	 * @return The equality
	 */
	public boolean areStatementsEqual(PsiStatement statementA, PsiStatement statementB) {
		String a = statementA.getText().replace("\\s+", "");
		String b = statementB.getText().replace("\\s+", "");

		boolean status = a.equals(b);
		return status;
	}

	/**
	 * Returns the {@link PsiMethod} declaration -- including the method name and parameter types -- as a String
	 *
	 * @param method The method
	 * @return The method declaration
	 */
	public String getMethodDeclaration(PsiMethod method) {
		if (logger.isDebugEnabled()) {
			logger.debug("getMethodDeclaration(PsiMethod) - start");
		}
		String methodName = method.getName();
		List<String> methodParams = getMethodParameterTypes(method);
		String methodParamsStr = String.join(",", methodParams);

		String methodDeclaration = AbstractLoggingVisitor.logStatementMethodName.formatted(methodName, methodParamsStr);

		if (logger.isDebugEnabled()) {
			logger.debug("getMethodDeclaration(PsiMethod) - end");
		}
		return methodDeclaration;
	}

	/**
	 * Returns a {@link List} of parameter types for the given {@link PsiMethod}
	 *
	 * @param method The method
	 * @return The parameter types
	 */
	public List<String> getMethodParameterTypes(PsiMethod method) {
		if (logger.isDebugEnabled()) {
			logger.debug("getMethodParameterTypes(PsiMethod) - start");
		}
		List<String> paramTypes = new ArrayList<>();

		PsiParameterList params = method.getParameterList();
		if (params != null) {
			for (PsiParameter param : params.getParameters()) {
				logger.debug("paramName=" + param.getName() + ", paramType=" + param.getType().getPresentableText());
				paramTypes.add(param.getType().getPresentableText());
			}
		} else {
			// No parameters
		}

		if (logger.isDebugEnabled()) {
			logger.debug("getMethodParameterTypes(PsiMethod) - end");
		}
		return paramTypes;
	}
}
