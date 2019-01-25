package org.vadere.gui.components.control.simulation;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.vadere.gui.components.model.DefaultSimulationConfig;
import org.vadere.gui.components.model.SimulationModel;
import org.vadere.gui.components.utils.Messages;
import org.vadere.gui.components.utils.Resources;
import org.vadere.gui.components.view.SimulationRenderer;
import org.vadere.gui.onlinevisualization.view.IRendererChangeListener;
import org.vadere.gui.postvisualization.PostVisualisation;
import org.vadere.gui.postvisualization.utils.TikzGenerator;

import javax.swing.*;

import java.awt.event.ActionEvent;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.prefs.Preferences;

public class ActionGenerateTikz extends AbstractAction implements IRendererChangeListener {
	private static Logger logger = LogManager.getLogger(ActionGenerateTikz.class);
	private static Resources resources = Resources.getInstance("global");
	private final TikzGenerator tikzGenerator;
	private final SimulationModel<? extends DefaultSimulationConfig> model;

	public ActionGenerateTikz(final String name, final Icon icon, final SimulationRenderer renderer,
							  final SimulationModel<? extends DefaultSimulationConfig> model) {
		super(name, icon);
		this.tikzGenerator = new TikzGenerator(renderer, model);
		this.model = model;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		Date todaysDate = new Date();
		SimpleDateFormat formatter = new SimpleDateFormat(resources.getProperty("SettingsDialog.dataFormat"));
		String formattedDate = formatter.format(todaysDate);

		JFileChooser fileChooser = new JFileChooser(Preferences.userNodeForPackage(PostVisualisation.class).get("SettingsDialog.snapshotDirectory.path", "."));
		File outputFile = new File(Messages.getString("FileDialog.filenamePrefix") + formattedDate + ".tex");

		fileChooser.setSelectedFile(outputFile);

		int returnVal = fileChooser.showDialog(null, "Save");

		if (returnVal == JFileChooser.APPROVE_OPTION) {

			outputFile = fileChooser.getSelectedFile().toString().endsWith(".tex") ? fileChooser.getSelectedFile()
					: new File(fileChooser.getSelectedFile().toString() + ".tex");

			tikzGenerator.generateTikz(outputFile);
		}
	}

	@Override
	public void update(SimulationRenderer renderer) {
	}
}
