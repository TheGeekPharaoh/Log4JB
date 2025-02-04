package net.odyssi.log4jb.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
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
	 * Adds import statements to the given {@link PsiClass}.  This method should only be called within a {@link WriteCommandAction}
	 *
	 * @param psiClass   The class
	 * @param classNames The import class names
	 */
	public void addImportStatements(PsiClass psiClass, String... classNames) {
		if (logger.isDebugEnabled()) {
			logger.debug("addImportStatements(PsiClass,String...) - start");
		}
		System.out.println("ADDING IMPORTS - psiClass=" + psiClass + ", classNames=" + Arrays.toString(classNames));
		Project proj = psiClass.getProject();

		PsiElementFactory factory = JavaPsiFacade.getInstance(proj).getElementFactory();
		PsiJavaFile javaFile = (PsiJavaFile) psiClass.getContainingFile();
		for (String className : classNames) {
			PsiClass importClass = JavaPsiFacade.getInstance(proj).findClass(className, GlobalSearchScope.allScope(proj));
			if (importClass == null) {
			} else {
				System.out.println("Adding import " + className + "...");
				PsiImportStatement importStatement = JavaPsiFacade.getElementFactory(proj).createImportStatement(importClass);
				((PsiJavaFile) psiClass.getContainingFile()).getImportList().add(importStatement);
			}
		}
		if (logger.isDebugEnabled()) {
			logger.debug("addImportStatements(PsiClass,String...) - end");
		}
	}

	/**
	 * Returns true if a logger is already declared for the given {@link PsiClass}
	 *
	 * @param psiClass The PSI class
	 * @return The status
	 */
	public boolean isLoggerDeclared(PsiClass psiClass) {
		if (logger.isDebugEnabled()) {
			logger.debug("isLoggerDeclared(PsiClass) - start");
		}
		PsiField field = findExistingField(psiClass, loggerObjectName);
		boolean status = (field != null);

		if (logger.isDebugEnabled()) {
			logger.debug("isLoggerDeclared(PsiClass) - end");
		}
		return status;
	}

	/**
	 * Declares a logger on the given {@link PsiClass}
	 *
	 * @param psiClass The PSI class
	 */
	public void declareLogger(PsiClass psiClass) {
		if (logger.isDebugEnabled()) {
			logger.debug("declareLogger(PsiClass) - start");
		}
		if (psiClass == null) {
		} else {
			if (isLoggerDeclared(psiClass)) {
			} else {
				Project proj = psiClass.getProject();
				PsiElementFactory factory = JavaPsiFacade.getElementFactory(psiClass.getProject());

				String loggerSignature = loggerVisibility + " " + loggerClassName + " " + loggerObjectName + " = " + loggerValueTemplate.formatted(psiClass.getName());
				System.out.println("loggerSignature=" + loggerSignature);

				WriteCommandAction.runWriteCommandAction(proj, () -> {
					addImportStatements(psiClass, classImports);
					PsiField loggerField = factory.createFieldFromText(loggerSignature, psiClass);

					psiClass.add(loggerField);
				});
			}
		}
		if (logger.isDebugEnabled()) {
			logger.debug("declareLogger(PsiClass) - end");
		}
	}

	/**
	 * Returns the {@link PsiField} with the given name, or null if no match is found
	 *
	 * @param psiClass  The PSI class
	 * @param fieldName The field name
	 * @return The PSI field
	 */
	private PsiField findExistingField(PsiClass psiClass, String fieldName) {
		if (logger.isDebugEnabled()) {
			logger.debug("findExistingField(PsiClass,String) - start");
		}
		for (PsiField field : psiClass.getFields()) {
			if (field.getName().equals(fieldName)) {
				return field;
			}
		}

		if (logger.isDebugEnabled()) {
			logger.debug("findExistingField(PsiClass,String) - end");
		}
		return null;
	}

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
		PsiFile file = selectedMethod.getContainingFile();

		int offset = editor.getCaretModel().getOffset();
		PsiElement element = file.findElementAt(offset);
		if (element == null) {
			return null;
		}

		if (element instanceof PsiIdentifier identifier) {
			PsiReference reference = identifier.getReference();
			if (reference != null) {
				PsiElement declaration = reference.resolve();
				if (declaration instanceof PsiLocalVariable) {
					return (PsiLocalVariable) declaration;
				}
			} else {
				// If no reference is found, check if the identifier is part of a local variable declaration
				PsiElement parent = identifier.getParent();
				if (parent instanceof PsiLocalVariable) {
					return (PsiLocalVariable) parent;
				} else if (parent instanceof PsiReferenceExpression refExpr) {
					PsiElement resolvedElement = refExpr.resolve();
					if (resolvedElement instanceof PsiLocalVariable variable) {
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

	private PsiIdentifier findIdentifier(PsiElement element) {
		if (logger.isDebugEnabled()) {
			logger.debug("findIdentifier(PsiElement) - start");
		}
		if (element instanceof PsiIdentifier) {
			return (PsiIdentifier) element;
		}
		for (PsiElement child : element.getChildren()) {
			PsiIdentifier identifier = findIdentifier(child);
			if (identifier != null) {
				return identifier;
			}
		}

		if (logger.isDebugEnabled()) {
			logger.debug("findIdentifier(PsiElement) - end");
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
