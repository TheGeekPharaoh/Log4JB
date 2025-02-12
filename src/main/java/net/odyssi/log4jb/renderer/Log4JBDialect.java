package net.odyssi.log4jb.renderer;

import net.odyssi.log4jb.dialogs.forms.GenericLogModel;

import java.util.List;

/**
 * Classes implementing this interface provide a dialect for the plugin, allowing multiple log frameworks to be supported
 *
 * @author sdnakhla
 */
public interface Log4JBDialect {

	/**
	 * Returns the String used to declare a logger variable within a class
	 *
	 * @return The logger declaration statement
	 */
	String getLoggerDeclarationStatement();

	/**
	 * Returns a {@link List} of the imports needed to declare and use the logger
	 *
	 * @return The logger imports
	 */
	List<String> getLoggerImports();

	/**
	 * Returns the String used to log the start of a method
	 *
	 * @param methodName The method name
	 * @param logLevel   The log level
	 * @param args       The optional argument types for the method
	 * @return The log statement
	 */
	String getMethodStartLogStatement(String methodName, String logLevel, String... args);

	/**
	 * Returns the String used to log the end of a method
	 *
	 * @param methodName The method name
	 * @param logLevel   The log level
	 * @param args       The optional argument types for the method
	 * @return The log statement
	 */
	String getMethodEndLogStatement(String methodName, String logLevel, String... args);

	/**
	 * Returns the String used to log an {@link Exception} at error level
	 *
	 * @param methodName            The method name
	 * @param exceptionVariableName The exception variable name
	 * @param methodArgs            The optional argument types for the method
	 * @return The log statement
	 */
	String getExceptionErrorLogStatement(String methodName, String exceptionVariableName, String... methodArgs);

	/**
	 * Returns the String used to log an {@link Exception} at warn level
	 *
	 * @param methodName            The method name
	 * @param exceptionVariableName The exception variable name
	 * @param methodArgs            The optional argument types for the method
	 * @return The log statement
	 */
	String getExceptionWarnLogStatement(String methodName, String exceptionVariableName, String... methodArgs);

	/**
	 * Returns the String used to log the specified variable name
	 *
	 * @param methodName   The method name
	 * @param variableName The logged variable name
	 * @param logLevel     The log level
	 * @param methodArgs   The optional argument types for the method
	 * @return The log statement
	 */
	String getVariableLogStatement(String methodName, String variableName, String logLevel, String... methodArgs);

	/**
	 * Returns the String used to log a user-specified message
	 *
	 * @param methodName The method name
	 * @param model      The log model
	 * @param methodArgs The optional argument types for the method
	 * @return The log statement
	 */
	String getLogStatement(String methodName, GenericLogModel model, String... methodArgs);

	/**
	 * Returns the template used to guard a log statement
	 *
	 * @param logLevel The log level
	 * @return The log guard template
	 */
	String getLogGuardTemplate(String logLevel);

}
