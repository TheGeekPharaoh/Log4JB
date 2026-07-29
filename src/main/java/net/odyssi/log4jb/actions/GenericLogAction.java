package net.odyssi.log4jb.actions;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import net.odyssi.log4jb.actions.dialogs.GenericLogFormDialog;
import net.odyssi.log4jb.actions.dialogs.forms.GenericLogModel;
import net.odyssi.log4jb.visitors.DeclareLoggerVisitor;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Generates a user-defined log statement at the selected location.
 *
 * @author sdnakhla
 */
public class GenericLogAction extends AnAction {

    private static final String baseTemplate = "if(%s.is%sEnabled()) {\n\t%s.%s(\"%s%s%s\"%s);\n}\n";
    private static final String loggerObjectName = "logger";

    @Override
    public void update(@NotNull AnActionEvent e) {
        // This action is enabled only if the cursor is inside a Java method.
        boolean isEnabled = isJavaMethodSelected(e);
        e.getPresentation().setEnabledAndVisible(isEnabled);
    }

    private boolean isJavaMethodSelected(AnActionEvent e) {
        final var editor = e.getData(CommonDataKeys.EDITOR);
        final var psiFile = e.getData(CommonDataKeys.PSI_FILE);

        if (psiFile instanceof PsiJavaFile && editor != null) {
            final var elementAt = psiFile.findElementAt(editor.getCaretModel().getOffset());
            return PsiTreeUtil.getParentOfType(elementAt, PsiMethod.class) != null;
        }
        return false;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        final Project project = e.getProject();
        final Editor editor = e.getData(CommonDataKeys.EDITOR);
        final PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);
        if (project == null || editor == null || !(psiFile instanceof PsiJavaFile)) {
            return;
        }

        final int offset = editor.getCaretModel().getOffset();
        final PsiElement elementAt = psiFile.findElementAt(offset);
        final PsiClass selectedClass = PsiTreeUtil.getParentOfType(elementAt, PsiClass.class);
        final PsiMethod selectedMethod = PsiTreeUtil.getParentOfType(elementAt, PsiMethod.class);

        if (selectedClass == null || selectedMethod == null) {
            return;
        }

        GenericLogFormDialog dialog = new GenericLogFormDialog(
                project,
                getClassVariables(psiFile, offset),
                getLocalVariables(psiFile, offset),
                getMethodParameters(psiFile, offset)
        );

