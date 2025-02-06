package net.odyssi.log4jb.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.CodeStyleManager;
import net.odyssi.log4jb.actions.visitors.LoggerDeclarationVisitor;
import net.odyssi.log4jb.actions.visitors.LoggerImportVisitor;
import net.odyssi.log4jb.actions.visitors.SystemErrReplacementVisitor;
import net.odyssi.log4jb.actions.visitors.SystemOutReplacementVisitor;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

/**
 * The Action used to replace calls to System.err.println with logging statements
 */
public class SystemErrReplacementAction extends AbstractLoggingAction {


	@Override
	public void actionPerformed(@NotNull AnActionEvent e) {
		Project project = e.getProject();
		Editor editor = e.getRequiredData(CommonDataKeys.EDITOR);
		PsiFile psiFile = e.getRequiredData(CommonDataKeys.PSI_FILE);

		Document document = editor.getDocument();
		PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(document);
		PsiClass selectedClass = getSelectedCursorClass(project, editor);

		PsiElementFactory factory = PsiElementFactory.getInstance(project);
		PsiMethod method = getSelectedCursorMethod(project, editor);

		WriteCommandAction.runWriteCommandAction(project, () -> {
			LoggerImportVisitor importVisitor = new LoggerImportVisitor((PsiJavaFile) selectedClass.getContainingFile(), Arrays.asList(classImports));
			LoggerDeclarationVisitor declarationVisitor = new LoggerDeclarationVisitor(selectedClass);
			SystemErrReplacementVisitor systemErrVisitor = new SystemErrReplacementVisitor(method);

			selectedClass.accept(declarationVisitor);
			psiFile.accept(importVisitor);
			psiFile.accept(systemErrVisitor);

			CodeStyleManager.getInstance(project).reformat(psiFile);
			PsiDocumentManager.getInstance(project).commitDocument(document);
		});
	}


	@Override
	public void update(@NotNull AnActionEvent e) {
		// Check if a Java class is selected
		boolean isEnabled = isJavaMethodSelected(e);

		// Enable or disable the action
		e.getPresentation().setEnabled(isEnabled);
	}
}
