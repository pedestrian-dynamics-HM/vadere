package org.vadere.gui.projectview.control;


import org.vadere.gui.projectview.model.ProjectViewModel;
import org.vadere.gui.projectview.view.VDialogManager;
import org.vadere.util.logging.Logger;

import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.prefs.BackingStoreException;

import javax.swing.*;

public class ActionCloseApplication extends AbstractAction {

	private static final long serialVersionUID = 1L;
	private static Logger logger = Logger.getLogger(ActionCloseApplication.class);

	private ProjectViewModel model;

	public ActionCloseApplication(final String name, final ProjectViewModel model) {
		super(name);
		this.model = model;
	}

	@Override
	public void actionPerformed(final ActionEvent event) {
		logger.info(String.format("closing application..."));
		try {
			if (!VDialogManager.continueSavingDespitePossibleJsonError())
				return;
			if (!ActionAbstractSaveProject.askSaveUnlessUserCancels(model))
				return;

		} catch (IOException e) {
			e.printStackTrace();
		}
		finally {
			System.exit(0);
		}
	}
}
