package net.odyssi.log4jb.actions.visitors;

import com.intellij.psi.*;
import com.intellij.psi.codeStyle.CodeStyleManager;
import org.apache.commons.lang.ArrayUtils;
import org.jetbrains.annotations.NotNull;

/**
 * A {@link org.intellij.markdown.ast.visitors.Visitor} implementation that adds a logging statement before each return
 * statement in a method
 */
// TODO Add check for existing log statements
public class MethodEndStatementVisitor extends AbstractMethodLoggingVisitor {

	public MethodEndStatementVisitor(PsiMethod method) {
		super(method);
	}

	@Override
	public void visitMethod(@NotNull PsiMethod method) {
		if(method.isConstructor()) {
			return;
		}

		super.visitMethod(method);
	}

	@Override
	public void visitReturnStatement(PsiReturnStatement returnStatement) {
		super.visitReturnStatement(returnStatement);
		this.addMethodReturnLoggingStatement(returnStatement);
	}

	@Override
	public void visitCodeBlock(@NotNull PsiCodeBlock block) {
		super.visitCodeBlock(block);
		if (block.getStatements().length == 0 || !(block.getStatements()[block.getStatements().length - 1] instanceof PsiReturnStatement)) {
			addMethodEndLoggingStatement(block);
		}
	}

	private PsiStatement buildLogStatement() {
		// TODO Generate proper log statement
		PsiStatement logStatement = JavaPsiFacade.getElementFactory(getMethod().getProject()).createStatementFromText("System.out.println(\"Method " + getMethod().getName() + " returned\");", getMethod());
		return logStatement;
	}

	/**
	 * Adds the method end logging statement immediately before a return statement
	 *
	 * @param returnStatement The return statement
	 */
	private void addMethodReturnLoggingStatement(PsiReturnStatement returnStatement) {
		PsiStatement logStatement = buildLogStatement();
		PsiCodeBlock codeBlock = (PsiCodeBlock) returnStatement.getParent();

		int index = ArrayUtils.indexOf(codeBlock.getStatements(), returnStatement);
		codeBlock.addBefore(logStatement, codeBlock.getStatements()[index]);
		CodeStyleManager.getInstance(getMethod().getProject()).reformat(logStatement);
	}

	private void addMethodEndLoggingStatement(PsiCodeBlock block) {
		PsiStatement logStatement = this.buildLogStatement();
		block.add(logStatement);
		CodeStyleManager.getInstance(getMethod().getProject()).reformat(logStatement);
	}
}
