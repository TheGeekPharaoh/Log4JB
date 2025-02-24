package net.odyssi.log4jb.dialogs.forms;

import javax.swing.table.DefaultTableModel;

/**
 * A read-only model for {@link javax.swing.JTable} and {@link com.intellij.ui.table.JBTable}
 */
public class ReadOnlyTableModel extends DefaultTableModel {
	/**
	 * Returns true regardless of parameter values.
	 *
	 * @param row    the row whose value is to be queried
	 * @param column the column whose value is to be queried
	 * @return true
	 * @see #setValueAt
	 */
	@Override
	public boolean isCellEditable(int row, int column) {
		return false;
	}
}
