package org.vadere.gui.topographycreator.view;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public abstract class SimpleDocumentListener implements DocumentListener {
	@Override
	public void insertUpdate(DocumentEvent e) {
		handle(e);
	}

	@Override
	public void removeUpdate(DocumentEvent e) {
		handle(e);
	}

	@Override
	public void changedUpdate(DocumentEvent e) {
		handle(e);
	}

	abstract public void handle(DocumentEvent e);
}
