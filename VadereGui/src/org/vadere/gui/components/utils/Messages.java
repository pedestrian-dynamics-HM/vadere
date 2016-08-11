package org.vadere.gui.components.utils;

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


	public static String getString(String key) {
		try {
			ResourceBundle bundle = Beans.isDesignTime() ? loadBundle() : RESOURCE_BUNDLE;
			return bundle.getString(key);
		} catch (MissingResourceException e) {
			return "!" + key + "!";
		}
	}

	private static final String BUNDLE_NAME = "messages";
	public static final Locale locale = setLanguage();
	private static final ResourceBundle RESOURCE_BUNDLE = loadBundle();

	private static ResourceBundle loadBundle() {
		return ResourceBundle.getBundle(BUNDLE_NAME, locale);
	}

	private static Locale setLanguage() {
		String language = Preferences.userNodeForPackage(VadereApplication.class).get("language", null);
		if (language != null) {
			switch (language) {
				case "de":
					return Locale.GERMAN;
				case "en":
				default:
					return Locale.ENGLISH;
			}
		}
		return Locale.getDefault();
	}

	public static void changeLanguage(Locale lang) {
		Preferences.userNodeForPackage(VadereApplication.class).put("language", lang.getLanguage());
		JOptionPane.showMessageDialog(ProjectView.getMainWindow(), getString("Messages.changeLanguagePopup.text"),
				getString("Messages.changeLanguagePopup.title"), JOptionPane.INFORMATION_MESSAGE);
	}

	public static boolean languageIsGerman() {
		return locale.getLanguage().equals(Locale.GERMAN.getLanguage());
	}

}
