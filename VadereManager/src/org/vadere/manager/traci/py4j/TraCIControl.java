package org.vadere.manager.traci.py4j;

import org.vadere.manager.TraCISocket;
import org.vadere.manager.traci.commands.control.TraCICloseCommand;
import org.vadere.manager.traci.commands.control.TraCIGetVersionCommand;
import org.vadere.manager.traci.commands.control.TraCISendFileCommand;
import org.vadere.manager.traci.commands.control.TraCISimStepCommand;
import org.vadere.manager.traci.reader.TraCIPacketBuffer;
import org.vadere.manager.traci.response.TraCIResponse;
import org.vadere.manager.traci.response.TraCISimTimeResponse;
import org.vadere.manager.traci.writer.TraCIPacket;
import org.vadere.util.io.IOUtils;

import java.io.IOException;
import java.nio.file.Paths;

/*
 * Author: Philipp Schuegraf
 *
 * This serves as an API for the TraCIEntryPoint, which makes it possible to call the API from python with the py4j package.
 */
class TraCIControl {

	private TraCISocket socket;
	private String basePath = "";
	private String defaultScenario = "";

	TraCIControl(TraCISocket socket, String basePath, String defaultScenario) {
		this.socket = socket;
		this.basePath = basePath;
		this.defaultScenario = defaultScenario;
	}

	public String getVersion() throws IOException {
		TraCIPacket p = TraCIGetVersionCommand.build();
		socket.sendExact(p);

		TraCIPacketBuffer buf = socket.receiveExact();
		TraCIResponse cmd = buf.nextResponse();

		return cmd.toString();
	}

	public String close() throws IOException {

		socket.sendExact(TraCICloseCommand.build());

		TraCIResponse cmd = socket.receiveResponse();

		return cmd.toString() + "\nBye";
	}

	private String nextSimTimeStepFromSimTime(double nextSimTime) throws IOException {
		TraCIPacket packet = TraCISimStepCommand.build(nextSimTime);
		socket.sendExact(packet);

		TraCISimTimeResponse cmd = (TraCISimTimeResponse) socket.receiveResponse();
		return cmd.toString();
	}

	public String nextSimTimeStep(String simTimeStep) throws IOException {
		double nextSimTime;
		if (simTimeStep.equals(""))
			nextSimTime = -1.;
		else
			nextSimTime = Double.parseDouble(simTimeStep);
		return nextSimTimeStepFromSimTime(nextSimTime);
	}

	private String sendFileFromPath(String filePath) throws IOException {
		String data;
		try {
			data = IOUtils.readTextFile(filePath);
		} catch (IOException e) {
			return "File not found: " + filePath;
		}

		TraCIPacket packet = TraCISendFileCommand.TraCISendFileCommand("Test", data);

		socket.sendExact(packet);

		TraCIPacketBuffer buf = socket.receiveExact();
		TraCIResponse cmd = buf.nextResponse();

		return cmd.toString();
	}

	public String sendFile(String scenarioPath) throws IOException {

		String filePath;

		if (!scenarioPath.isEmpty()) {
			if (!basePath.isEmpty()) {
				filePath = Paths.get(basePath, scenarioPath + ".scenario").toString();
			} else {
				filePath = scenarioPath;
			}
		} else {
			if (!basePath.isEmpty() && !defaultScenario.isEmpty()) {
				filePath = Paths.get(basePath, defaultScenario).toString();
				System.out.println("use default " + defaultScenario);
			} else {
				System.out.println("no default scenario set");
				return "";
			}
		}
		return sendFileFromPath(filePath);
	}
}
