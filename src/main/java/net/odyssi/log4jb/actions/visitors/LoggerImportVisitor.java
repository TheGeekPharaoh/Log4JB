package net.odyssi.log4jb.actions.visitors;

import com.intellij.psi.*;
import net.odyssi.log4jb.actions.LogMethodAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A {@link org.intellij.markdown.ast.visitors.Visitor} used to import the logger class in the selected class
 *
 */
public class LoggerImportVisitor extends JavaRecursiveElementVisitor {

	private static final Logger logger = LoggerFactory.getLogger(LoggerImportVisitor.class);

	private final PsiJavaFile file;

	private final List<String> importStatements;

	private final Set<String> existingImports = new HashSet<>();

	public LoggerImportVisitor(PsiJavaFile file, List<String> importStatements) {
		this.file = file;
		this.importStatements = importStatements;
	}

	@Override
	public void visitImportStatement(PsiImportStatement element) {
		if (logger.isDebugEnabled()) {
			logger.debug("visitImportStatement(PsiImportStatement) - start");
		}

		super.visitImportStatement(element);

		existingImports.add(element.getImportReference().getQualifiedName());
		
		if (logger.isDebugEnabled()) {
			logger.debug("visitImportStatement(PsiImportStatement) - end");
		}
	}

	@Override
	public void visitFile(PsiFile file) {
		if (logger.isDebugEnabled()) {
			logger.debug("visitFile(PsiFile) - start");
		}
		super.visitFile(file);
		for (String importStatement : importStatements) {
			if (!existingImports.contains(importStatement)) {
				addImportStatement(importStatement);
			}
		}
		if (logger.isDebugEnabled()) {
			logger.debug("visitFile(PsiFile) - end");
		}
	}

	private void addImportStatement(String importStatement) {
		if (logger.isDebugEnabled()) {
			logger.debug("addImportStatement(String) - start");
		}
		PsiClass psiClass = JavaPsiFacade.getInstance(file.getProject()).findClass(importStatement, file.getResolveScope());
		if (psiClass != null) {
			PsiImportStatement statement = PsiElementFactory.SERVICE.getInstance(file.getProject())
					.createImportStatement(psiClass);
			((PsiJavaFile) file).getImportList().add(statement);
		}
		if (logger.isDebugEnabled()) {
			logger.debug("addImportStatement(String) - end");
		}
	}
}