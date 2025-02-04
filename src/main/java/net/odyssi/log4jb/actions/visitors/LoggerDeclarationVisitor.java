package net.odyssi.log4jb.actions.visitors;

import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

public class LoggerDeclarationVisitor extends JavaElementVisitor {
	private final PsiClass psiClass;
	private boolean loggerFieldExists = false;

	public LoggerDeclarationVisitor(PsiClass psiClass) {
		this.psiClass = psiClass;
	}

	@Override
	public void visitField(PsiField field) {
		super.visitField(field);
		if (field.getName().equals("logger") && field.getType().equalsToText("Logger")) {
			loggerFieldExists = true;
		}
	}

	@Override
	public void visitClass(PsiClass aClass) {
		super.visitClass(aClass);
		if (!loggerFieldExists) {
			addLoggerField();
		}
	}

	private void addLoggerField() {
		PsiElementFactory factory = JavaPsiFacade.getElementFactory(psiClass.getProject());
		PsiField loggerField = factory.createFieldFromText("private final static Logger logger = LoggerFactory.getLogger(" + psiClass.getQualifiedName() + ".class);", psiClass);
		psiClass.add(loggerField);
	}
}