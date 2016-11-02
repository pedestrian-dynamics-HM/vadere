package org.vadere.gui.topographycreator.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;

import org.vadere.state.scenario.Topography;
import org.vadere.state.util.StateJsonConverter;

import com.fasterxml.jackson.core.JsonProcessingException;

/**
 * IO-Class to write a Topography to a certain stream.
 *
 */
public class JSONWriter {
	/**
	 * Write the Topography to a certain File.
	 * 
	 * @param topography the topography
	 * @param file the certain file
	 */
	public static void writeTopography(final Topography topography, final File file) {
		try {
			writeTopography(topography, new PrintStream(file));
		} catch (FileNotFoundException | JsonProcessingException e) {
			e.printStackTrace();
		}
	}

	public static void writeTopography(Topography topography, PrintStream stream) throws JsonProcessingException {
		stream.print(StateJsonConverter.serializeTopography(topography));
		stream.flush();
	}
}
