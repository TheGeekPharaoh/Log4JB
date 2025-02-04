package net.odyssi.log4jb.actions.visitors;

import com.intellij.psi.*;

import java.util.List;

/**
 * A {@link org.intellij.markdown.ast.visitors.Visitor} used to import the logger class in the selected class
 *
 */
public class LoggerImportVisitor extends JavaRecursiveElementVisitor {

	private final PsiJavaFile file;

	private final List<String> importStatements;

	public LoggerImportVisitor(PsiJavaFile file, List<String> importStatements) {
		this.file = file;
		this.importStatements = importStatements;
	}

	@Override
	public void visitImportStatement(PsiImportStatement element) {
		super.visitImportStatement(element);
		importStatements.remove(element.getImportReference().getQualifiedName());
	}

	@Override
	public void visitFile(PsiFile file) {
		super.visitFile(file);
		for (String importStatement : importStatements) {
			addImportStatement(importStatement);
		}
	}

	private void addImportStatement(String importStatement) {
		PsiClass psiClass = JavaPsiFacade.getInstance(file.getProject()).findClass(importStatement, file.getResolveScope());
		if (psiClass != null) {
			PsiImportStatement statement = PsiElementFactory.SERVICE.getInstance(file.getProject())
					.createImportStatement(psiClass);
			((PsiJavaFile) file).getImportList().add(statement);
		}
	}
}