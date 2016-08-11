package org.vadere.gui.postvisualization.control;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.vadere.gui.postvisualization.PostVisualisation;
import org.vadere.util.io.IOUtils;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

public class ActionCloseSettingDialog extends AbstractAction {
	private static Logger logger = LogManager.getLogger(ActionCloseSettingDialog.class);
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
