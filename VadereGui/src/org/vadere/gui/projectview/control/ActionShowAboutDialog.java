package org.vadere.gui.projectview.control;

import javax.swing.*;

import org.vadere.gui.components.utils.Messages;
import org.vadere.gui.components.utils.Resources;
import org.vadere.simulator.projects.io.HashGenerator;

import java.awt.event.ActionEvent;
import java.text.MessageFormat;

public class ActionShowAboutDialog extends AbstractAction {

	private static final long serialVersionUID = 1L;
	private static Resources resources = Resources.getInstance("global");

	public ActionShowAboutDialog(final String name) {
		super(name);
	}

	@Override
	public void actionPerformed(final ActionEvent event) {
		String text = "";
		text += "<html>";
		text += "<font size =\"3\"><em>" + MessageFormat.format(Messages.getString("ProjectView.version"), HashGenerator.releaseNumber()) + "</em></font><br>";
		text += "<br>";
		text += "<font size =\"3\">www.vadere.org</font><br>";
		text += "<font size =\"3\">" + MessageFormat.format(Messages.getString("ProjectView.license.text"), "GNU Lesser General Public License (<em>LGPL</em>).") + "</font>";
		text += "</html>";

		JOptionPane.showMessageDialog(null,
				text, Messages.getString("ProjectView.aboutDialog.label"),
				JOptionPane.INFORMATION_MESSAGE, new ImageIcon(resources.getImage("vadere_small.png")));
	}
}
