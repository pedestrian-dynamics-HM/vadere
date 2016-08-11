package org.vadere.gui.projectview.control;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.vadere.gui.components.utils.Messages;
import org.vadere.gui.projectview.model.ProjectViewModel;
import org.vadere.gui.projectview.view.VTable;
import org.vadere.util.io.IOUtils;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class ActionDeleteScenarios extends AbstractAction {

	private static Logger logger = LogManager.getLogger(ActionDeleteScenarios.class);

	private ProjectViewModel model;
	private final VTable scenarioTable;

	public ActionDeleteScenarios(final String name, final ProjectViewModel model, final VTable scenarioTable) {
		super(name);
		this.model = model;
		this.scenarioTable = scenarioTable;
	}

	@Override
	public void actionPerformed(ActionEvent arg0) {
		if (IOUtils.chooseYesNoCancel(
				scenarioTable.getSelectedRowCount() > 1 ? Messages.getString("DeleteTestQuestionMultiple.text")
						: Messages.getString("DeleteTestQuestionOne.text"),
				Messages.getString("DeleteTestQuestion.title")) == JOptionPane.YES_OPTION) {
			model.deleteScenarios(scenarioTable.getSelectedRows());
		}
	}
}
