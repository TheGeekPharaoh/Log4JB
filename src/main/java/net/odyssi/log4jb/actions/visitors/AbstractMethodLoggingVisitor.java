package net.odyssi.log4jb.actions.visitors;

import com.intellij.psi.JavaRecursiveElementVisitor;
import com.intellij.psi.PsiMethod;

/**
 * The abstract base class for method logging visitors
 *
 * @author sdnakhla
 */
public abstract class AbstractMethodLoggingVisitor extends JavaRecursiveElementVisitor {

	private final PsiMethod method;

	public AbstractMethodLoggingVisitor(PsiMethod method) {
		this.method = method;
	}

	public PsiMethod getMethod() {
		return method;
	}
}
