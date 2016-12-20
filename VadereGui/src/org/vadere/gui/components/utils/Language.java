package org.vadere.gui.components.utils;

import org.vadere.gui.projectview.VadereApplication;

import java.util.Locale;
import java.util.prefs.Preferences;

public class Language {

	public static final Locale locale = Language.setLanguage();

	/*
	 * The default language of this application is english.
	 */
	static {
		Locale.setDefault(Locale.ENGLISH);
	}

	static Locale setLanguage() {
		String language = Preferences.userNodeForPackage(VadereApplication.class).get("language", null);
		if (language != null) {
			switch (language) {
				case "de":
					return new Locale("de", "DE");
				case "en":
				default:
					return new Locale("en");
			}
		}
		return Locale.getDefault();
	}

	public static boolean languageIsGerman() {
		return locale.getLanguage().equals(Locale.GERMAN.getLanguage());
	}
}
