package org.vadere.gui.projectview.control;


import org.vadere.gui.projectview.model.ProjectViewModel;
import org.vadere.util.logging.Logger;

import java.awt.event.ActionEvent;

import javax.swing.*;

public class ActionInterruptScenarios extends AbstractAction {

	private static Logger logger = Logger.getLogger(ActionInterruptScenarios.class);

	private ProjectViewModel model;

	public ActionInterruptScenarios(final String name, final ProjectViewModel model) {
		super(name);
		this.model = model;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		model.getProject().interruptRunningScenarios();
		model.refreshOutputTable();
	}
}
