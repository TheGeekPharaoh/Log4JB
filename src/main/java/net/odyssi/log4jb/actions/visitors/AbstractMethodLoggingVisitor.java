package net.odyssi.log4jb.actions.visitors;

import com.intellij.psi.JavaRecursiveElementVisitor;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiParameterList;

import java.util.ArrayList;
import java.util.List;

/**
 * The abstract base class for method logging visitors
 *
 * @author sdnakhla
 */
public abstract class AbstractMethodLoggingVisitor extends JavaRecursiveElementVisitor {

	private static final String logStatementMethodName = "%s(%s)";

	private final PsiMethod method;

	public AbstractMethodLoggingVisitor(PsiMethod method) {
		this.method = method;
	}

	public PsiMethod getMethod() {
		return method;
	}

	/**
	 * Returns the {@link PsiMethod} declaration -- including the method name and parameter types -- as a String
	 *
	 * @param method The method
	 * @return The method declaration
	 */
	protected String getMethodDeclaration(PsiMethod method) {
		String methodName = method.getName();
		List<String> methodParams = getMethodParameterTypes(method);
		String methodParamsStr = String.join(",", methodParams);

		String methodDeclaration = logStatementMethodName.formatted(methodName, methodParamsStr);

		return methodDeclaration;
	}

	/**
	 * Returns a {@link List} of parameter types for the given {@link PsiMethod}
	 *
	 * @param method The method
	 * @return The parameter types
	 */
	protected List<String> getMethodParameterTypes(PsiMethod method) {
		List<String> paramTypes = new ArrayList<>();

		PsiParameterList params = method.getParameterList();
		if (params != null) {
			for (PsiParameter param : params.getParameters()) {
				System.out.println("paramName=" + param.getName() + ", paramType=" + param.getType().getPresentableText());
				paramTypes.add(param.getType().getPresentableText());
			}
		} else {
			System.out.println("NO PARAMS");
		}

		return paramTypes;
	}
}
