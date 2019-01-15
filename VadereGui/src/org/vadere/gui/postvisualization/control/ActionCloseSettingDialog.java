package org.vadere.gui.postvisualization.control;


import org.vadere.gui.postvisualization.PostVisualisation;
import org.vadere.util.io.IOUtils;
import org.vadere.util.logging.Logger;

import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.swing.*;

public class ActionCloseSettingDialog extends AbstractAction {
	private static Logger logger = Logger.getLogger(ActionCloseSettingDialog.class);
	private final JDialog dialog;

	public ActionCloseSettingDialog(final JDialog dialog) {
		this.dialog = dialog;
	}

	@Override
	public void actionPerformed(ActionEvent e) {

		try {
			IOUtils.saveUserPreferences(PostVisualisation.preferencesFilename,
					Preferences.userNodeForPackage(PostVisualisation.class));
		} catch (IOException | BackingStoreException e1) {
			e1.printStackTrace();
		}

		dialog.setVisible(false);
	}

}
