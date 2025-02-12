package net.odyssi.log4jb.renderer;

import net.odyssi.log4jb.dialogs.forms.GenericLogModel;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The {@link Log4JBDialect} supporting the SLF4J framework
 *
 * @author sdnakhla
 */
public class SLF4JLog4JBDialect extends AbstractLog4JBDialect {

	private static final String LOGGER_DECLARATION_TEMPLATE = "private final static Logger logger = LoggerFactory.getLogger(%s.class);";

	private static final String[] LOGGER_IMPORTS = new String[]{"org.slf4j.Logger", "org.slf4j.LoggerFactory"};

	private static final String LOG_GUARD_TEMPLATE = "if(%s.is%sEnabled()) {\n	%s\n}";

	private static final String METHOD_START_SUFFIX = "start";

	private static final String METHOD_END_SUFFIX = "end";

	private static final String LOGGER_NAME = "logger";

	private static final String METHOD_START_END_TEMPLATE = "%s.%s(\"%s - %s\");";

	private static final String EXCEPTION_LOG_STATEMENT_TEMPLATE = "%s.%s(\"%s\", %s);";

	private static final String VARIABLE_LOG_TEMPLATE = "%s.%s(\"%s - %s %s={}\", %s);";

	private static final String GENERIC_LOG_TEMPLATE = "%s.%s(\"%s %s%s\"%s);";

	/**
	 * Returns the String used to declare a logger variable within a class
	 *
	 * @param className The class name
	 * @return The logger declaration statement
	 */
	@Override
	public String getLoggerDeclarationStatement(String className) {
		String statement = String.format(LOGGER_DECLARATION_TEMPLATE, className);

		return statement;
	}

	/**
	 * Returns a {@link List} of the imports needed to declare and use the logger
	 *
	 * @return The logger imports
	 */
	@Override
	public List<String> getLoggerImports() {
		List<String> imports = Arrays.asList(LOGGER_IMPORTS);

		return imports;
	}

	/**
	 * Returns the String used to log the start of a method
	 *
	 * @param methodName The method name
	 * @param logLevel   The log level
	 * @param args       The optional argument types for the method
	 * @return The log statement
	 */
	@Override
	public String getMethodStartLogStatement(String methodName, String logLevel, String... args) {
		String methodSignature = getMethodSignature(methodName, args);
		String statement = METHOD_START_END_TEMPLATE.formatted(LOGGER_NAME, logLevel, methodSignature, METHOD_START_SUFFIX);

		return statement;
	}

	/**
	 * Returns the String used to log the end of a method
	 *
	 * @param methodName The method name
	 * @param logLevel   The log level
	 * @param args       The optional argument types for the method
	 * @return The log statement
	 */
	@Override
	public String getMethodEndLogStatement(String methodName, String logLevel, String... args) {
		String methodSignature = getMethodSignature(methodName, args);
		String statement = METHOD_START_END_TEMPLATE.formatted(LOGGER_NAME, logLevel, methodSignature, METHOD_END_SUFFIX);

		return statement;
	}

	/**
	 * Returns the String used to log an {@link Exception} at error level
	 *
	 * @param methodName            The method name
	 * @param exceptionVariableName The exception variable name
	 * @param methodArgs            The optional argument types for the method
	 * @return The log statement
	 */
	@Override
	public String getExceptionErrorLogStatement(String methodName, String exceptionVariableName, String... methodArgs) {
		String methodSignature = getMethodSignature(methodName, methodArgs);
		String logLevel = "error";

		String statement = EXCEPTION_LOG_STATEMENT_TEMPLATE.formatted(LOGGER_NAME, logLevel, methodSignature, exceptionVariableName);

		return statement;
	}

	/**
	 * Returns the String used to log an {@link Exception} at warn level
	 *
	 * @param methodName            The method name
	 * @param exceptionVariableName The exception variable name
	 * @param methodArgs            The optional argument types for the method
	 * @return The log statement
	 */
	@Override
	public String getExceptionWarnLogStatement(String methodName, String exceptionVariableName, String... methodArgs) {
		String methodSignature = getMethodSignature(methodName, methodArgs);
		String logLevel = "warn";

		String statement = EXCEPTION_LOG_STATEMENT_TEMPLATE.formatted(LOGGER_NAME, logLevel, methodSignature, exceptionVariableName);

		return statement;
	}

	/**
	 * Returns the String used to log the specified variable name
	 *
	 * @param methodName   The method name
	 * @param variableType The variable type
	 * @param variableName The logged variable name
	 * @param logLevel     The log level
	 * @param methodArgs   The optional argument types for the method
	 * @return The log statement
	 */
	@Override
	public String getVariableLogStatement(String methodName, String variableType, String variableName, String logLevel, String... methodArgs) {
		String methodSignature = getMethodSignature(methodName, methodArgs);
		String statement = VARIABLE_LOG_TEMPLATE.formatted(LOGGER_NAME, logLevel, methodSignature, variableType, variableName, variableName);

		return statement;
	}

	/**
	 * Returns the String used to log a user-specified message
	 *
	 * @param methodName The method name
	 * @param model      The log model
	 * @param methodArgs The optional argument types for the method
	 * @return The log statement
	 */
	@Override
	public String getLogStatement(String methodName, GenericLogModel model, String... methodArgs) {
		String methodSignature = getMethodSignature(methodName, methodArgs);
		String logMessage = (model.getLogMessage() != null && model.getLogMessage().length() > 0) ? " - " + model.getLogMessage() : "";
		String variableLogStatement = getVariableLogStatement(model.getSelectedGlobalVariables(), model.getSelectedLocalVariables(), model.getSelectedMethodParameters());
		String variableLogValues = getVariableLogValuesStatement(model.getSelectedGlobalVariables(), model.getSelectedLocalVariables(), model.getSelectedMethodParameters());

		String logStatementStr = GENERIC_LOG_TEMPLATE.formatted(LOGGER_NAME, LOGGER_NAME, model.getLogLevel(), methodSignature, logMessage, variableLogStatement, variableLogValues);

		return logStatementStr;
	}

	/**
	 * Builds a log statement from the given variables
	 *
	 * @param globalVariables  The global variables
	 * @param localVariables   The local variables
	 * @param methodParameters The method parameters
	 * @return The log statement
	 */
	protected String getVariableLogStatement(Set<String> globalVariables, Set<String> localVariables, Set<String> methodParameters) {
		String s = null;
		if (globalVariables.size() == 0 && localVariables.size() == 0 && methodParameters.size() == 0) {
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
	 *
	 * @param globalVariables  The global variables
	 * @param localVariables   The local variables
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
	 * Returns the template used to guard a log statement
	 *
	 * @param logLevel The log level
	 * @return The log guard template
	 */
	@Override
	public String getLogGuardTemplate(String logLevel) {
		String logLevelValue;
		switch (logLevel.toLowerCase()) {
			case "debug":
				logLevelValue = "Debug";
				break;
			case "trace":
				logLevelValue = "Trace";
				break;
			case "warn":
				logLevelValue = "Warn";
				break;
			case "error":
				logLevelValue = "Error";
				break;
			case "fatal":
				logLevelValue = "Fatal";
				break;
			default:
				logLevelValue = "Info";
				break;
		}

		String guard = String.format(LOG_GUARD_TEMPLATE, LOGGER_NAME, logLevelValue);
		return guard;
	}
}
