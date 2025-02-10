package net.odyssi.log4jb.actions.visitors;

import com.intellij.psi.*;
import com.intellij.openapi.util.Key;
import net.odyssi.log4jb.actions.LogMethodAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggerDeclarationVisitor extends JavaElementVisitor {

	private static final Key<Boolean> LOGGER_FIELD_EXISTS = Key.create("LoggerDeclarationVisitor");

	private static final Logger logger = LoggerFactory.getLogger(LoggerDeclarationVisitor.class);

	private final PsiClass psiClass;

	public LoggerDeclarationVisitor(PsiClass psiClass) {
		this.psiClass = psiClass;
		if (logger.isDebugEnabled()) {
			logger.debug("LoggerDeclarationVisitor(PsiClass) - end");
		}
	}

	@Override
	public void visitField(PsiField field) {
		if (logger.isDebugEnabled()) {
			logger.debug("visitField(PsiField) - start");
		}
		super.visitField(field);
		if (field.getName().equals("logger") && field.getType().equalsToText("Logger")) {
			psiClass.putUserData(LOGGER_FIELD_EXISTS, true);
		}
		if (logger.isDebugEnabled()) {
			logger.debug("visitField(PsiField) - end");
		}
	}

	@Override
	public void visitClass(PsiClass aClass) {
		if (logger.isDebugEnabled()) {
			logger.debug("visitClass(PsiClass) - start");
		}
		super.visitClass(aClass);
		// Do nothing here
		if (logger.isDebugEnabled()) {
			logger.debug("visitClass(PsiClass) - end");
		}
	}

	public void addLoggerFieldIfNotExists() {
		if (logger.isDebugEnabled()) {
			logger.debug("addLoggerFieldIfNotExists() - start");
		}
		if (!(Boolean) psiClass.getUserData(LOGGER_FIELD_EXISTS)) {
			addLoggerField();
		}
		if (logger.isDebugEnabled()) {
			logger.debug("addLoggerFieldIfNotExists() - end");
		}
	}

	private void addLoggerField() {
		if (logger.isDebugEnabled()) {
			logger.debug("addLoggerField() - start");
		}
		PsiElementFactory factory = JavaPsiFacade.getElementFactory(psiClass.getProject());
		PsiField loggerField = factory.createFieldFromText("private final static Logger logger = LoggerFactory.getLogger(" + psiClass.getQualifiedName() + ".class);", psiClass);
		psiClass.add(loggerField);
		if (logger.isDebugEnabled()) {
			logger.debug("addLoggerField() - end");
		}
	}
}