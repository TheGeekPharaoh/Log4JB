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
public abstract class AbstractMethodLoggingVisitor extends AbstractLoggingVisitor {

	private final PsiMethod method;

	public AbstractMethodLoggingVisitor(PsiMethod method) {
		this.method = method;
	}

	public PsiMethod getMethod() {
		return method;
	}

}
