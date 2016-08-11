package org.vadere.gui.postvisualization;

import java.io.IOException;
import java.util.prefs.BackingStoreException;
import java.util.prefs.InvalidPreferencesFormatException;
import java.util.prefs.Preferences;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.vadere.gui.postvisualization.view.PostvisualizationWindow;
import org.vadere.util.io.IOUtils;

public class PostVisualisation {
	public static final String preferencesFilename = "PostVisualisation.preferences.xml";
	private static Logger logger = LogManager.getLogger(PostVisualisation.class);

	public static void main(String[] args) {
		logger.info("starting post visualization ...");
		// load settings
		loadPreferences();
		logger.info("preferences started");
		// start main gui
		PostvisualizationWindow.start();
		logger.info("post visualization started");
	}

	/**
	 * Load the preferences from file.
	 */
	private static void loadPreferences() {
		Preferences prefs = null;
		try {
			IOUtils.loadUserPreferences(preferencesFilename, PostVisualisation.class);
		} catch (IOException | InvalidPreferencesFormatException e) {
			logger.error("preferences file not found or corrupted. creating a new file...");
			prefs = Preferences.userNodeForPackage(PostVisualisation.class);

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
	private static void defaultPreferences(final Preferences prefs) {
		prefs.put("View.outputDirectory.path", ".");
		prefs.put("PostVis.snapshotDirectory.path", ".");
		prefs.put("recentlyOpenedFiles", "");
	}
}
