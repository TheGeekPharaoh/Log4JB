package net.odyssi.log4jb.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import net.odyssi.log4jb.dialogs.GenericLogFormDialog;
import net.odyssi.log4jb.forms.GenericLogModel;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Generates a user-defined log statement at the selected location
 *
 * @author sdnakhla
 */
public class GenericLogAction extends AbstractLoggingAction {

	private static final String baseTemplate = "if(%s.is%sEnabled()) {\n	%s.%s(\"%s %s%s\"%s);\n}\n";

	@Override
	public void actionPerformed(AnActionEvent e) {
		Project proj = e.getProject();
		Editor editor = e.getData(CommonDataKeys.EDITOR);

		GenericLogFormDialog dialog = new GenericLogFormDialog(proj, getClassVariables(editor), getLocalVariables(editor), getMethodParameters(editor));

		if (dialog.showAndGet()) {
			GenericLogModel logModel = dialog.buildLogModel();
			WriteCommandAction.runWriteCommandAction(proj, () -> {
				PsiClass selectedClass = getSelectedCursorClass(proj, editor);

				declareLogger(selectedClass);
				this.applyLogStatements(logModel, proj, editor);
			});
		}
	}

	/**
	 * Applies the logging as defined in the {@link GenericLogModel}
	 *
	 * @param logModel The log model
	 * @param project  The project
	 * @param editor   The editor
	 */
	protected void applyLogStatements(GenericLogModel logModel, Project project, Editor editor) {
		String logStatementStr = this.buildLogStatement(logModel, project, editor);

		PsiMethod selectedMethod = getSelectedCursorMethod(project, editor);
		PsiStatement logStatement = this.createExpressionStatement(selectedMethod.getContainingClass(), logStatementStr);

		PsiFile file = selectedMethod.getContainingFile();
		int caretOffset = editor.getCaretModel().getOffset();

		// Ensure that the document is in a consistent state
		PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(editor.getDocument());

		// Calculate the offset and find the anchor element
		int line = editor.getDocument().getLineNumber(caretOffset) + 1;
		int lineStartOffset = editor.getDocument().getLineStartOffset(line);
		PsiElement anchorElement = file.findElementAt(lineStartOffset);

		// Check if the anchor element is valid
		if (anchorElement != null) {
			PsiElement anchorParent = anchorElement.getParent();
			if (anchorParent != null) {
				// Add the log statement after the anchor element
				anchorParent.addAfter(logStatement, anchorElement);
			} else {
				// Handle the case where the anchor element has no parent
				file.add(logStatement);
			}
		} else {
			// Handle the case where the anchor element is null
			file.add(logStatement);
		}
	}

	/**
	 * Builds the log statement as defined in the {@link GenericLogModel}
	 * @param logModel The log model
	 * @param project The project
	 * @param editor The editor
	 * @return The log statement
	 */
	protected String buildLogStatement(GenericLogModel logModel, Project project, Editor editor) {
		PsiMethod selectedMethod = getSelectedCursorMethod(project, editor);
		String methodDeclaration = getMethodDeclaration(selectedMethod);
		String logLevelOperation = getLogLevelOperation(logModel.getLogLevel());
		String logMessage = (logModel.getLogMessage() != null && logModel.getLogMessage().length() > 0) ? " - " + logModel.getLogMessage() : "";
		String variableLogStatement = getVariableLogStatement(logModel.getSelectedGlobalVariables(), logModel.getSelectedLocalVariables(), logModel.getSelectedMethodParameters());
		String variableLogValues = getVariableLogValuesStatement(logModel.getSelectedGlobalVariables(), logModel.getSelectedLocalVariables(), logModel.getSelectedMethodParameters());

		String logStatementStr = baseTemplate.formatted(loggerObjectName, capitalizeFirstLetter(logLevelOperation), loggerObjectName, logLevelOperation, methodDeclaration, logMessage, variableLogStatement, variableLogValues);
		System.out.println("logStatementStr=" + logStatementStr);

		return logStatementStr;
	}

	/**
	 * Builds a log statement from the given variables
	 * @param globalVariables The global variables
	 * @param localVariables The local variables
	 * @param methodParameters The method parameters
	 * @return The log statement
	 */
	protected String getVariableLogStatement(Set<String> globalVariables, Set<String> localVariables, Set<String> methodParameters) {
		String s = null;
		if(globalVariables.size() == 0 && localVariables.size() == 0 && methodParameters.size() == 0) {
			s = "";
		} else {
			Set<String> combinedVariables = new LinkedHashSet<>();
			combinedVariables.addAll(globalVariables);
			combinedVariables.addAll(localVariables);
			combinedVariables.addAll(methodParameters);

			s = " - " + combinedVariables.stream().collect(Collectors.joining("={}, ")) + "={}";
		}

		return s;
	}

