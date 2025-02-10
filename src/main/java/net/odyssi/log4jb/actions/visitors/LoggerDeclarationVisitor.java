package net.odyssi.log4jb.actions.visitors;

import com.intellij.openapi.util.Key;
import com.intellij.psi.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggerDeclarationVisitor extends JavaElementVisitor {

	private static final Key<Boolean> LOGGER_FIELD_EXISTS = Key.create("LoggerDeclarationVisitor");

	private static final Logger logger = LoggerFactory.getLogger(LoggerDeclarationVisitor.class);

	private final PsiClass psiClass;

	private boolean loggerExists = false;

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

			if (logger.isDebugEnabled()) {
				logger.debug("visitField(PsiField)  - Logger exists on class - psiClass={}", psiClass);
			}
			psiClass.putUserData(LOGGER_FIELD_EXISTS, true);
		} else {
			if (logger.isDebugEnabled()) {
				logger.debug("visitField(PsiField)  - Logger does not exist on class - psiClass={}", psiClass);
			}
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

		for (PsiField field : aClass.getFields()) {
			visitField(field);
		}

		if (loggerExists) {
			// Do nothing
		} else {
			this.addLoggerFieldIfNotExists();
		}

		if (logger.isDebugEnabled()) {
			logger.debug("visitClass(PsiClass) - end");
		}
	}

	public void addLoggerFieldIfNotExists() {
		if (logger.isDebugEnabled()) {
			logger.debug("addLoggerFieldIfNotExists() - start");
		}


		if ((psiClass.getUserData(LOGGER_FIELD_EXISTS) != null) && (!(Boolean) psiClass.getUserData(LOGGER_FIELD_EXISTS))) {
			if (logger.isDebugEnabled()) {
				logger.debug("addLoggerFieldIfNotExists()  - Logger already defined.  Skipping... - psiClass={}", psiClass);
			}
		} else {
			if (logger.isDebugEnabled()) {
				logger.debug("addLoggerFieldIfNotExists()  - Adding logger to class... - psiClass={}", psiClass);
			}
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