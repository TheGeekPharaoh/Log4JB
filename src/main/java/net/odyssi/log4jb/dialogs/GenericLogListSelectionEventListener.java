package net.odyssi.log4jb.dialogs;

import com.intellij.openapi.ListSelection;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Listens for list selection events and updates appropriate tracking values
 */
public class GenericLogListSelectionEventListener implements ListSelectionListener {

	private Set<String[]> availableVariables = null;

	private Set<String> selectedVariables = null;

	public GenericLogListSelectionEventListener(Set<String[]> availableVariables, Set<String> selectedVariables) {
		this.availableVariables = availableVariables;
		this.selectedVariables = selectedVariables;
	}

	/**
	 * Called whenever the value of the selection changes.
	 *
	 * @param e the event that characterizes the change.
	 */
	@Override
	public void valueChanged(ListSelectionEvent e) {
		if(this.selectedVariables != null && this.availableVariables != null) {
			String[] availableVariablesArr = this.availableVariables.stream().map(v -> v[0]).toArray(String[]::new);
			this.selectedVariables.clear();

			ListSelectionModel model = (ListSelectionModel) e.getSource();
			if(!model.isSelectionEmpty()) {
				int minIndex = model.getMinSelectionIndex();
				int maxIndex = model.getMaxSelectionIndex();

				for(int i=minIndex; i <= maxIndex; i++) {
					if(model.isSelectedIndex(i)) {
						this.selectedVariables.add(availableVariablesArr[i]);
					}
				}
			}
		}
	}
}
