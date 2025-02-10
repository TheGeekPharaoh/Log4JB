package net.odyssi.log4jb.dialogs.forms;

import java.util.*;

/**
 * The model for a generic log operation, outlining what should be written in the log statement
 *
 * @author sdnakhla
 */
public class GenericLogModel {

	private String logMessage = null;

	private String logLevel = null;

	private Set<String> selectedGlobalVariables = new LinkedHashSet<>();

	private Set<String> selectedMethodParameters = new LinkedHashSet<>();

	private Set<String> selectedLocalVariables = new LinkedHashSet<>();

	public GenericLogModel() {
	}

	public GenericLogModel(String logMessage, String logLevel, Set<String> selectedGlobalVariables, Set<String> selectedMethodParameters, Set<String> selectedLocalVariables) {
		this.logMessage = logMessage;
		this.logLevel = logLevel;
		this.selectedGlobalVariables = selectedGlobalVariables;
		this.selectedMethodParameters = selectedMethodParameters;
		this.selectedLocalVariables = selectedLocalVariables;
	}

	public String getLogMessage() {
		return logMessage;
	}

	public void setLogMessage(String logMessage) {
		this.logMessage = logMessage;
	}

	public String getLogLevel() {
		return logLevel;
	}

	public void setLogLevel(String logLevel) {
		this.logLevel = logLevel;
	}

	public Set<String> getSelectedGlobalVariables() {
		return selectedGlobalVariables;
	}

	public void setSelectedGlobalVariables(Set<String> selectedGlobalVariables) {
		this.selectedGlobalVariables = selectedGlobalVariables;
	}

	public Set<String> getSelectedMethodParameters() {
		return selectedMethodParameters;
	}

	public void setSelectedMethodParameters(Set<String> selectedMethodParameters) {
		this.selectedMethodParameters = selectedMethodParameters;
	}

	public Set<String> getSelectedLocalVariables() {
		return selectedLocalVariables;
	}

	public void setSelectedLocalVariables(Set<String> selectedLocalVariables) {
		this.selectedLocalVariables = selectedLocalVariables;
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || getClass() != o.getClass()) return false;
		GenericLogModel that = (GenericLogModel) o;
		return Objects.equals(logMessage, that.logMessage) && Objects.equals(logLevel, that.logLevel) && Objects.equals(selectedGlobalVariables, that.selectedGlobalVariables) && Objects.equals(selectedMethodParameters, that.selectedMethodParameters) && Objects.equals(selectedLocalVariables, that.selectedLocalVariables);
	}

	@Override
	public int hashCode() {
		return Objects.hash(logMessage, logLevel, selectedGlobalVariables, selectedMethodParameters, selectedLocalVariables);
	}

	@Override
	public String toString() {
		return "GenericLogModel{" +
				"logMessage='" + logMessage + '\'' +
				", logLevel='" + logLevel + '\'' +
				", selectedGlobalVariables=" + selectedGlobalVariables +
				", selectedMethodParameters=" + selectedMethodParameters +
				", selectedLocalVariables=" + selectedLocalVariables +
				'}';
	}
}
