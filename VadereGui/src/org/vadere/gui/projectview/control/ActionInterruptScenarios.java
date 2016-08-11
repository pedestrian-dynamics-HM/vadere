package org.vadere.gui.projectview.control;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.vadere.gui.projectview.model.ProjectViewModel;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class ActionInterruptScenarios extends AbstractAction {

	private static Logger logger = LogManager.getLogger(ActionInterruptScenarios.class);

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
