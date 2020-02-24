package org.vadere.util.config;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.FileBasedConfiguration;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.builder.fluent.PropertiesBuilderParameters;
import org.apache.commons.configuration2.convert.DefaultListDelimiterHandler;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.vadere.util.logging.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * A {@link VadereConfig} reads its options from a text file in "properties" style. I.e., simple
 * key-value pairs where keys can contain dots but no section dividers are allowed (for more details
 * see https://commons.apache.org/proper/commons-configuration/userguide/howto_properties.html#Properties_files).
 * {@link VadereConfig} uses the Apache Commons Configuration library to read such property files.
 *
 * A Vadere config file looks like this:
 *
 * <pre>
 * # PostVis
 * PostVis.SVGWidth=1024
 * PostVis.SVGHeight=768
 * </pre>
 *
 * This config object is used like this:
 *
 * <pre>int svgWidth = VadereConfig.getConfig().getInt("PostVis.SVGWidth");</pre>
 */
public class VadereConfig {

	// Static Variables
	private static final Logger LOGGER = Logger.getLogger(VadereConfig.class);

	// If changing any of the following values, remember to also change it in the CI configuration
	private static final String DEFAULT_HOME_DIR = System.getProperty("user.home");
	private static final String DEFAULT_CONFIG_DIR = ".config";

	// Both variables must not be "final" so that we are able
	// to inject another config file from CLI argument "--config-file myconfig.conf"
	// via static method "setConfigPath()".
	private static String CONFIG_FILENAME = "vadere.conf";
	private static Path CONFIG_PATH = Path.of(DEFAULT_HOME_DIR, DEFAULT_CONFIG_DIR, CONFIG_FILENAME);

	private static VadereConfig SINGLETON_INSTANCE;

	// Variables
	private FileBasedConfiguration vadereConfig;

	// Constructors
	private VadereConfig() {

		createDefaultConfigIfNonExisting();

		LOGGER.info(String.format("Use config file from path %s", CONFIG_PATH));

		// If Vadere was started like "vadere-console.jar --config-file here.txt", search in current working directory.
		String basePath = (CONFIG_PATH.getParent() == null) ? System.getProperty("user.dir") : CONFIG_PATH.getParent().toString();

		PropertiesBuilderParameters propertiesParams = new Parameters()
				.properties()
				.setFileName(CONFIG_FILENAME)
				.setBasePath(basePath)
				.setListDelimiterHandler(new DefaultListDelimiterHandler(',')
				);

		FileBasedConfigurationBuilder<FileBasedConfiguration> builder =
				new FileBasedConfigurationBuilder<FileBasedConfiguration>(PropertiesConfiguration.class)
						.configure(propertiesParams);
		builder.setAutoSave(true);

		try {
			vadereConfig = builder.getConfiguration();
		} catch (ConfigurationException ex) {
			LOGGER.error(String.format("Error while reading config file \"%s\": %s", CONFIG_PATH.toString(), ex.getMessage()));
			LOGGER.info("Create and use default config");
		}

		compareAndChangeDefaultKeysInExistingFile();

/*  // TODO: see also issue #258 -- the problem is that this function sorts the file, but it gets reverted by the (auto-) save functionality of the builder object
        try{
            sortFileLinesAlphabetically(builder.getFileHandler().getFile());
        }catch (IOException ioex){
            LOGGER.error(String.format("Error while reading config file \"%s\": %s", CONFIG_PATH.toString(),
                    ioex.getMessage()));
        }
*/

		try {
			builder.save();
		} catch (ConfigurationException ex) {
			LOGGER.error(String.format("Error while saving config file \"%s\": %s", CONFIG_PATH.toString(),
					ex.getMessage()));
		}

	}


/*  // TODO: see also issue #258 -- the problem is that this function sorts the file, but it gets reverted by the (auto-) save functionality of the builder object
    private void sortFileLinesAlphabetically(File file) throws IOException {
        FileReader fileReader = new FileReader(file);

        BufferedReader bufferedReader = new BufferedReader(fileReader);
        String inputLine;
        List<String> lineList = new ArrayList<>();
        while ((inputLine = bufferedReader.readLine()) != null) {
            if(!inputLine.isBlank()) {
                lineList.add(inputLine);
            }
        }

        fileReader.close();
        bufferedReader.close();

        Collections.sort(lineList);

        FileWriter fileWriter = new FileWriter(file);
        PrintWriter out = new PrintWriter(fileWriter);
        for (String outputLine : lineList) {
            out.println(outputLine);
        }

        out.flush();
        out.close();
        fileWriter.close();
    }*/

	private void createDefaultConfigIfNonExisting() {
		try { // Ensure that config directory exists.
			Files.createDirectories(Path.of(DEFAULT_HOME_DIR, DEFAULT_CONFIG_DIR));
		} catch (IOException ex) {
			LOGGER.error(String.format("Cannot create directory: %s", Path.of(DEFAULT_HOME_DIR, DEFAULT_CONFIG_DIR)));
		}

		if (Files.exists(CONFIG_PATH) == false) {
			Map<String, String> defaultConfig = getDefaultConfig();

			try {
				LOGGER.info(String.format("Writing default config file: %s", CONFIG_PATH));

				Files.write(
						CONFIG_PATH,
						defaultConfig
								.entrySet()
								.stream()
								.map(entry -> entry.getKey() + " = " + entry.getValue())
								.sorted(String::compareTo)
								.collect(Collectors.toList()));
			} catch (IOException e) {
				LOGGER.error(String.format("Error while writing default config file \"%s\": %s", CONFIG_PATH, e.getMessage()));
			}
		}
	}

	private void compareAndChangeDefaultKeysInExistingFile() {
		// Function that removes and adds Key-Value pairs locally when keys are inserted or removed in Vadere.conf.
		// For both actions a info is written on the console.

		// The keys of the default config have to match the keys from the existing file!
		Map<String, String> defaultConfig = getDefaultConfig();

		// First case: if key is missing in existing file then add it but added in to the defaultConfig
		//   -- usually happens when a new configuration key was introduced
		for (String key : defaultConfig.keySet()) {
			if (!vadereConfig.containsKey(key)) {
				String defaultValue = defaultConfig.get(key);

				vadereConfig.addProperty(key, defaultValue);
				LOGGER.info(String.format("Added key \"%s = %s\" to file %s because the configuration key-value pair " +
						"was not present in the file.", key, defaultValue, CONFIG_PATH));
			}
		}

		// Second case: if key was removed from defaultConfig, then it is also removed from the current file
		//   -- usually happens when a configuration key was removed
		for (Iterator<String> iter = vadereConfig.getKeys(); iter.hasNext(); ) {
			String key = iter.next();
			if (!defaultConfig.containsKey(key)) {
				iter.remove();
				LOGGER.info(String.format("Removed key \"%s\" in file %s because there is no corresponding key entry " +
						"in the \"defaultConfig\" in source file VadereConfig.java", key, CONFIG_PATH));
			}
		}

	}

	// Static setters

	/**
	 * With this setter one can inject a different config file instead of using
	 * "~/.config/vadere.conf".
	 *
	 * @param configPath Path to config file.
	 */
	public static void setConfigPath(String configPath) {
		CONFIG_PATH = Path.of(configPath);
		CONFIG_FILENAME = CONFIG_PATH.getFileName().toString();
	}

	// Static getters

	/**
	 * Use Apache Common Configuration API on the returned object to retrieve Vadere's config
	 * options.
	 *
	 * See https://commons.apache.org/proper/commons-configuration/userguide/howto_properties.html#Properties_files
	 *
	 * @return A Configuration object from Apache Common Configuration library.
	 */
	public static Configuration getConfig() {
		if (SINGLETON_INSTANCE == null) {
			SINGLETON_INSTANCE = new VadereConfig();
		}
		return SINGLETON_INSTANCE.vadereConfig;
	}

	// Methods

	private static Map<String, String> getDefaultConfig() {
		//NOTE: Remember to also add the new configuration in existing vadere.conf file.

		final Map<String, String> defaultConfig = new HashMap<>();

		String defaultSearchDirectory = System.getProperty("user.home");

		defaultConfig.put("Density.measurementScale", "10.0");
		defaultConfig.put("Density.measurementRadius", "15");
		defaultConfig.put("Density.standardDeviation", "0.5");
		defaultConfig.put("Gui.showNodes", "false");
		defaultConfig.put("Gui.node.radius", "0.3");
		defaultConfig.put("Gui.dataProcessingViewMode", "gui");
		defaultConfig.put("Gui.toolbar.size", "40");
		defaultConfig.put("Gui.lastSavePoint", defaultSearchDirectory);
		defaultConfig.put("History.lastUsedProject", "");
		defaultConfig.put("History.recentProjects", "");
		defaultConfig.put("Messages.language", Locale.ENGLISH.getLanguage());
		defaultConfig.put("Pedestrian.radius", "0.195");
		defaultConfig.put("PostVis.SVGWidth", "1024");
		defaultConfig.put("PostVis.SVGHeight", "768");
		defaultConfig.put("PostVis.maxNumberOfSaveDirectories", "5");
		defaultConfig.put("PostVis.maxFramePerSecond", "40");
		defaultConfig.put("PostVis.framesPerSecond", "20");
		defaultConfig.put("PostVis.timeResolution", "0.4");
		defaultConfig.put("PostVis.cellWidth", "1.0");
		defaultConfig.put("PostVis.minCellWidth", "0.01");
		defaultConfig.put("PostVis.maxCellWidth", "10.0");
		defaultConfig.put("PostVis.enableJsonInformationPanel", "true");
		defaultConfig.put("ProjectView.icon.height.value", "35");
		defaultConfig.put("ProjectView.icon.width.value", "35");
		defaultConfig.put("ProjectView.cellWidth", "1.0");
		defaultConfig.put("ProjectView.minCellWidth", "0.01");
		defaultConfig.put("ProjectView.maxCellWidth", "10.0");
		defaultConfig.put("ProjectView.defaultDirectory", defaultSearchDirectory);
		defaultConfig.put("ProjectView.defaultDirectoryAttributes", defaultSearchDirectory);
		defaultConfig.put("ProjectView.defaultDirectoryScenarios", defaultSearchDirectory);
		defaultConfig.put("ProjectView.defaultDirectoryOutputProcessors", defaultSearchDirectory);
		defaultConfig.put("Project.simulationResult.show", "true");
		defaultConfig.put("Project.ScenarioChecker.active", "false");
		defaultConfig.put("SettingsDialog.dataFormat", "yyyy_MM_dd_HH_mm_ss");
		defaultConfig.put("SettingsDialog.outputDirectory.path", ".");
		defaultConfig.put("SettingsDialog.snapshotDirectory.path", ".");
		defaultConfig.put("SettingsDialog.showLogo", "false");
		defaultConfig.put("Testing.stepCircleOptimization.compareBruteForceSolution", "false");
		defaultConfig.put("TopographyCreator.dotRadius", "0.5");
		defaultConfig.put("Vadere.cache.useGlobalCacheBaseDir", "false");
		defaultConfig.put("Vadere.cache.globalCacheBaseDir", defaultSearchDirectory + "/.cache/vadere");

		return defaultConfig;
	}
}
