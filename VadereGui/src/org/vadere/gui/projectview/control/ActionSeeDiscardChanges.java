package org.vadere.gui.projectview.control;


import org.vadere.gui.components.utils.Messages;
import org.vadere.gui.projectview.model.ProjectViewModel;
import org.vadere.gui.projectview.view.ProjectView;
import org.vadere.gui.projectview.view.VDialogManager;
import org.vadere.gui.projectview.view.VTable;
import org.vadere.util.logging.Logger;

import java.awt.event.ActionEvent;

import javax.swing.*;

public class ActionSeeDiscardChanges extends AbstractAction {

	private static Logger logger = Logger.getLogger(ActionSeeDiscardChanges.class);

	private final ProjectViewModel model;
	private final VTable scenarioTable;

	public ActionSeeDiscardChanges(final String name, final ProjectViewModel model, final VTable scenarioTable) {
		super(name);
		this.model = model;
		this.scenarioTable = scenarioTable;
	}

	@Override
	public void actionPerformed(final ActionEvent event) {
		int[] selectedRows = scenarioTable.getSelectedRows();

		Object[] options = {
				Messages.getString("ActionSeeDiscardChanges.saveChanges.title"),
				Messages.getString("ActionSeeDiscardChanges.discardChanges.title"),
				Messages.getString("ActionSeeDiscardChanges.cancel.title")
		};

		String singularPlural = "singular";
		if (selectedRows.length > 1)
			singularPlural = "plural";

		int choice = JOptionPane.showOptionDialog(
				ProjectView.getMainWindow(),
				VDialogManager.getPanelWithBodyAndTextArea("<html>" +
						Messages.getString("ActionSeeDiscardChanges.changes." + singularPlural + ".text")
								.replace("%VAR%", String.valueOf(selectedRows.length))
						+
						":<br><br></html>", model.getDiffOfSelectedScenarios(selectedRows)),
				Messages.getString("ActionSeeDiscardChanges.popup.title"),
				JOptionPane.YES_NO_CANCEL_OPTION,
				JOptionPane.INFORMATION_MESSAGE, null, options, options[2]);

		if (choice == JOptionPane.YES_OPTION)
			model.saveSelectedScenarios(selectedRows);

		if (choice == JOptionPane.NO_OPTION)
			model.discardChangesOfSelectedScenarios(selectedRows);

		model.refreshScenarioNames();
	}

}
