package org.vadere.gui.topographycreator.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;

import org.vadere.simulator.projects.io.JsonConverter;
import org.vadere.state.scenario.Topography;

import com.fasterxml.jackson.core.JsonProcessingException;

/**
 * Write a topography to a stream.
 *
 */
public class TopographyJsonWriter {

	/** Write the topography to a certain file. */
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
