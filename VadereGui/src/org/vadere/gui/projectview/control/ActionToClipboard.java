package org.vadere.gui.projectview.control;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;

import javax.swing.*;

public class ActionToClipboard extends AbstractAction {

	String data;

	public ActionToClipboard(String name, String data){
		super(name);
		this.data = data;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		StringSelection selection = new StringSelection(data);
		Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		clipboard.setContents(selection, selection);
	}
}
