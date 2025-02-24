package net.odyssi.log4jb.dialogs;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import net.odyssi.log4jb.dialogs.forms.GenericLogForm;
import net.odyssi.log4jb.dialogs.forms.GenericLogModel;
import net.odyssi.log4jb.dialogs.forms.ReadOnlyTableModel;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.util.LinkedHashSet;
import java.util.Set;

public class GenericLogFormDialog extends DialogWrapper {

	private static final Logger logger = LoggerFactory.getLogger(GenericLogFormDialog.class);
	private GenericLogForm form = null;

	private Set<String[]> globalVariables = null;

	private Set<String[]> localVariables = null;

	private Set<String[]> methodParameters = null;

	private final Set<String> selectedGlobalVariables = new LinkedHashSet<>();

	private final Set<String> selectedMethodParameters = new LinkedHashSet<>();

	private final Set<String> selectedLocalVariables = new LinkedHashSet<>();

	public GenericLogFormDialog(@Nullable Project project, Set<String[]> globalVariables, Set<String[]> localVariables, Set<String[]> methodParameters) {
		super(project);

		this.globalVariables = globalVariables;
		this.localVariables = localVariables;
		this.methodParameters = methodParameters;

		setTitle("Log at this position...");
		setSize(800, 600);

		this.form = new GenericLogForm();

		this.initTableModels();
		init();
	}

	protected void initTableModels() {
		if (logger.isDebugEnabled()) {
			logger.debug("initTableModels() - start");
		}
		String[] columns = new String[]{"Name", "Type"};

		DefaultTableModel globalVariablesModel = new ReadOnlyTableModel();
		DefaultTableModel localVariablesModel = new ReadOnlyTableModel();
		DefaultTableModel methodParametersModel = new ReadOnlyTableModel();

		DefaultTableModel[] models = new DefaultTableModel[]{globalVariablesModel, localVariablesModel, methodParametersModel};
		for (DefaultTableModel model : models) {
			for (String column : columns) {
				model.addColumn(column);
			}
		}

		if (this.globalVariables != null) {
			for (String[] row : this.globalVariables) {
				globalVariablesModel.addRow(row);
			}
		}

		if (this.localVariables != null) {
			for (String[] row : this.localVariables) {
				localVariablesModel.addRow(row);
			}
		}

		if (this.methodParameters != null) {
			for (String[] row : this.methodParameters) {
				methodParametersModel.addRow(row);
			}
		}

		this.form.getGlobalVariablesTable().setModel(globalVariablesModel);
		this.form.getGlobalVariablesTable().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		this.form.getGlobalVariablesTable().getSelectionModel().addListSelectionListener(new GenericLogListSelectionEventListener(this.globalVariables, this.selectedGlobalVariables));

		this.form.getMethodParametersTable().setModel(methodParametersModel);
		this.form.getMethodParametersTable().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		this.form.getMethodParametersTable().getSelectionModel().addListSelectionListener(new GenericLogListSelectionEventListener(this.methodParameters, this.selectedMethodParameters));

		this.form.getLocalVariablesTable().setModel(localVariablesModel);
		this.form.getLocalVariablesTable().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		this.form.getLocalVariablesTable().getSelectionModel().addListSelectionListener(new GenericLogListSelectionEventListener(this.localVariables, this.selectedLocalVariables));
		if (logger.isDebugEnabled()) {
			logger.debug("initTableModels() - end");
		}
	}

	/**
	 * Factory method. It creates panel with dialog options. Options panel is located at the
	 * center of the dialog's content pane. The implementation can return {@code null}
	 * value. In this case there will be no options panel.
	 */
	@Override
	protected @Nullable JComponent createCenterPanel() {
		if (logger.isDebugEnabled()) {
			logger.debug("createCenterPanel() - start");
		}

		if (logger.isDebugEnabled()) {
			logger.debug("createCenterPanel() - end");
		}
		return this.form.getContentPanel();
	}

	/**
	 * Builds and returns the {@link GenericLogModel} based off the contents of the completed form
	 * @return The log model
	 */
	public GenericLogModel buildLogModel() {
		if (logger.isDebugEnabled()) {
			logger.debug("buildLogModel() - start");
		}
		GenericLogModel model = new GenericLogModel();
		model.setLogLevel((String) this.form.getLogLevel().getModel().getSelectedItem());
		model.setLogMessage(this.form.getLogMessage().getText());
		model.setSelectedGlobalVariables(this.selectedGlobalVariables);
		model.setSelectedLocalVariables(this.selectedLocalVariables);
		model.setSelectedMethodParameters(this.selectedMethodParameters);

		if (logger.isDebugEnabled()) {
			logger.debug("buildLogModel() - end");
		}
		return model;
	}
}
