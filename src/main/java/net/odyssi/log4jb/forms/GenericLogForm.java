package net.odyssi.log4jb.forms;

import javax.swing.*;

public class GenericLogForm {
	private JPanel contentPanel;
	private JTextField logMessage;
	private JComboBox logLevel;
	private JTable globalVariablesTable;
	private JTable methodParametersTable;
	private JTable availableExceptionsTable;
	private JTable localVariablesTable;

	public JTextField getLogMessage() {
		return logMessage;
	}

	public void setLogMessage(JTextField logMessage) {
		this.logMessage = logMessage;
	}

	public JComboBox getLogLevel() {
		return logLevel;
	}

	public void setLogLevel(JComboBox logLevel) {
		this.logLevel = logLevel;
	}

	public JTable getGlobalVariablesTable() {
		return globalVariablesTable;
	}

	public void setGlobalVariablesTable(JTable globalVariablesTable) {
		this.globalVariablesTable = globalVariablesTable;
	}

	public JTable getMethodParametersTable() {
		return methodParametersTable;
	}

	public void setMethodParametersTable(JTable methodParametersTable) {
		this.methodParametersTable = methodParametersTable;
	}

	public JTable getAvailableExceptionsTable() {
		return availableExceptionsTable;
	}

	public void setAvailableExceptionsTable(JTable availableExceptionsTable) {
		this.availableExceptionsTable = availableExceptionsTable;
	}

	public JTable getLocalVariablesTable() {
		return localVariablesTable;
	}

	public void setLocalVariablesTable(JTable localVariablesTable) {
		this.localVariablesTable = localVariablesTable;
	}

	public JPanel getContentPanel() {
		return contentPanel;
	}

	public void setContentPanel(JPanel contentPanel) {
		this.contentPanel = contentPanel;
	}

}
