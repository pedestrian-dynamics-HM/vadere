package org.vadere.gui.projectview.control;

import org.vadere.gui.components.utils.Messages;
import org.vadere.gui.components.utils.Resources;
import org.vadere.util.version.Version;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class ActionShowAboutDialog extends AbstractAction {

	private static final long serialVersionUID = 1L;
	private static final Resources resources = Resources.getInstance("global");

	public ActionShowAboutDialog(final String name) {
		super(name);
	}

	@Override
	public void actionPerformed(final ActionEvent event) {
		String releaseVersion = String.format("%s: %s", Messages.getString("ProjectView.version.release"), Version.releaseNumber());
		String versionControlInfo = String.format("%s: %s", Messages.getString("ProjectView.version.commit"), Version.getVersionControlCommitHash());
		String license = String.format("%s: %s", Messages.getString("ProjectView.license.text"), "GNU Lesser General Public License (LGPL)");

		String text = "";
		text += "<html>";
		text += "<font size =\"3\"><em>" + releaseVersion + "</em></font><br>";
		text += "<font size =\"3\"><em>" + versionControlInfo + "</em></font><br>";
		text += "<font size =\"3\"><em>" + license + "</em></font><br>";
		text += "<br>";
		text += "<font size =\"3\">www.vadere.org</font>";
		text += "</html>";

		JOptionPane.showMessageDialog(null,
				text, Messages.getString("ProjectView.aboutDialog.label"),
				JOptionPane.INFORMATION_MESSAGE, new ImageIcon(resources.getImage("vadere_small.png")));
	}
}
