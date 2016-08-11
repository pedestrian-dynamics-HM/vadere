package org.vadere.gui.projectview.view;

import javax.swing.*;
import java.awt.*;

/**
 * A panel with a progress bar and a text.
 * 
 * 
 */
public class ProgressPanel extends JPanel {

	private static final long serialVersionUID = 786950536706090248L;
	private JLabel lblProgressText;
	private JProgressBar progressBar;

	/**
	 * Create the panel.
	 */
	public ProgressPanel() {
		setLayout(new GridLayout(0, 3, 0, 0));

		lblProgressText = new JLabel("progress text");
		add(lblProgressText);

		progressBar = new JProgressBar();
		add(progressBar);
		progressBar.setValue(50);

	}

	/**
	 * Set text and progress bar value
	 * 
	 * @param text
	 * @param value
	 */
	public void setData(String text, int value) {
		this.progressBar.setValue(value);
		lblProgressText.setText(text);
	}
}
