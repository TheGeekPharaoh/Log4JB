package net.odyssi.log4jb.util;

import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiType;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Shared utility for building method signature strings used in log messages.
 * <p>
 * Example output: {@code "processOrder(String,int,List)"}
 */
public final class MethodSignatureBuilder {

    private MethodSignatureBuilder() {
        // Utility class
    }

    /**
     * Builds a human-readable method signature from a PSI method.
     *
     * @param method The PSI method to build a signature for
     * @return A string like "methodName(ParamType1,ParamType2)"
     */
    public static String build(PsiMethod method) {
        if (method == null) {
            return "unknown()";
        }
        final String methodName = method.getName();
        final String parameterTypes = Arrays.stream(method.getParameterList().getParameters())
                .map(PsiParameter::getType)
                .map(PsiType::getPresentableText)
                .collect(Collectors.joining(","));

        return String.format("%s(%s)", methodName, parameterTypes);
    }
}
