package org.vadere.gui.postvisualization.control;

import java.awt.event.ActionEvent;
import java.io.File;
import java.util.prefs.Preferences;

import javax.swing.JFileChooser;
import javax.swing.JTextField;

import org.vadere.gui.components.utils.Resources;
import org.vadere.gui.postvisualization.PostVisualisation;
import org.vadere.gui.postvisualization.model.PostvisualizationModel;

public class ActionSetSnapshotDirectory extends ActionVisualization {
	private static Resources resources = Resources.getInstance("postvisualization");

	private final JTextField textField;

	public ActionSetSnapshotDirectory(final String name, final PostvisualizationModel model,
			final JTextField textField) {
		super(name, model);
		this.textField = textField;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		final JFileChooser fc = new JFileChooser(resources.getProperty("PostVis.snapshotDirectory.path"));
		fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		int returnVal = fc.showOpenDialog(null);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			File file = fc.getSelectedFile();
			resources.setProperty("PostVis.snapshotDirectory.path", file.getAbsolutePath());
			Preferences.userNodeForPackage(PostVisualisation.class).put("PostVis.snapshotDirectory.path",
					file.getAbsolutePath());
			textField.setText(file.getAbsolutePath());
		}
	}

}
