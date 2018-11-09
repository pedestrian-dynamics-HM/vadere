package org.vadere.util.lang;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.ResourceBundle;

/**
 *  Based StackOverflow post https://stackoverflow.com/a/4325119
 */
public class ModuleResourceBundle extends ResourceBundle {


	protected static final Control CONTROL = new MultiResourceBundleControl();
	private Properties properties;
	private HashSet<String>	usedProperties;


	public ModuleResourceBundle(String baseName, Locale locale){
		setParent(ResourceBundle.getBundle(baseName, locale, CONTROL));
	}

	protected ModuleResourceBundle(Properties properties, HashSet<String> usedProperties){
		this.properties = properties;
		this.usedProperties = usedProperties;
	}

	@Override
	protected Object handleGetObject(@NotNull String key) {
		Object value = properties != null ? properties.get(key) : null;
		if (value == null){
			value = parent != null ? parent.getObject(key) : null;
			if (value == null){
				return "!" + key + "!";
			}
		}
		return value;
	}

	@NotNull
	@Override
	@SuppressWarnings("unchecked")
	public Enumeration<String> getKeys() {
		return properties != null ? (Enumeration<String>) properties.propertyNames() : parent.getKeys();
	}

	protected static class MultiResourceBundleControl extends Control {


		private static final String DEFAULT_LANG_TAG = "en";

		@Override
		public List<Locale> getCandidateLocales(String baseName, Locale locale) {
			return super.getCandidateLocales(baseName, locale);
		}

		@Override
		public Locale getFallbackLocale(String baseName, Locale locale) {
			return super.getFallbackLocale(baseName, locale);
		}

		@Override
		public ResourceBundle newBundle(String baseName, Locale locale, String format,
										ClassLoader loader, boolean reload)
				throws IOException {
			if (locale.getLanguage().isEmpty())
				return null;

			Properties properties = load(baseName, locale, loader);
			String include = properties.getProperty("include");
			HashSet<String> usedProperties = new HashSet<>();
			if (include != null){
				for (String includeBasename : include.split("\\s*,\\s*")){
					if (usedProperties.add(includeBasename)){
						properties.putAll(load(includeBasename, locale, loader));
					}
				}
			}
			return new ModuleResourceBundle(properties, usedProperties);
		}

		private Properties load(String baseName, Locale locale, ClassLoader loader) throws IOException {
			Properties properties = new Properties();
			String lang_tag = locale.getLanguage().endsWith(DEFAULT_LANG_TAG) ? "" : "_" + locale.getLanguage() + "_" + locale.getCountry();
			InputStream stream = loader.getResourceAsStream(baseName + lang_tag +".properties");
			properties.load(stream);
			return properties;
		}

	}
}
