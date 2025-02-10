package net.odyssi.log4jb.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * The abstract base class for a logging action
 *
 * @author sdnakhla
 */
public abstract class AbstractLoggingAction extends AnAction {

	public static final String loggerVisibility = "private static final";

	public static final String loggerObjectName = "logger";

	public static final String[] classImports = new String[]{"org.slf4j.Logger", "org.slf4j.LoggerFactory"};

	public static final String loggerClassName = "Logger";

	private static final String loggerValueTemplate = "LoggerFactory.getLogger(%s.class);";

	private static final String logStatementMethodName = "%s(%s)";

	private static final Logger logger = LoggerFactory.getLogger(AbstractLoggingAction.class);

	/**
	 * Returns true if the selected element is a {@link PsiClass}
	 *
	 * @param e The event
	 * @return The status
	 */
	public boolean isJavaClassSelected(AnActionEvent e) {
		if (logger.isDebugEnabled()) {
			logger.debug("isJavaClassSelected(AnActionEvent) - start");
		}
		Project proj = e.getProject();
		Editor editor = e.getRequiredData(CommonDataKeys.EDITOR);

		boolean status = getSelectedCursorClass(proj, editor) != null;

		if (logger.isDebugEnabled()) {
			logger.debug("isJavaClassSelected(AnActionEvent) - end");
		}
		return status;
	}

	/**
	 * Returns the {@link PsiMethod} the cursor is currently in
	 *
	 * @param project The project
	 * @param editor  The editor
	 * @return The selected method, if applicable
	 */
	public PsiMethod getSelectedCursorMethod(Project project, Editor editor) {
		if (logger.isDebugEnabled()) {
			logger.debug("getSelectedCursorMethod(Project,Editor) - start");
		}
		PsiFile file = PsiManager.getInstance(project).findFile(editor.getVirtualFile());

		if (file == null) {
			if (logger.isDebugEnabled()) {
				logger.debug("getSelectedCursorMethod(Project,Editor) - end");
			}
			return null;
		}

		// Get the offset of the cursor
		int offset = editor.getCaretModel().getOffset();

		// Find the element at the cursor position
		PsiElement element = file.getViewProvider().findElementAt(offset);

		// If not, try to find the parent PsiMethod
		PsiElement parent = element;
		while ((parent = parent.getParent()) != null) {
			if (parent instanceof PsiMethod) {
				System.out.println("FOUND METHOD - " + parent);
				if (logger.isDebugEnabled()) {
					logger.debug("getSelectedCursorMethod(Project,Editor) - end");
				}
				return (PsiMethod) parent;
			}
		}

		if (logger.isDebugEnabled()) {
			logger.debug("getSelectedCursorMethod(Project,Editor) - end");
		}
		return null;
	}

	/**
	 * Returns the {@link PsiClass} the cursor is currently in
	 *
	 * @param project The project
	 * @param editor  The editor
	 * @return The selected class, if applicable
	 */
	public PsiClass getSelectedCursorClass(Project project, Editor editor) {
		if (logger.isDebugEnabled()) {
			logger.debug("getSelectedCursorClass(Project,Editor) - start");
		}
		PsiFile file = PsiManager.getInstance(project).findFile(editor.getVirtualFile());

		if (file == null) {
			if (logger.isDebugEnabled()) {
				logger.debug("getSelectedCursorClass(Project,Editor) - end");
			}
			return null;
		}

		// Get the offset of the cursor
		int offset = editor.getCaretModel().getOffset();

		// Find the element at the cursor position
		PsiElement element = file.getViewProvider().findElementAt(offset);

		// If not, try to find the parent PsiClass
		PsiElement parent = element;
		while ((parent = parent.getParent()) != null) {
			if (parent instanceof PsiClass) {
				System.out.println("FOUND CLASS - " + parent);
				if (logger.isDebugEnabled()) {
					logger.debug("getSelectedCursorClass(Project,Editor) - end");
				}
				return (PsiClass) parent;
			}
		}

		if (logger.isDebugEnabled()) {
			logger.debug("getSelectedCursorClass(Project,Editor) - end");
		}
		return null;
	}

