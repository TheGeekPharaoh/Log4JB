package net.odyssi.log4jb.actions.visitors;

import com.intellij.psi.JavaRecursiveElementVisitor;
import com.intellij.psi.PsiMethod;

/**
 * The abstract base class for a Visitor that is used to replace statements within a Java class
 *
 * @author sdnakhla
 */
public abstract class AbstractReplacementVisitor extends JavaRecursiveElementVisitor {

	private final PsiMethod method;

	public AbstractReplacementVisitor(PsiMethod method) {
		this.method = method;
	}

	public PsiMethod getMethod() {
		return method;
	}
}
