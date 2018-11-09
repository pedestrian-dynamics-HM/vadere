package org.vadere.simulator.utils;

import org.vadere.util.lang.BundleManager;

import java.util.Locale;
import java.util.prefs.Preferences;

import javax.swing.*;

public class Messages {

	private static final String BUNDLE_NAME = "i18n_simulation";

	public static String getString(String key) {
		return BundleManager.instance().getString(BUNDLE_NAME, key);
	}

	public static boolean languageIsGerman(){
		return BundleManager.instance().languageIsGerman();
	}

	public static Locale getCurrentLocale(){
		return BundleManager.instance().getCurrentLocale();
	}


}
