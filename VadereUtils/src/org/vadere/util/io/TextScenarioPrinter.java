package org.vadere.util.io;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Prints a scenario file with obstacles, sources and targets forming a given
 * text.
 * 
 */
public class TextScenarioPrinter {
	private static final int widthPerCharacter = 3;
	private static final int heightPerCharacter = 3;
	private static final int boundaryWidth = 1;

	/**
	 * the number of the target ids in the given text. Used to change the
	 * coordinates appropriately.
	 */
	private static int targetCounter;
	public static String pathToLetters = "data/TextScenarioPrinter";
	private static int targetCounterAdd;
	private static double yAdd;
	private static double xAdd;

	/**
	 * Entry point, use arg0 to set the text, arg1 to set the output file name.
	 * 
	 * @param args
	 *        string array. args[0] must be the text, args[1] the output
	 *        file name.
	 */
	public static void main(String... args) {
		if (args.length < 2) {
			System.out
					.println("Please specify text (arg0) and filepath (arg1) for the text scenario.");
			return;
		}

		// store the text in uppercase
		String text = args[0].replace("\\n", "\n").toUpperCase();

		// store the file path for the new scenario file containing the
		// obstacles etc.
		String textScenarioFilePath = args[1];

		// create the text scenario
		createTextScenario(text, textScenarioFilePath);
	}

	/**
	 * Creates a scenario file from a given text.
	 * 
	 * @param text
	 *        the text to convert to a scenario
	 * @param textScenarioFilePath
	 *        the file path to the new scenario file
	 */
	private static void createTextScenario(String text,
			String textScenarioFilePath) {
		String[] lines = text.split("\n");
		int maxLenPerLine = 0;
		for (String line : lines) {
			if (line.length() > maxLenPerLine) {
				maxLenPerLine = line.length();
			}
		}
		int xSize = maxLenPerLine * widthPerCharacter + boundaryWidth * 2;
		int ySize = lines.length * heightPerCharacter + boundaryWidth * 2;

		String scenarioLines =
				"<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?><scenario><topography boundaryWidth=\"1.00\" xSize=\""
						+ xSize + "\" ySize=\"" + ySize + "\">";
		String topographyLines = "";
		String objectsLines = "";
		targetCounter = 1;
		yAdd = 0; // this value is added to every y value of a letters scenario
		// definition file. used to introduce line breaks.
		xAdd = 0;

		// loop through letters, build topology and objects
		for (char letter : text.toCharArray()) {
			String[] definition = convertLetterToscenario(letter);
			topographyLines += System.lineSeparator() + definition[0];
			objectsLines += System.lineSeparator() + definition[1];
		}

		// combine scenario file
		scenarioLines = String.format("%s%n%s</topography>%n%s%n</scenario>",
				scenarioLines, topographyLines, objectsLines);

		// write the scenario file
		Charset charset = StandardCharsets.UTF_8;
		Path filepath;
		try {
			filepath = Paths.get(textScenarioFilePath);
			Files.deleteIfExists(filepath);
			filepath = Files.createFile(filepath);
			BufferedWriter writer = Files.newBufferedWriter(filepath, charset);
			writer.write(scenarioLines, 0, scenarioLines.length());
			writer.flush();
		} catch (IOException x) {
			System.err.format("IOException: %s%n", x);
		}
	}

	/**
	 * Converts a given char to a sequence of scenario object definitions stored
	 * in a string.
	 * 
	 * @param letter
	 * @return a string array containing the topography [0] and object
	 *         definitions [1] (obstacles, sources, targets).
	 */
	private static String[] convertLetterToscenario(char letter) {
		// a line break just means adding a character width to y
		if (letter == '\n') {
			yAdd += widthPerCharacter;
			xAdd = 0;
			return new String[] {"", ""};
		}

		Path pathToLetterDefinition = Paths.get(pathToLetters,
				String.format("%c", letter) + ".xml");
		String topographyOutput = "";
		String objectsOutput = "";
		boolean isTopology = true;
		targetCounterAdd = 0;
		try {
			List<String> lines = Files.readAllLines(pathToLetterDefinition,
					StandardCharsets.UTF_8);

			for (String line : lines) {
				if (line.startsWith("<?xml") || line.startsWith("<scenario>")
						|| line.startsWith("<topography")
						|| line.startsWith("</scenario>")) {
					continue;
				}
				if (line.contains("</topography>")) {
					isTopology = false;
				} else {
					if (isTopology) {
						topographyOutput += correctCoordinates(line);
					} else {
						objectsOutput += correctCoordinates(line);
					}
				}
			}
		} catch (IOException e) {
			// file not found, can't create object definitions.
			// don't write this letter, but increase letter counter, thus
			// leaving a hole in the scenario text.
			topographyOutput = "";
			objectsOutput = "";
		}

		xAdd += widthPerCharacter;
		targetCounter += targetCounterAdd;

		return new String[] {topographyOutput, objectsOutput};
	}

	/**
	 * Corrects the coordinates in the given line of a scenario definition file
	 * according to current letterCounter, sourceCounter and targetCounter
	 * values.
	 * 
	 * @param line
	 *        line of a scenario definition file
	 * @return the same line, only that all contained coordinates are adjusted.
	 */
	private static String correctCoordinates(String line) {
		String[] coordinateTypes = new String[] {"x", "y", "xMin", "xMax",
				"yMin", "yMax", "targetId", "id"};

		for (String type : coordinateTypes) {
			String modifiedType = " " + type + "=";
			// if the line triangleContains one of the given coordinateTypes, replace
			// the number by an appropriate new number.
			// this depends on whether its an x or y value type.
			if (line.contains(modifiedType)) {
				String originalText = line;
				StringBuffer newText = new StringBuffer();
				StringBuffer tempBuff = null;
				// match floating point values
				Pattern myPattern = Pattern.compile(type
						+ "=\"[-+]?([0-9]*\\.[0-9]+|[0-9]+)\"");
				Matcher myMatcher = myPattern.matcher(originalText);
				// run through all number matches
				while (myMatcher.find()) {
					tempBuff = new StringBuffer();
					String toReplace = myMatcher.group();
					double number = Double.parseDouble(toReplace.substring(
							type.length() + "=\"".length(), toReplace.length()
									- "\"".length()));

					// if we found a source or target id, change to the current source/target id counter.
					if (type.contains("id")) {
						number -= 1; // remove the initial number, as ids in each letter definition file start with one, not zero.
						number += targetCounter;
						targetCounterAdd++;

						tempBuff.append(String.format(type + "=\"%.0f\"",
								number));
					} else if (type.contains("targetId")) {
						number -= 1; // remove the initial number, as ids in each letter definition file start with one, not zero.
						number += targetCounter;
						tempBuff.append(String.format(type + "=\"%.0f\"",
								number));
					} else
					// a real coordinate, change according to letterCounter
					{
						// only change x values
						if (!type.contains("y")) {
							number += xAdd + 1;
						} else {
							number += 1;
							number += yAdd;
						}

						tempBuff.append(String.format(type + "=\"%.2f\"",
								number));
					}

					myMatcher.appendReplacement(newText, tempBuff.toString());
				}

				myMatcher.appendTail(newText);
				line = newText.toString();
			}
		}

		return line;
	}
}
