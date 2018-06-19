package org.vadere.gui.postvisualization.control;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.vadere.gui.components.utils.Resources;
import org.vadere.gui.postvisualization.PostVisualisation;
import org.vadere.gui.postvisualization.utils.SVGGenerator;
import org.vadere.gui.postvisualization.utils.TikZGenerator;
import org.vadere.gui.postvisualization.view.PostvisualizationRenderer;
import org.vadere.state.scenario.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.prefs.Preferences;

public class ActionGenerateTikZ extends ActionVisualization {
	private static Logger logger = LogManager.getLogger(ActionGenerateTikZ.class);
	private static Resources resources = Resources.getInstance("postvisualization");
	private final TikZGenerator tikZGenerator;

	public ActionGenerateTikZ(final String name, final Icon icon, final PostvisualizationRenderer renderer) {
		super(name, icon, renderer.getModel());
		this.tikZGenerator = new TikZGenerator(renderer, renderer.getModel());
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		Date todaysDate = new Date();
		SimpleDateFormat formatter = new SimpleDateFormat(resources.getProperty("View.dataFormat"));
		String formattedDate = formatter.format(todaysDate);

		JFileChooser fileChooser = new JFileChooser(Preferences.userNodeForPackage(PostVisualisation.class).get("PostVis.snapshotDirectory.path", "."));
		File outputFile = new File("pv_snapshot_" + formattedDate + ".tex");

		fileChooser.setSelectedFile(outputFile);

		int returnVal = fileChooser.showDialog(null, "Save");

		if (returnVal == JFileChooser.APPROVE_OPTION) {

			outputFile = fileChooser.getSelectedFile().toString().endsWith(".tex") ? fileChooser.getSelectedFile()
					: new File(fileChooser.getSelectedFile().toString() + ".tex");

			boolean completeDocument = true;
			tikZGenerator.generateTikZ(outputFile, completeDocument);
		}
	}
}
