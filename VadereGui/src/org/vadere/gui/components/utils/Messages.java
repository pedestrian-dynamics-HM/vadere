package org.vadere.gui.components.utils;

import com.android.dx.gen.Local;

import javax.swing.*;

import org.vadere.gui.projectview.VadereApplication;
import org.vadere.gui.projectview.view.ProjectView;

import java.beans.Beans;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;

/**
 * Messages class used in localization.
 * 
 * 
 */
public abstract class Messages {

	private static final String BUNDLE_NAME = "messages";
	private static final ResourceBundle RESOURCE_BUNDLE = loadBundle();

	public static String getString(String key) {
		try {
			ResourceBundle bundle = Beans.isDesignTime() ? loadBundle() : RESOURCE_BUNDLE;
			return bundle.getString(key);
		} catch (MissingResourceException e) {
			return "!" + key + "!";
		}
	}

	private static ResourceBundle loadBundle() {
		return ResourceBundle.getBundle(BUNDLE_NAME, Language.locale);
	}

	public static void changeLanguage(Locale lang) {
		Preferences.userNodeForPackage(VadereApplication.class).put("language", lang.getLanguage());
		JOptionPane.showMessageDialog(ProjectView.getMainWindow(), getString("Messages.changeLanguagePopup.text"),
				getString("Messages.changeLanguagePopup.title"), JOptionPane.INFORMATION_MESSAGE);
	}

}
