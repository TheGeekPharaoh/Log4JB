package net.odyssi.log4jb.actions.visitors;

import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

/**
 * A {@link JavaRecursiveElementVisitor} that replaces calls to System.out.println with logger statements
 *
 * @author sdnakhla
 */
public class SystemOutReplacementVisitor extends AbstractReplacementVisitor {


	public SystemOutReplacementVisitor(PsiMethod method) {
		super(method);
	}



	public void visitMethodCallExpression(PsiMethodCallExpression expression) {
		if (PsiTreeUtil.isAncestor(getMethod(), expression, false)) {
			// Check if the method call is System.out.println()
			PsiMethod calledMethod = expression.resolveMethod();
			if (calledMethod != null && calledMethod.getContainingClass().getQualifiedName().equals("java.io.PrintStream")
					&& calledMethod.getName().equals("println")) {
				PsiExpression qualifier = expression.getMethodExpression().getQualifierExpression();
				if (qualifier != null && qualifier.getText().equals("System.out")) {
					// Create a new logger.debug() call
					PsiElementFactory factory = PsiElementFactory.getInstance(expression.getProject());
					String args = expression.getArgumentList().getText();
					// Remove the parentheses from the argument list
					args = args.substring(1, args.length() - 1);
					PsiMethodCallExpression newCall = (PsiMethodCallExpression) factory.createExpressionFromText("logger.debug(" + args + ")", expression);

					// Replace the original expression with the new one
					expression.replace(newCall);
				}
			}
		}
		super.visitMethodCallExpression(expression);
	}

	private String createLoggerStatement(PsiExpression argument, boolean isError) {
		String argumentText = argument.getText();
		String loggerName = "loggy"; // adjust this to your logger variable name
		String loggerMethod = isError ? "error" : "info";
		return String.format("%s.%s(%s);", loggerName, loggerMethod, argumentText);
	}
}