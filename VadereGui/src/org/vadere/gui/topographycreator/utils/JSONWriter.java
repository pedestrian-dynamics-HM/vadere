package org.vadere.gui.topographycreator.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;

import org.vadere.simulator.projects.io.JsonConverter;
import org.vadere.simulator.projects.io.JsonSerializerTopography;
import org.vadere.state.scenario.Topography;
import org.vadere.util.io.IOUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.gson.JsonElement;

/**
 * IO-Class to write a Topography to a certain stream.
 * 
 *
 */
@Deprecated
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
		stream.print(JsonConverter.serializeTopography(topography));
		stream.flush();
	}
}
