package org.vadere.util.lang;

import org.jetbrains.annotations.NotNull;
import org.vadere.util.config.VadereConfig;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.swing.*;

/**
 * Manage all {@link java.util.ResourceBundle}s present in the application.
 * Each module must use this Manager to allow easy inheritance of language keys.
 * If the application language is changed this manager will propagate the new Locale
 * to each Bundel.
 */
public class BundleManager {
	private static final Locale defaultLocale = new Locale("en");
	//good performance threadsafe Singletone. Sync block will only be used once
	private static BundleManager instance;
	public static BundleManager instance(){
		if(instance ==  null){
			synchronized (BundleManager.class){
				if(instance == null){
					instance = new BundleManager();
				}
			}
		}
		return instance;
	}


	private HashMap<String, ModuleResourceBundle> bundleMap;
	private Locale currentLocale;


	public Locale getCurrentLocale(){
		return (Locale)currentLocale.clone();
	}

	public boolean languageIsGerman(){
		return currentLocale.getLanguage().equals(Locale.GERMAN.getLanguage());
	}

	public String getString(String baseName, String key){
		ModuleResourceBundle b = bundleMap.getOrDefault(baseName, loadBundle(baseName));
		return b != null ? b.getString(key) : "!" +baseName + " Bundle Not Found!";
	}

	public void setLanguage(Locale locale){
		currentLocale = locale;
		Locale.setDefault(locale);
		reloadBundles();
	}

	public void setLanguage(Class<?> clazz){
		String language = VadereConfig.getConfig().getString("Messages.language", null);
		Locale locale = defaultLocale;
		if (language != null) {
			switch (language) {
				case "de":
					locale = new Locale("de", "DE");
					break;
				case "en":
				default:
					locale = defaultLocale;
			}
		}
		setLanguage(locale);
	}

	private BundleManager(){
		bundleMap = new HashMap<>();
		currentLocale = defaultLocale;
	}

	private void reloadBundles(){
		for (Object o : bundleMap.entrySet()) {
			Map.Entry pair = (Map.Entry) o;
			String key = (String) pair.getKey();
			bundleMap.put(key, new ModuleResourceBundle(key, currentLocale));
		}
	}

	private ModuleResourceBundle loadBundle(@NotNull String baseName){
		if (bundleMap.containsKey(baseName)){
			return bundleMap.get(baseName);
		} else {
			ModuleResourceBundle bundle = new ModuleResourceBundle(baseName, currentLocale);
			bundleMap.put(baseName, bundle);
			return bundle;
		}
	}
}
