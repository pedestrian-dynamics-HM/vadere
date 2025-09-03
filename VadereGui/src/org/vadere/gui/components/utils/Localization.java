package org.vadere.gui.components.utils;

import org.vadere.gui.projectview.view.ProjectView;
import org.vadere.util.config.VadereConfig;
import org.vadere.util.lang.BundleManager;

import javax.swing.*;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Localization {

	private static final String BUNDLE_NAME = "localization";
	private static final Pattern findKeysPattern  = Pattern.compile("\\{\\{(?<key>[^}]+)}}");;
	private static final HashSet<String> visited = new HashSet<>();

	public static String getString(String keyContainingPattern) {
		visited.clear();
		return getStringRecursive(keyContainingPattern, visited);
	}

	/**
	 * Recursively resolves a string value, replacing any placeholder keys
	 * of the form <code>{{key}}</code> with their corresponding resolved values.
	 */
	private static String getStringRecursive(String keyContainingPattern, Set<String> visited) {
		String value = BundleManager.instance().getString(BUNDLE_NAME, keyContainingPattern);
		Matcher findKeyMatcher = findKeysPattern.matcher(value);
		if(!findKeyMatcher.find()) {
			return value;
		}

		if (!visited.add(keyContainingPattern)) {
			throw new IllegalArgumentException("Circular reference detected for key: " + keyContainingPattern);
		}

		StringBuilder stringBuilder = new StringBuilder();
		do {
			String innerKey = findKeyMatcher.group("key").trim();
			String replacement = getStringRecursive(innerKey, visited);
			findKeyMatcher.appendReplacement(stringBuilder, Matcher.quoteReplacement(replacement));
		}while (findKeyMatcher.find());
		findKeyMatcher.appendTail(stringBuilder);

		visited.remove(keyContainingPattern); // allow re-use in other paths
		return stringBuilder.toString();
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
		VadereConfig.getConfig().setProperty("Messages.language", lang.getLanguage());
		BundleManager.instance().setLanguage(lang);
		JOptionPane.showMessageDialog(ProjectView.getMainWindow(), getString("Messages.changeLanguagePopup.text"),
				getString("Messages.changeLanguagePopup.title"), JOptionPane.INFORMATION_MESSAGE);
	}

}
