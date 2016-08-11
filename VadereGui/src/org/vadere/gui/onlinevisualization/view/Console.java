package org.vadere.gui.onlinevisualization.view;

import java.awt.Font;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

/**
 * Quick implementation of a console. This is necessary as there may be no
 * console when application won't be started within Eclipse for example. Calls
 * to System.out or System.err are redirected to the consoles internal text
 * buffer.
 */
public class Console {

	private JTextArea textArea;
	private ByteArrayOutputStream text;
	private PrintStream originalStreamOut;
	private int charCopiedToOriginalStream = 0;
	private JFrame mainWindow = null;

	/**
	 * Creates a new console window. System.out and System.err are redirected to
	 * the internal text buffer.
	 */
	public Console() {
		text = new ByteArrayOutputStream();
		PrintStream printStream = new PrintStream(text);
		originalStreamOut = System.out;
		System.setOut(printStream);
		System.setErr(printStream);
	}

	/**
	 * Should be called at end of simulation only, as displaying the window
	 * slows down simulation.
	 */
	public void showOutput() {
		mainWindow = new JFrame("Vadere Output");
		JScrollPane scrollPane;

		textArea = new JTextArea();
		textArea.setFont(new Font("Courier New", Font.PLAIN, 12));
		textArea.setText(text.toString());

		scrollPane = new JScrollPane(textArea);

		mainWindow.add(scrollPane);

		mainWindow.setSize(550, 250);
		mainWindow.setVisible(true);
		mainWindow.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}

	public void toFront() {
		mainWindow.setAlwaysOnTop(true);
		mainWindow.setAlwaysOnTop(false);
	}

	public void flush() {
		String s = text.toString();
		originalStreamOut.print(s.substring(charCopiedToOriginalStream));
		charCopiedToOriginalStream += s.length() - charCopiedToOriginalStream;
	}
}
