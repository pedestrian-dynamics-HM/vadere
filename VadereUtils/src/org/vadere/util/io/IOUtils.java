package org.vadere.util.io;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.prefs.BackingStoreException;
import java.util.prefs.InvalidPreferencesFormatException;
import java.util.prefs.Preferences;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * Contains utilities for input and output.
 * 
 */
public class IOUtils {

	public static final String OS_NAME = System.getProperty("os.name").toLowerCase(Locale.US);

	public static final String OS_ARCH = System.getProperty("os.arch").toLowerCase(Locale.US);

	public static final String OS_VERSION = System.getProperty("os.version").toLowerCase(Locale.US);

	public static final String SCENARIO_FILE_EXTENSION = ".scenario";

	public static final String OUTPUT_DIR = "output";

	public static final String SCENARIO_DIR = "scenarios";

	public static final String CORRUPT_DIR = "corrupt";

	public static final String VADERE_PROJECT_FILENAME = "vadere.project";

	public static final String SYSTEM_VARIABLE_PROJECT_PATH = "VADERE_PROJECTS";

	public static final String DATE_FORMAT = "yyyy-MM-dd_HH-mm-ss.S";

	public static final String OUTPUT_FILE_EXTENSION = ".csv";

	public static final String TRAJECTORY_FILE_EXTENSION = ".trajectories";

	public static final String PROJECT_PATH = System.getenv(SYSTEM_VARIABLE_PROJECT_PATH);

	public static Optional<File> getFirstFile(final File directory, final String fileExtension) {
		final File[] files = getFileList(directory, fileExtension);
		if (files.length != 0) {
			return Optional.of(files[0]);
		} else {
			return Optional.empty();
		}
	}

	public static File[] getFilesInScenarioDirectory(Path scenarioDirectory) {
		return IOUtils.getFileList(scenarioDirectory.toFile(), IOUtils.SCENARIO_FILE_EXTENSION);
	}

	public static File[] getFileList(final File directory, final String fileExtension) {
		return directory.listFiles((d, name) -> name.toLowerCase().endsWith(fileExtension));
	}

	public static File[] getScenarioFilesInOutputDirectory(Path outputDir) throws IOException {
		return Files.walk(outputDir)
				.filter(path -> !path.toString().contains("\\corrupt")) // don't look into the corrupt-folder
				.filter(path -> path.getFileName().toString().endsWith(".scenario"))
				.map(path -> new File(path.toString()))
				.toArray(File[]::new);
	}

	/**
	 * Prints a given string to a file with a given name.
	 */
	public static void printDataFile(Path filename, String data)
			throws IOException {

		Path filepath = Paths.get(System.getProperty("user.dir"), filename.toString());
		createDirectoryIfNotExisting(filepath.getParent());

		// create, write and close the file
		BufferedWriter bw = new BufferedWriter(new FileWriter(filepath.toFile()));

		bw.write(data);
		bw.close();
	}

	public static void createDirectoryIfNotExisting(Path path) throws IOException {
		if (!Files.exists(path)) {
			Files.createDirectories(path);
		}
	}

	/**
	 * Computes the path to the users home directory.
	 * http://stackoverflow.com/questions
	 * /3784657/what-is-the-best-way-to-save-user-settings-in-java-application
	 * Works in Windows, iOS and Linux.
	 * 
	 * @return path to the user directory
	 */
	public static String getUserDataDirectory() {
		return System.getProperty("user.home") + File.separator;
	}

	/**
	 * Loads user preferences from the given file.
	 * 
	 * @param filename
	 *        filename of the file storing the preferences
	 * @param cls
	 *        class type for which the preferences should be loaded
	 * @return the preferences object, or null.
	 * @throws InvalidPreferencesFormatException
	 * @throws IOException
	 */
	public static Preferences loadUserPreferences(String filename, Class<?> cls)
			throws IOException, InvalidPreferencesFormatException {
		FileInputStream fis = new FileInputStream(filename);
		Preferences.importPreferences(fis);

		return Preferences.userNodeForPackage(cls);
	}

	/** Saves a given preference to file. */
	public static void saveUserPreferences(String preferencesfilename,
			Preferences prefs) throws IOException, BackingStoreException {
		try (FileOutputStream fos = new FileOutputStream(preferencesfilename)) {
			prefs.exportNode(fos);
		} catch (IOException e) {
			throw e;
		}
	}

