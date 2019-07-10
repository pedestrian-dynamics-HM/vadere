package org.vadere.util.config;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.FileBasedConfiguration;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.builder.fluent.PropertiesBuilderParameters;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.vadere.util.logging.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A {@link VadereConfig} reads its options from a text file in "properties" style. I.e., simple key-value pairs
 * where keys can contain dots but no section dividers are allowed (for more details see
 * https://commons.apache.org/proper/commons-configuration/userguide/howto_properties.html#Properties_files).
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
    private static final String CONFIG_FILENAME = "vadere.conf";
    private static final String HOME_DIR = System.getProperty("user.home");
    private static final String CONFIG_DIR = ".config";

    private static final Path PATH_TO_CONFIG_DIR = Path.of(HOME_DIR, CONFIG_DIR);
    private static final Path PATH_TO_CONFIG = Path.of(HOME_DIR, CONFIG_DIR, CONFIG_FILENAME);

    private static VadereConfig SINGLETON_INSTANCE = new VadereConfig();

    // Variables
    private FileBasedConfiguration vadereConfig;

    // Constructors
    private VadereConfig() {
        createDefaultConfigIfNonExisting();

        PropertiesBuilderParameters propertiesParams = new Parameters()
                .properties()
                .setFileName(CONFIG_FILENAME)
                .setBasePath(PATH_TO_CONFIG.getParent().toString());

        FileBasedConfigurationBuilder<FileBasedConfiguration> builder =
                new FileBasedConfigurationBuilder<FileBasedConfiguration>(PropertiesConfiguration.class)
                .configure(propertiesParams);
        builder.setAutoSave(true);

        try {
            vadereConfig = builder.getConfiguration();
        } catch (ConfigurationException ex) {
            LOGGER.error(String.format("Error while reading config file \"%s\": %s", PATH_TO_CONFIG.toString(), ex.getMessage()));
            LOGGER.info("Create and use default config");
        }
    }

    private void createDefaultConfigIfNonExisting() {
        try { // Ensure that config directory exists.
            Files.createDirectories(PATH_TO_CONFIG_DIR);
        } catch (IOException ex) {
            LOGGER.error(String.format("Cannot create directory: %s", PATH_TO_CONFIG));
        }

        if (Files.exists(PATH_TO_CONFIG) == false) {
            Map<String, String> defaultConfig = getDefaultConfig();

            try {
                LOGGER.info(String.format("Writing default config file: %s", PATH_TO_CONFIG));

                Files.write(
                        PATH_TO_CONFIG,
                        defaultConfig
                                .entrySet()
                                .stream()
                                .map(entry -> entry.getKey() + " = " + entry.getValue())
                                .sorted(String::compareTo)
                                .collect(Collectors.toList()));
            } catch (IOException e) {
                LOGGER.error(String.format("Error while writing default config file \"%s\": %s", PATH_TO_CONFIG.toString(), e.getMessage()));
            }
        }
    }

    // Static getters
    /**
     * Use Apache Common Configuration API on the returned object to retrieve Vadere's config options.
     *
     * See https://commons.apache.org/proper/commons-configuration/userguide/howto_properties.html#Properties_files
     *
     * @return A Configuration object from Apache Common Configuration library.
     */
    public static Configuration getConfig() {
        return SINGLETON_INSTANCE.vadereConfig;
    }

    // Methods

    private static Map<String, String> getDefaultConfig(){
        //NOTE: Remember to also add the new configuration in existing vadere.conf file.

        final Map<String, String> defaultConfig = new HashMap<>();

        String defaultSearchDirectory = System.getProperty("user.home");

        defaultConfig.put("Gui.dataProcessingViewMode", "gui");
        defaultConfig.put("Gui.toolbar.size", "40");
        defaultConfig.put("Gui.lastSavePoint", defaultSearchDirectory);
        defaultConfig.put("Density.measurementScale", "10.0");
        defaultConfig.put("Density.measurementRadius", "15");
        defaultConfig.put("Density.standardDeviation", "0.5");
        defaultConfig.put("Messages.language", Locale.ENGLISH.getLanguage());
        defaultConfig.put("Pedestrian.radius", "0.195");
        defaultConfig.put("PostVis.SVGWidth", "1024");
        defaultConfig.put("PostVis.SVGHeight", "768");
        defaultConfig.put("PostVis.maxNumberOfSaveDirectories", "5");
        defaultConfig.put("PostVis.maxFramePerSecond", "30");
        defaultConfig.put("PostVis.framesPerSecond", "5");
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
        defaultConfig.put("SettingsDialog.maxNumberOfTargets", "10");
        defaultConfig.put("SettingsDialog.dataFormat", "yyyy_MM_dd_HH_mm_ss");
        defaultConfig.put("SettingsDialog.outputDirectory.path", ".");
        defaultConfig.put("SettingsDialog.snapshotDirectory.path", ".");
        defaultConfig.put("SettingsDialog.showLogo", "false");
        defaultConfig.put("Testing.stepCircleOptimization.compareBruteForceSolution", "false");
        defaultConfig.put("TopographyCreator.dotRadius", "0.5");

        return defaultConfig;
    }
}
