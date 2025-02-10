package net.odyssi.log4jb.actions.visitors;

import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import net.odyssi.log4jb.actions.LogMethodAction;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * A {@link JavaRecursiveElementVisitor} that replaces calls to System.err.println with logger statements
 *
 * @author sdnakhla
 */
public class SystemErrReplacementVisitor extends AbstractReplacementVisitor {

	private static final Logger logger = LoggerFactory.getLogger(SystemErrReplacementVisitor.class);

	private final List<PsiMethodCallExpression> expressionsToReplace = new ArrayList<>();

	public SystemErrReplacementVisitor(PsiMethod method) {
		super(method);
		if (logger.isDebugEnabled()) {
			logger.debug("SystemErrReplacementVisitor(PsiMethod) - end");
		}
	}

	@Override
	public void visitMethodCallExpression(PsiMethodCallExpression expression) {
		if (logger.isDebugEnabled()) {
			logger.debug("visitMethodCallExpression(PsiMethodCallExpression) - start");
		}
		super.visitMethodCallExpression(expression);

		if (PsiTreeUtil.isAncestor(getMethod(), expression, false)) {
			// Check if the method call is System.out.println()
			PsiMethod calledMethod = expression.resolveMethod();
			if (calledMethod != null && calledMethod.getContainingClass().getQualifiedName().equals("java.io.PrintStream")
					&& calledMethod.getName().equals("println")) {
				PsiExpression qualifier = expression.getMethodExpression().getQualifierExpression();
				if (qualifier != null && qualifier.getText().equals("System.err")) {
					expressionsToReplace.add(expression);
				}
			}
		}
		if (logger.isDebugEnabled()) {
			logger.debug("visitMethodCallExpression(PsiMethodCallExpression) - end");
		}
	}

	@Override
	public void visitJavaFile(@NotNull PsiJavaFile file) {
		if (logger.isDebugEnabled()) {
			logger.debug("visitJavaFile(PsiJavaFile) - start");
		}
		super.visitJavaFile(file);
		this.performReplacements();
		if (logger.isDebugEnabled()) {
			logger.debug("visitJavaFile(PsiJavaFile) - end");
		}
	}

	protected void performReplacements() {
		if (logger.isDebugEnabled()) {
			logger.debug("performReplacements() - start");
		}
		for (PsiMethodCallExpression expression : expressionsToReplace) {
			// Create a new logger.debug() call
			PsiElementFactory factory = PsiElementFactory.getInstance(expression.getProject());
			String args = expression.getArgumentList().getText();
			// Remove the parentheses from the argument list
			args = args.substring(1, args.length() - 1);
			String methodDeclaration = getMethodDeclaration(getMethod());

			String template = "%s.error(\"%s - %s\");";
			String sanitizedArgs = args.replaceAll("^\"|\"$", "");

			String logStatementText = template.formatted("logger", methodDeclaration, sanitizedArgs);

			PsiStatement logStatement = factory.createStatementFromText(logStatementText, null);
			PsiElement parent = expression.getParent();
			if (parent instanceof PsiStatement) {
				PsiStatement statement = (PsiStatement) parent;
				statement.replace(logStatement);
			}
		}
		if (logger.isDebugEnabled()) {
			logger.debug("performReplacements() - end");
		}
	}
}