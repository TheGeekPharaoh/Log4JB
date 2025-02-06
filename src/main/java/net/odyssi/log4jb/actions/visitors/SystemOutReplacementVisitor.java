package net.odyssi.log4jb.actions.visitors;

import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * A {@link JavaRecursiveElementVisitor} that replaces calls to System.out.println with logger statements
 *
 * @author sdnakhla
 */
public class SystemOutReplacementVisitor extends AbstractReplacementVisitor {

	private final List<PsiMethodCallExpression> expressionsToReplace = new ArrayList<>();

	public SystemOutReplacementVisitor(PsiMethod method) {
		super(method);
	}

	@Override
	public void visitMethodCallExpression(PsiMethodCallExpression expression) {
		super.visitMethodCallExpression(expression);

		if (PsiTreeUtil.isAncestor(getMethod(), expression, false)) {
			// Check if the method call is System.out.println()
			PsiMethod calledMethod = expression.resolveMethod();
			if (calledMethod != null && calledMethod.getContainingClass().getQualifiedName().equals("java.io.PrintStream")
					&& calledMethod.getName().equals("println")) {
				PsiExpression qualifier = expression.getMethodExpression().getQualifierExpression();
				if (qualifier != null && qualifier.getText().equals("System.out")) {
					expressionsToReplace.add(expression);
				}
			}
		}
	}

	@Override
	public void visitJavaFile(@NotNull PsiJavaFile file) {
		super.visitJavaFile(file);
		this.performReplacements();
	}

	protected void performReplacements() {
		for (PsiMethodCallExpression expression : expressionsToReplace) {
			// Create a new logger.debug() call
			PsiElementFactory factory = PsiElementFactory.getInstance(expression.getProject());
			String args = expression.getArgumentList().getText();
			// Remove the parentheses from the argument list
			args = args.substring(1, args.length() - 1);
			String methodDeclaration = getMethodDeclaration(getMethod());

			String template = "if(%s.isDebugEnabled()) {\n\t%s.debug(\"%s - %s\");\n}";
			String sanitizedArgs = args.replaceAll("^\"|\"$", "");

			String logStatementText = template.formatted("logger", "logger", methodDeclaration, sanitizedArgs);

			PsiStatement logStatement = factory.createStatementFromText(logStatementText, null);
			PsiElement parent = expression.getParent();
			if (parent instanceof PsiStatement) {
				PsiStatement statement = (PsiStatement) parent;
				statement.replace(logStatement);
			}
		}
	}
}