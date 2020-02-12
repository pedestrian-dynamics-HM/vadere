package org.vadere.manager.client;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class ConsoleReader implements Runnable {

	private HashMap<String, ConsoleCommand> cmdMap;
	private HashMap<String, String> helpMap;
	private BufferedReader reader;
	private Scanner scanner;
	private boolean running;

	public ConsoleReader() {
		cmdMap = new HashMap<>();
		helpMap = new HashMap<>();
		reader = new BufferedReader(new InputStreamReader(System.in));
		running = true;

		addCommand("help", "Print this Help", this::cmd_help);
	}

	public static void main(String[] args) {
		ConsoleReader r = new ConsoleReader();
		r.addCommand("do-shit", "guess..",
				args1 -> System.out.println("do-shit with " + Arrays.toString(args1)));

		Thread thread = new Thread(r);

		System.out.println("start..");

		thread.start();

		try {
			thread.join();
			System.out.println("joined");
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private void cmd_help(String[] args) {
		if (!args[0].equals("help"))
			System.out.println("Unknown command: " + args[0]);

		System.out.println("Help: ");
		int cmdLen = helpMap.entrySet().stream().map(o -> o.getKey().length()).max(Integer::compareTo).orElse(20);
		helpMap.entrySet()
				.stream()
				.sorted(Comparator.comparing(Map.Entry::getKey))
				.forEach(e -> {
					System.out.printf("%-" + cmdLen + "s     %s\n", e.getKey(), e.getValue());
				});
		System.out.println();
	}

	private void executeCmd(String cmdStr) {
		if (cmdStr.equals(""))
			return;
		String[] cmdArgs = cmdStr.split(" ");
		ConsoleCommand cmd = cmdMap.getOrDefault(cmdArgs[0], this::cmd_help);
		try {
			cmd.execute(cmdArgs);
		} catch (EOFException eof) {
			System.out.println("Server closed connection");
			stop();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void addCommand(String cmdStr, String cmdHelp, ConsoleCommand cmd) {
		cmdMap.put(cmdStr, cmd);
		helpMap.put(cmdStr, cmdHelp);
	}

	private void commandLoop() {
		while (running) {

			try {
				System.out.print("> ");
				String cmd = reader.readLine();
				executeCmd(cmd.trim());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		System.out.println("ending....");
	}

	synchronized public void stop() {
		running = false;
	}

	@Override
	public void run() {
		commandLoop();
	}
}