	/**
	 * Builds a log value statement from the given variables
	 * @param globalVariables The global variables
	 * @param localVariables The local variables
	 * @param methodParameters The method parameters
	 * @return The log statement
	 */
	protected String getVariableLogValuesStatement(Set<String> globalVariables, Set<String> localVariables, Set<String> methodParameters) {
		String s = null;
		if (globalVariables.size() == 0 && localVariables.size() == 0 && methodParameters.size() == 0) {
			s = "";
		} else {
			Set<String> combinedVariables = new LinkedHashSet<>();
			combinedVariables.addAll(globalVariables);
			combinedVariables.addAll(localVariables);
			combinedVariables.addAll(methodParameters);

			s = ", " + combinedVariables.stream().collect(Collectors.joining(", "));
		}

		return s;
	}

	/**
	 * Returns the operation corresponding to the given log level value
	 *
	 * @param logLevel The log level
	 * @return The operation
	 */
	protected String getLogLevelOperation(String logLevel) {
		String s = null;
		switch (logLevel) {
			case "DEBUG":
			case "INFO":
			case "WARN":
			case "ERROR":
			case "FATAL":
			case "TRACE":
				s = logLevel.toLowerCase();
				break;
			default:
				s = "debug";
				break;
		}

		return s;
	}

	private String capitalizeFirstLetter(String str) {
		if (str == null || str.isEmpty()) {
			return str;
		}
		return str.substring(0, 1).toUpperCase() + str.substring(1);
	}

	/**
	 * Returns the parameter information for the method the caret is currently in
	 *
	 * @param editor The editor
	 * @return The method parameters
	 */
	protected Set<String[]> getMethodParameters(Editor editor) {
		Caret caret = editor.getCaretModel().getPrimaryCaret();

		// Get the current project
		Project project = editor.getProject();

		// Get the PSI file
		PsiFile psiFile = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());

		// Get the PSI element at the caret position
		PsiElement element = psiFile.findElementAt(caret.getOffset());

		// Find the parent PsiMethod element
		PsiMethod method = PsiTreeUtil.getParentOfType(element, PsiMethod.class);

		if (method != null) {
			// Get the method parameters
			PsiParameterList parameterList = method.getParameterList();
			PsiParameter[] parameters = parameterList.getParameters();

			// Create a set to store the parameter information
			Set<String[]> parameterInfo = new HashSet<>();

			// Iterate over the parameters and extract the name and type
			for (PsiParameter parameter : parameters) {
				String paramName = parameter.getName();
				String paramType = parameter.getType().getCanonicalText();

				parameterInfo.add(new String[]{paramName, paramType});
			}

			return parameterInfo;
		}

		// If no parent PsiMethod element is found, return an empty set
		return new HashSet<>();
	}

	/**
	 * Returns the variable information for the current class
	 *
	 * @param editor The editor
	 * @return The variables
	 */
	protected Set<String[]> getClassVariables(Editor editor) {
		// Get the current caret
		Caret caret = editor.getCaretModel().getPrimaryCaret();

		// Get the current project
		Project project = editor.getProject();

		// Get the PSI file
		PsiFile psiFile = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());

		// Get the PSI element at the caret position
		PsiElement element = psiFile.findElementAt(caret.getOffset());

		// Find the parent PsiClass element
		PsiClass clazz = PsiTreeUtil.getParentOfType(element, PsiClass.class);

		if (clazz != null) {
			// Get the class fields
			PsiField[] fields = clazz.getFields();

			// Create a set to store the field information
			Set<String[]> fieldInfo = new HashSet<>();

			// Iterate over the fields and extract the name and type
			for (PsiField field : fields) {
				String fieldName = field.getName();
				String fieldType = field.getType().getCanonicalText();

				fieldInfo.add(new String[]{fieldName, fieldType});
			}

			return fieldInfo;
		}

		// If no parent PsiClass element is found, return an empty set
		return new HashSet<>();
	}

	/**
	 * Returns the local variables that are declared before the cursor position
	 *
	 * @param editor The editor
	 * @return The variable information
	 */
	protected Set<String[]> getLocalVariables(Editor editor) {
		// Get the current caret
		Caret caret = editor.getCaretModel().getPrimaryCaret();

		// Get the current project
		Project project = editor.getProject();

		// Get the PSI file
		PsiFile psiFile = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());

		// Get the PSI element at the caret position
		PsiElement element = psiFile.findElementAt(caret.getOffset());

		// Find the parent PsiMethod element
		PsiMethod method = PsiTreeUtil.getParentOfType(element, PsiMethod.class);

		if (method != null) {
			// Create a set to store the local variable information
			Set<String[]> variableInfo = new HashSet<>();

			// Iterate over all elements within the method body
			for (PsiElement child : method.getBody().getChildren()) {
				// Check if the element is a declaration statement
				if (child instanceof PsiDeclarationStatement statement) {
					// Check if the declaration statement has a local variable declaration
					PsiElement declaration = statement.getDeclaredElements()[0];
					if (declaration instanceof PsiLocalVariable variable) {
						// Check if the variable declaration is before the caret
						if (variable.getTextRange().getStartOffset() < caret.getOffset()) {
							String variableName = variable.getName();
							String variableType = variable.getType().getCanonicalText();

							variableInfo.add(new String[]{variableName, variableType});
						}
					}
				}
			}

			return variableInfo;
		}

		// If no parent PsiMethod element is found, return an empty set
		return new HashSet<>();
	}
}
