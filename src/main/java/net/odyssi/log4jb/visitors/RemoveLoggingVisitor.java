package net.odyssi.log4jb.visitors;

import com.intellij.psi.*;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.Query;

/**
 * A PSI visitor that removes all SLF4J logging artifacts from a class.
 * <p>
 * This visitor finds and deletes:
 * <ul>
 *     <li>The {@code logger} field declaration.</li>
 *     <li>All statements that use the logger (e.g., {@code logger.debug(...)}).</li>
 *     <li>Any {@code if} statements that guard logger calls (e.g., {@code if(logger.isDebugEnabled())}).</li>
 * </ul>
 * Note: This visitor does not remove imports, as this is best handled by the IDE's "Optimize Imports" feature
 * after the code modifications are complete.
 */
public class RemoveLoggingVisitor extends JavaRecursiveElementVisitor {

    @Override
    public void visitClass(PsiClass aClass) {
        super.visitClass(aClass);

        final PsiField loggerField = aClass.findFieldByName("logger", false);
        if (loggerField == null) {
            return;
        }

        // Find all references to the logger field within the scope of the class.
        final Query<PsiReference> loggerReferences = ReferencesSearch.search(loggerField, aClass.getUseScope());

        for (PsiReference reference : loggerReferences) {
            final PsiElement element = reference.getElement();

            // Find the enclosing statement for the logger usage.
            PsiStatement statement = PsiTreeUtil.getParentOfType(element, PsiStatement.class);
            if (statement == null) {
                continue;
            }

            // If the statement is an `if` block (a guard condition), we delete the whole block.
            // Otherwise, we assume it's a simple expression statement like `logger.error(...)`.
            // In either case, the `statement` is the element to delete.
            // We check for `if` specifically to handle cases where a logger call might be nested.
            // The top-most statement containing the reference is what we want.

            PsiElement elementToDelete = statement;

            // Check if the parent is the `then` branch of an `if` statement whose condition also uses the logger.
            if (statement.getParent() instanceof PsiCodeBlock && statement.getParent().getParent() instanceof PsiIfStatement ifStatement) {
                if (ifStatement.getCondition() != null && ifStatement.getCondition().getText().contains("logger.")) {
                    elementToDelete = ifStatement;
                }
            }

            if (elementToDelete.isValid()) {
                elementToDelete.delete();
            }
        }

        // Finally, delete the logger field itself.
        if (loggerField.isValid()) {
            loggerField.delete();
        }
    }
}