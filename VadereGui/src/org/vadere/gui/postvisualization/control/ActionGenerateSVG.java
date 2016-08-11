package org.vadere.gui.postvisualization.control;

import java.awt.event.ActionEvent;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.prefs.Preferences;

import javax.swing.Icon;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.vadere.gui.components.utils.Resources;
import org.vadere.gui.postvisualization.PostVisualisation;
import org.vadere.gui.postvisualization.utils.SVGGenerator;
import org.vadere.gui.postvisualization.view.PostvisualizationRenderer;

public class ActionGenerateSVG extends ActionVisualization {
	private static Logger logger = LogManager.getLogger(ActionGenerateSVG.class);
	private static Resources resources = Resources.getInstance("postvisualization");
	private final SVGGenerator svgGenerator;

	public ActionGenerateSVG(final String name, final Icon icon, final PostvisualizationRenderer renderer) {
		super(name, icon, renderer.getModel());
		this.svgGenerator = new SVGGenerator(renderer, renderer.getModel());
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		Date todaysDate = new java.util.Date();
		SimpleDateFormat formatter = new SimpleDateFormat(resources.getProperty("View.dataFormat"));
		String formattedDate = formatter.format(todaysDate);
		File outputfile = new File(
				Preferences.userNodeForPackage(PostVisualisation.class).get("PostVis.snapshotDirectory.path", ".")
						+ System.getProperty("file.separator") + "pv_snapshot_" + formattedDate + ".svg");
		svgGenerator.generateSVG(outputfile);
	}
}