        if (dialog.showAndGet()) {
            GenericLogModel logModel = dialog.buildLogModel();
            WriteCommandAction.runWriteCommandAction(project, "Log4JB: Insert Log Statement", null, () -> {
                // First, ensure the logger is declared.
                selectedClass.accept(new DeclareLoggerVisitor(selectedClass));

                // Build and insert the log statement.
                applyLogStatements(logModel, selectedMethod, project, editor);
            });
        }
    }

    /**
     * Applies the logging as defined in the {@link GenericLogModel}.
     *
     * @param logModel       The log model
     * @param selectedMethod The method the cursor is inside
     * @param project        The project
     * @param editor         The editor
     */
    protected void applyLogStatements(GenericLogModel logModel, PsiMethod selectedMethod, Project project, Editor editor) {
        final PsiClass containingClass = selectedMethod.getContainingClass();
        if (containingClass == null) {
            return;
        }

        String logStatementStr = buildLogStatement(logModel, selectedMethod);
        PsiElementFactory factory = JavaPsiFacade.getElementFactory(project);
        PsiStatement logStatement = factory.createStatementFromText(logStatementStr, null);

        PsiFile file = selectedMethod.getContainingFile();
        int caretOffset = editor.getCaretModel().getOffset();

        // Ensure that the document is in a consistent state
        PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(editor.getDocument());

        // Calculate the offset and find the anchor element
        int line = editor.getDocument().getLineNumber(caretOffset) + 1;
        int lineStartOffset = editor.getDocument().getLineStartOffset(line);
        PsiElement anchorElement = file.findElementAt(lineStartOffset);

        // Insert the log statement at the appropriate location
        if (anchorElement != null) {
            PsiElement anchorParent = anchorElement.getParent();
            if (anchorParent != null) {
                anchorParent.addAfter(logStatement, anchorElement);
            } else {
                file.add(logStatement);
            }
        } else {
            file.add(logStatement);
        }
    }

    /**
     * Builds the log statement as defined in the {@link GenericLogModel}.
     *
     * @param logModel       The log model
     * @param selectedMethod The method the cursor is inside
     * @return The log statement text
     */
    protected String buildLogStatement(GenericLogModel logModel, PsiMethod selectedMethod) {
        String methodDeclaration = getMethodDeclaration(selectedMethod);
        String logLevelOperation = getLogLevelOperation(logModel.getLogLevel());
        String logMessage = (logModel.getLogMessage() != null && !logModel.getLogMessage().isEmpty())
                ? " - " + logModel.getLogMessage() : "";
        String variableLogStatement = getVariableLogStatement(
                logModel.getSelectedGlobalVariables(),
                logModel.getSelectedLocalVariables(),
                logModel.getSelectedMethodParameters()
        );
        String variableLogValues = getVariableLogValuesStatement(
                logModel.getSelectedGlobalVariables(),
                logModel.getSelectedLocalVariables(),
                logModel.getSelectedMethodParameters()
        );

        return baseTemplate.formatted(
                loggerObjectName, capitalizeFirstLetter(logLevelOperation),
                loggerObjectName, logLevelOperation,
                methodDeclaration, logMessage, variableLogStatement, variableLogValues
        );
    }

    /**
     * Builds the method signature string for use in log messages.
     * Example: "myMethod(String,int)"
     */
    public String getMethodDeclaration(PsiMethod method) {
        final var methodName = method.getName();
        final var parameterTypes = Arrays.stream(method.getParameterList().getParameters())
                .map(PsiParameter::getType)
                .map(PsiType::getPresentableText)
                .collect(Collectors.joining(","));

        return String.format("%s(%s)", methodName, parameterTypes);
    }

    /**
     * Builds the variable placeholder portion of a log message.
     * Example: " - name={}, age={}"
     */
    protected String getVariableLogStatement(Set<String> globalVariables, Set<String> localVariables, Set<String> methodParameters) {
        if (globalVariables.isEmpty() && localVariables.isEmpty() && methodParameters.isEmpty()) {
            return "";
        }

        Set<String> combinedVariables = new LinkedHashSet<>();
        combinedVariables.addAll(globalVariables);
        combinedVariables.addAll(localVariables);
        combinedVariables.addAll(methodParameters);

        return " - " + combinedVariables.stream().collect(Collectors.joining("={}, ")) + "={}";
    }

    /**
     * Builds the variable values portion of a log statement.
     * Example: ", name, age"
     */
    protected String getVariableLogValuesStatement(Set<String> globalVariables, Set<String> localVariables, Set<String> methodParameters) {
        if (globalVariables.isEmpty() && localVariables.isEmpty() && methodParameters.isEmpty()) {
            return "";
        }

        Set<String> combinedVariables = new LinkedHashSet<>();
        combinedVariables.addAll(globalVariables);
        combinedVariables.addAll(localVariables);
        combinedVariables.addAll(methodParameters);

        return ", " + String.join(", ", combinedVariables);
    }

    /**
     * Returns the SLF4J method name corresponding to the given log level.
     */
    protected String getLogLevelOperation(String logLevel) {
        return switch (logLevel) {
            case "DEBUG", "INFO", "WARN", "ERROR", "FATAL", "TRACE" -> logLevel.toLowerCase();
            default -> "debug";
        };
    }

    private String capitalizeFirstLetter(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    /**
     * Returns the parameter information for the method the caret is currently in.
     *
     * @param psiFile The PSI file
     * @param offset  The caret offset
     * @return The method parameters as name/type pairs
     */
    protected Set<String[]> getMethodParameters(PsiFile psiFile, int offset) {
        final PsiElement element = psiFile.findElementAt(offset);
        final PsiMethod method = PsiTreeUtil.getParentOfType(element, PsiMethod.class);

        if (method != null) {
            Set<String[]> parameterInfo = new LinkedHashSet<>();
            for (PsiParameter parameter : method.getParameterList().getParameters()) {
                parameterInfo.add(new String[]{parameter.getName(), parameter.getType().getPresentableText()});
            }
            return parameterInfo;
        }

        return new LinkedHashSet<>();
    }

    /**
     * Returns the field information for the current class.
     *
     * @param psiFile The PSI file
     * @param offset  The caret offset
     * @return The class fields as name/type pairs
     */
    protected Set<String[]> getClassVariables(PsiFile psiFile, int offset) {
        final PsiElement element = psiFile.findElementAt(offset);
        final PsiClass clazz = PsiTreeUtil.getParentOfType(element, PsiClass.class);

        if (clazz != null) {
            Set<String[]> fieldInfo = new LinkedHashSet<>();
            for (PsiField field : clazz.getFields()) {
                fieldInfo.add(new String[]{field.getName(), field.getType().getPresentableText()});
            }
            return fieldInfo;
        }

        return new LinkedHashSet<>();
    }

    /**
     * Returns the local variables declared before the cursor position, including
     * variables in nested scopes (for loops, if blocks, etc.).
     *
     * @param psiFile The PSI file
     * @param offset  The caret offset
     * @return The local variable information as name/type pairs
     */
    protected Set<String[]> getLocalVariables(PsiFile psiFile, int offset) {
        final PsiElement element = psiFile.findElementAt(offset);
        final PsiMethod method = PsiTreeUtil.getParentOfType(element, PsiMethod.class);

        if (method == null || method.getBody() == null) {
            return new LinkedHashSet<>();
        }

        Set<String[]> variableInfo = new LinkedHashSet<>();

        // Use PsiTreeUtil to find all local variables in the method body, including nested scopes
        Collection<PsiLocalVariable> allLocalVariables = PsiTreeUtil.findChildrenOfType(method.getBody(), PsiLocalVariable.class);
        for (PsiLocalVariable variable : allLocalVariables) {
            // Only include variables declared before the caret position
            if (variable.getTextRange().getStartOffset() < offset) {
                variableInfo.add(new String[]{variable.getName(), variable.getType().getPresentableText()});
            }
        }

        return variableInfo;
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}
