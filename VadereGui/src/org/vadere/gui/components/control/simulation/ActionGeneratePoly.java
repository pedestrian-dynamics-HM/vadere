package org.vadere.gui.components.control.simulation;

import org.vadere.gui.components.model.DefaultSimulationConfig;
import org.vadere.gui.components.model.SimulationModel;
import org.vadere.gui.components.utils.Messages;
import org.vadere.gui.components.utils.Resources;
import org.vadere.gui.components.view.SimulationRenderer;
import org.vadere.gui.postvisualization.PostVisualisation;
import org.vadere.meshing.utils.io.poly.PolyGenerator;
import org.vadere.state.scenario.Obstacle;
import org.vadere.util.geometry.shapes.VPolygon;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.logging.Logger;

import java.awt.event.ActionEvent;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

import javax.swing.*;

public class ActionGeneratePoly extends AbstractAction {
	private static Logger logger = Logger.getLogger(ActionGeneratePNG.class);
	private static Resources resources = Resources.getInstance("global");
	private final SimulationModel<? extends DefaultSimulationConfig> model;

	public ActionGeneratePoly(final String name, Icon icon, final SimulationRenderer renderer,
	                         final SimulationModel<? extends DefaultSimulationConfig> model) {
		super(name, icon);
		this.model = model;
	}

	@Override
	public void actionPerformed(final ActionEvent e) {
		JFileChooser fileChooser = new JFileChooser(Preferences.userNodeForPackage(PostVisualisation.class).get("SettingsDialog.snapshotDirectory.path", "."));

		Date todaysDate = new java.util.Date();
		SimpleDateFormat formatter = new SimpleDateFormat(resources.getProperty("SettingsDialog.dataFormat"));
		String formattedDate = formatter.format(todaysDate);


		File outputFile = new File(Messages.getString("FileDialog.filenamePrefix") + formattedDate + ".poly");
		fileChooser.setSelectedFile(outputFile);

		int returnVal = fileChooser.showDialog(null, "Save");

		if (returnVal == JFileChooser.APPROVE_OPTION) {

			outputFile = fileChooser.getSelectedFile().toString().endsWith(".poly") ? fileChooser.getSelectedFile()
					: new File(fileChooser.getSelectedFile().toString() + ".poly");

			List<Obstacle> boundingObstacles = model.getTopography().getBoundaryObstacles();


			Rectangle2D.Double boundWithBorder = model.getTopography().getBounds();
			double boundWidth = model.getTopography().getBoundingBoxWidth();
			VRectangle bound = new VRectangle(boundWithBorder.x + boundWidth, boundWithBorder.y + boundWidth, boundWithBorder.width - 2*boundWidth, boundWithBorder.height - 2*boundWidth);

			List<Obstacle> obstacles = new ArrayList<>(model.getTopography().getObstacles());
			obstacles.removeAll(model.getTopography().getBoundaryObstacles());
			String polyString = PolyGenerator.toPSLG(
					new VPolygon(bound),
					obstacles.stream()
							.map(obs -> obs.getShape())
							.map(shape -> new VPolygon(shape))
							.collect(Collectors.toList()));
			try {
				outputFile.createNewFile();
				Writer out = new OutputStreamWriter(new FileOutputStream(outputFile), "UTF-8");
				out.write(polyString);
				out.flush();
				logger.info("generate new Poly.file: " + outputFile.getAbsolutePath());
			} catch (IOException e1) {
				logger.error(e1.getMessage());
				e1.printStackTrace();
			}
		}
	}
}