	/** Writes a given string to a given file. */
	public static void writeTextFile(String filepath, String text) throws IOException {
		Files.deleteIfExists(Paths.get(filepath));
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(filepath))) {
			writer.write(text);
		}
	}

	/** Reads all text of a given file and store it in a string. */
	public static String readTextFile(Path filePath) throws IOException {
		List<String> lines = Files.readAllLines(filePath, StandardCharsets.UTF_8);
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < lines.size(); i++) {
			sb.append(lines.get(i));
			if (i < lines.size() - 1) {
				sb.append(System.lineSeparator());
			}
		}
		return sb.toString();
	}

	public static String readTextFile(String filePath) throws IOException {
		return readTextFile(Paths.get(filePath));
	}

	/**
	 * Runs file selector with given title using given subdirectory. If the
	 * subdirectory is not an absolute path, it is combined with user.dir.
	 */
	public static String chooseFile(String title, String subdir,
			FileFilter filter) {
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setDialogTitle(title);
		fileChooser.addChoosableFileFilter(filter);
		fileChooser.setFileFilter(filter);
		setFileChooserDirectory(subdir, fileChooser);
		int returnState = fileChooser.showOpenDialog(null);
		return returnValueDependingOnReturnState(fileChooser, returnState);
	}

	private static String returnValueDependingOnReturnState(JFileChooser fileChooser, int returnState) {
		if (returnState == JFileChooser.APPROVE_OPTION) {
			return fileChooser.getSelectedFile().getAbsolutePath();
		} else {
			return null;
		}
	}

	public static String chooseFileSave(String title, String subdir, FileFilter filter) {
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setDialogTitle(title);
		fileChooser.addChoosableFileFilter(filter);
		fileChooser.setFileFilter(filter);
		setFileChooserDirectory(subdir, fileChooser);
		int returnState = fileChooser.showSaveDialog(null);
		return returnValueDependingOnReturnState(fileChooser, returnState);
	}

	public static String chooseFileOrDirSave(String title, String subdir,
			FileFilter filter) {
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setDialogTitle(title);
		fileChooser.addChoosableFileFilter(filter);
		fileChooser.setFileFilter(filter);
		fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
		setFileChooserDirectory(subdir, fileChooser);
		int returnState = fileChooser.showSaveDialog(null);
		return returnValueDependingOnReturnState(fileChooser, returnState);
	}

	private static void setFileChooserDirectory(String subdir, JFileChooser fileChooser) {
		if (subdir.startsWith(File.pathSeparator)) {
			fileChooser.setCurrentDirectory(new File(System
					.getProperty("user.dir") + subdir));
		} else {
			fileChooser.setCurrentDirectory(new File(subdir));
		}
	}

	/**
	 * Runs file selector with given title using given subdirectory. If the
	 * subdirectory is not an absolute path, it is combined with user.dir.
	 * 
	 * @param title
	 *        title of the dialog
	 * @param subdir
	 *        subdirectory of the user.dir
	 * @return the path chosen by the user
	 */
	public static String chooseJSONFileSave(String title, String subdir) {
		String filetype = "json";
		FileFilter filter = new FileNameExtensionFilter("JSON file", filetype);
		return chooseFileSave(title, subdir, filter);
	}

	/** Shows an error box with given message and title. */
	public static void errorBox(String infoMessage, String title) {
		JOptionPane.showMessageDialog(null, infoMessage, "InfoBox: " + title,
				JOptionPane.ERROR_MESSAGE);
	}

	/** Shows an warn box with given message and title. */
	public static void warnBox(String infoMessage, String title) {
		JOptionPane.showMessageDialog(null, infoMessage, "InfoBox: " + title,
				JOptionPane.WARNING_MESSAGE);
	}

	/** Shows an info box with given message and title. */
	public static void infoBox(String infoMessage, String title) {
		JOptionPane.showMessageDialog(null, infoMessage, "InfoBox: " + title,
				JOptionPane.INFORMATION_MESSAGE);
	}

	/**
	 * Opens a yes-no-cancel message box and returns its result (as
	 * JOptionPane.OPTION, i.e. int)
	 */
	public static int chooseYesNoCancel(String infoMessage, String title) {
		return JOptionPane.showConfirmDialog(null, infoMessage, title,
				JOptionPane.YES_NO_CANCEL_OPTION);
	}
}
