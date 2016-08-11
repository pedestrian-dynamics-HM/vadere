package org.vadere.gui.projectview;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.vadere.gui.projectview.view.ProjectView;
import org.vadere.util.io.IOUtils;

import java.io.IOException;
import java.util.prefs.BackingStoreException;
import java.util.prefs.InvalidPreferencesFormatException;
import java.util.prefs.Preferences;

/**
 * Entry point for the Vadere GUI.
 * 
 * 
 */
public class VadereApplication {
	public static final String preferencesFilename = "VadereTestingSuite.preferences.xml";
	private static Logger logger = LogManager.getLogger(VadereApplication.class);

	public static void main(String[] args) {
		logger.info("starting Vadere GUI...");
		// load settings
		loadPreferences();

		// start main gui
		ProjectView.start();
	}

	/**
	 * Load the preferences from file.
	 */
	private static void loadPreferences() {
		Preferences prefs = null;
		try {
			prefs = IOUtils.loadUserPreferences(preferencesFilename, VadereApplication.class);
		} catch (IOException | InvalidPreferencesFormatException e) {
			logger.error("preferences file not found or corrupted. creating a new file...");
			prefs = Preferences.userNodeForPackage(VadereApplication.class);

			defaultPreferences(prefs);
			try {
				IOUtils.saveUserPreferences(preferencesFilename, prefs);
			} catch (IOException | BackingStoreException e1) {
				logger.error("preferences file could not be written.");
			}
		}
	}

	/**
	 * Set default preferences.
	 * 
	 * @param prefs
	 */
	private static void defaultPreferences(Preferences prefs) {
		prefs.put("default_directory", System.getProperty("user.dir") + "/projects");
		prefs.put("default_directory_attributes", System.getProperty("user.dir") + "/attributes");
		prefs.put("default_directory_scenarios", System.getProperty("user.dir") + "/scenarios");
	}
}
