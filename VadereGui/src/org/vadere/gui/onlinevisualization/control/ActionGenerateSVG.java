package org.vadere.gui.onlinevisualization.control;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.vadere.gui.components.model.DefaultSimulationConfig;
import org.vadere.gui.components.model.SimulationModel;
import org.vadere.gui.components.utils.Resources;
import org.vadere.gui.components.view.SimulationRenderer;
import org.vadere.gui.onlinevisualization.view.IRendererChangeListener;
import org.vadere.gui.postvisualization.PostVisualisation;
import org.vadere.gui.postvisualization.utils.SVGGenerator;

import java.awt.event.ActionEvent;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.prefs.Preferences;

import javax.swing.*;

public class ActionGenerateSVG extends AbstractAction implements IRendererChangeListener {
	private static Logger logger = LogManager.getLogger(ActionGenerateSVG.class);
	private static Resources resources = Resources.getInstance("postvisualization");
	private final SVGGenerator svgGenerator;
	private final SimulationModel<? extends DefaultSimulationConfig> model;

	public ActionGenerateSVG(final String name, final Icon icon, final SimulationRenderer renderer,
			final SimulationModel<? extends DefaultSimulationConfig> model) {
		super(name, icon);
		this.svgGenerator = new SVGGenerator(renderer, model);
		this.model = model;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		Date todaysDate = new java.util.Date();
		SimpleDateFormat formatter = new SimpleDateFormat(resources.getProperty("View.dataFormat"));
		String formattedDate = formatter.format(todaysDate);

		JFileChooser fileChooser = new JFileChooser(Preferences.userNodeForPackage(PostVisualisation.class).get("PostVis.snapshotDirectory.path", "."));
		File outputFile = new File("pv_snapshot_" + formattedDate + ".svg");

		fileChooser.setSelectedFile(outputFile);

		int returnVal = fileChooser.showDialog(null, "Save");

		if (returnVal == JFileChooser.APPROVE_OPTION) {

			outputFile = fileChooser.getSelectedFile().toString().endsWith(".svg") ? fileChooser.getSelectedFile()
					: new File(fileChooser.getSelectedFile().toString() + ".svg");
			svgGenerator.generateSVG(outputFile);
		}

	}

	@Override
	public void update(SimulationRenderer renderer) {}
}