	/**
	 * Returns the {@link PsiLocalVariable} the cursor is currently in
	 *
	 * @param project The project
	 * @param editor  The editor
	 * @return The selected variable, if applicable
	 */
	public PsiLocalVariable getSelectedCursorLocalVariable(Project project, Editor editor) {
		if (logger.isDebugEnabled()) {
			logger.debug("getSelectedCursorLocalVariable(Project,Editor) - start");
		}
		PsiMethod selectedMethod = getSelectedCursorMethod(project, editor);
		if (selectedMethod == null) {
			if (logger.isDebugEnabled()) {
				logger.debug("getSelectedCursorLocalVariable(Project,Editor) - end");
			}
			return null;
		}

		PsiFile file = selectedMethod.getContainingFile();

		int offset = editor.getCaretModel().getOffset();
		PsiElement element = file.findElementAt(offset);
		if (element == null) {
			if (logger.isDebugEnabled()) {
				logger.debug("getSelectedCursorLocalVariable(Project,Editor) - end");
			}
			return null;
		}

		if (element instanceof PsiIdentifier identifier) {
			PsiReference reference = identifier.getReference();
			if (reference != null) {
				PsiElement declaration = reference.resolve();
				if (declaration instanceof PsiLocalVariable) {
					if (logger.isDebugEnabled()) {
						logger.debug("getSelectedCursorLocalVariable(Project,Editor) - end");
					}
					return (PsiLocalVariable) declaration;
				}
			} else {
				// If no reference is found, check if the identifier is part of a local variable declaration
				PsiElement parent = identifier.getParent();
				if (parent instanceof PsiLocalVariable) {
					if (logger.isDebugEnabled()) {
						logger.debug("getSelectedCursorLocalVariable(Project,Editor) - end");
					}
					return (PsiLocalVariable) parent;
				} else if (parent instanceof PsiReferenceExpression refExpr) {
					PsiElement resolvedElement = refExpr.resolve();
					if (resolvedElement instanceof PsiLocalVariable variable) {
						if (logger.isDebugEnabled()) {
							logger.debug("getSelectedCursorLocalVariable(Project,Editor) - end");
						}
						return variable;
					}
				}
			}
		}

		if (logger.isDebugEnabled()) {
			logger.debug("getSelectedCursorLocalVariable(Project,Editor) - end");
		}
		return null;
	}

	/**
	 * Returns true if the selected element is a {@link PsiMethod}
	 *
	 * @param e The event
	 * @return The status
	 */
	public boolean isJavaMethodSelected(AnActionEvent e) {
		if (logger.isDebugEnabled()) {
			logger.debug("isJavaMethodSelected(AnActionEvent) - start");
		}

		Project proj = e.getProject();
		Editor editor = e.getRequiredData(CommonDataKeys.EDITOR);

		boolean status;
		if (editor == null) {
			status = false;
		} else {
			status = getSelectedCursorMethod(proj, editor) != null;
		}

		if (logger.isDebugEnabled()) {
			logger.debug("isJavaMethodSelected(AnActionEvent) - end");
		}
		return status;
	}

	/**
	 * Returns true if the selected element is a {@link PsiLocalVariable}
	 *
	 * @param e The event
	 * @return The status
	 */
	public boolean isJavaLocalVariableSelected(AnActionEvent e) {
		if (logger.isDebugEnabled()) {
			logger.debug("isJavaLocalVariableSelected(AnActionEvent) - start");
		}
		Project proj = e.getProject();
		Editor editor = e.getRequiredData(CommonDataKeys.EDITOR);

		boolean status = getSelectedCursorLocalVariable(proj, editor) != null;

		if (logger.isDebugEnabled()) {
			logger.debug("isJavaLocalVariableSelected(AnActionEvent) - end");
		}
		return status;
	}

	/**
	 * Creates a new {@link PsiStatement} from the given statement text
	 *
	 * @param psiClass      The PSI class
	 * @param statementText The statement text
	 * @return The statement
	 */
	public PsiStatement createExpressionStatement(PsiClass psiClass, String statementText) {
		if (logger.isDebugEnabled()) {
			logger.debug("createExpressionStatement(PsiClass,String) - start");
		}
		PsiElementFactory factory = JavaPsiFacade.getElementFactory(psiClass.getProject());
		PsiStatement statement = factory.createStatementFromText(statementText, null);

		if (logger.isDebugEnabled()) {
			logger.debug("createExpressionStatement(PsiClass,String) - end");
		}
		return statement;
	}

	/**
	 * Returns the {@link PsiMethod} declaration -- including the method name and parameter types -- as a String
	 *
	 * @param method The method
	 * @return The method declaration
	 */
	protected String getMethodDeclaration(PsiMethod method) {
		if (logger.isDebugEnabled()) {
			logger.debug("getMethodDeclaration(PsiMethod) - start");
		}
		String methodName = method.getName();
		List<String> methodParams = getMethodParameterTypes(method);
		String methodParamsStr = String.join(",", methodParams);

		String methodDeclaration = logStatementMethodName.formatted(methodName, methodParamsStr);

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
	private List<String> getMethodParameterTypes(PsiMethod method) {
		if (logger.isDebugEnabled()) {
			logger.debug("getMethodParameterTypes(PsiMethod) - start");
		}
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

		if (logger.isDebugEnabled()) {
			logger.debug("getMethodParameterTypes(PsiMethod) - end");
		}
		return paramTypes;
	}
}
