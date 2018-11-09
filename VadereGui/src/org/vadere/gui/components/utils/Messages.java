package org.vadere.gui.components.utils;

import org.vadere.gui.projectview.VadereApplication;
import org.vadere.gui.projectview.view.ProjectView;
import org.vadere.util.lang.BundleManager;

import java.util.Locale;
import java.util.prefs.Preferences;

import javax.swing.*;

/**
 * Messages class used in localization.
 * 
 */
public class Messages {

	private static final String BUNDLE_NAME = "messages";

	public static String getString(String key) {
		return BundleManager.instance().getString(BUNDLE_NAME, key);
	}

	public static boolean languageIsGerman(){
		return BundleManager.instance().languageIsGerman();
	}

	public static Locale getCurrentLocale(){
		return BundleManager.instance().getCurrentLocale();
	}

	public static void loadLanguageFromPreferences(Class<?> clazz){
		BundleManager.instance().setLanguage(clazz);
	}

	public static void changeLanguage(Locale lang) {
		Preferences.userNodeForPackage(VadereApplication.class).put("language", lang.getLanguage());
		BundleManager.instance().setLanguage(lang);
		JOptionPane.showMessageDialog(ProjectView.getMainWindow(), getString("Messages.changeLanguagePopup.text"),
				getString("Messages.changeLanguagePopup.title"), JOptionPane.INFORMATION_MESSAGE);
	}

}
