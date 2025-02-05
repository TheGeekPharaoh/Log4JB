package net.odyssi.log4jb.actions.visitors;

import com.intellij.psi.*;
import com.intellij.openapi.util.Key;

public class LoggerDeclarationVisitor extends JavaElementVisitor {

	private static final Key<Boolean> LOGGER_FIELD_EXISTS = Key.create("LoggerDeclarationVisitor");
	private final PsiClass psiClass;

	public LoggerDeclarationVisitor(PsiClass psiClass) {
		this.psiClass = psiClass;
	}

	@Override
	public void visitField(PsiField field) {
		super.visitField(field);
		if (field.getName().equals("logger") && field.getType().equalsToText("Logger")) {
			psiClass.putUserData(LOGGER_FIELD_EXISTS, true);
		}
	}

	@Override
	public void visitClass(PsiClass aClass) {
		super.visitClass(aClass);
		// Do nothing here
	}

	public void addLoggerFieldIfNotExists() {
		if (!(Boolean) psiClass.getUserData(LOGGER_FIELD_EXISTS)) {
			addLoggerField();
		}
	}

	private void addLoggerField() {
		PsiElementFactory factory = JavaPsiFacade.getElementFactory(psiClass.getProject());
		PsiField loggerField = factory.createFieldFromText("private final static Logger logger = LoggerFactory.getLogger(" + psiClass.getQualifiedName() + ".class);", psiClass);
		psiClass.add(loggerField);
	}
}