package org.vadere.simulator.projects.io;

public class TextOutOfNodeException extends Exception {

	public TextOutOfNodeException() {
		super("Text outside of the JSON Node can't be parsed.");
	}

}
