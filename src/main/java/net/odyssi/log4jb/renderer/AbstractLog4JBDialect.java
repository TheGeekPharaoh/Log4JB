package net.odyssi.log4jb.renderer;

/**
 * The abstract base class for a {@link Log4JBDialect}
 *
 * @author sdnakhla 
 */
public abstract class AbstractLog4JBDialect implements Log4JBDialect {

	private static final String SIGNATURE_TEMPLATE = "%s(%s)";

	/**
	 * Returns the formatted method signature string
	 * @param methodName The method name
	 * @param args The optional method parameters
	 * @return The method signature string
	 */
	public String getMethodSignature(String methodName, String... args) {
		String methodParams;
		if(args != null && args.length > 0) {
			methodParams = String.join(",", args);
		} else {
			methodParams = "";
		}

		String signature = SIGNATURE_TEMPLATE.formatted(methodName, methodParams);

		return signature;
	}
}
