package org.vadere.util.io.filewatcher;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class TimeStepFileHandler {

	private File timeStepFile = null;

	private static String ADirectiory = "Time Step file path is a directory";

	public TimeStepFileHandler(String timeStepFilePath) throws Exception {

		timeStepFile = new File(timeStepFilePath);

		if (timeStepFile.isDirectory()) {

			throw new Exception(ADirectiory);
		}
	}

	public void writeTimeStepFile(ArrayList<String> states) throws IOException {

		timeStepFile.delete();
		timeStepFile.createNewFile();

		FileWriter writer = new FileWriter(timeStepFile, false);
		BufferedWriter bufWriter = new BufferedWriter(writer);

		for (int iter = 0; iter < states.size(); iter++) {

			if (iter == states.size() - 1) {
				bufWriter.write(states.get(iter));
			} else {
				bufWriter.write(states.get(iter) + "\n");
			}

		}

		bufWriter.close();
		writer.close();
	}

	public void deleteTimeStepFile() {

		timeStepFile.delete();
	}

	public ArrayList<String> readTimeStepFile() throws IOException {

		ArrayList<String> states = new ArrayList<String>();

		try {

			FileReader reader = new FileReader(timeStepFile);
			BufferedReader bufferReader = new BufferedReader(reader);

			String state = null;

			while ((state = bufferReader.readLine()) != null) {

				states.add(state);
			}

			bufferReader.close();
			reader.close();
		} catch (IOException ex) {

		}

		return states;
	}
}
