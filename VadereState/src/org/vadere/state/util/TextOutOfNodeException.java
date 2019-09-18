package org.vadere.state.util;

import java.io.IOException;

public class TextOutOfNodeException extends IOException {

	// is an exception to used this case: stackoverflow.com/a/26026359
	public TextOutOfNodeException() {
		super("Text outside of the JSON Node can't be parsed.");
	}

}
