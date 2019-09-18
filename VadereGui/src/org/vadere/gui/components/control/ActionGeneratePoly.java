package org.vadere.gui.components.control;

import org.apache.commons.configuration2.Configuration;
import org.vadere.gui.components.control.simulation.ActionGeneratePNG;
import org.vadere.gui.components.model.DefaultModel;
import org.vadere.gui.components.model.DefaultSimulationConfig;
import org.vadere.gui.components.utils.Messages;
import org.vadere.meshing.mesh.impl.PSLG;
import org.vadere.meshing.utils.io.poly.PSLGGenerator;
import org.vadere.state.scenario.Obstacle;
import org.vadere.util.config.VadereConfig;
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
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.*;

public class ActionGeneratePoly extends AbstractAction {
	private static Logger logger = Logger.getLogger(ActionGeneratePNG.class);
	private static final Configuration CONFIG = VadereConfig.getConfig();
	private final DefaultModel<? extends DefaultSimulationConfig> model;

	public ActionGeneratePoly(final String name, Icon icon, final DefaultModel<? extends DefaultSimulationConfig> model) {
		super(name, icon);
		this.model = model;
	}

	@Override
	public void actionPerformed(final ActionEvent e) {
		JFileChooser fileChooser = new JFileChooser(CONFIG.getString("SettingsDialog.snapshotDirectory.path"));

		Date todaysDate = new java.util.Date();
		SimpleDateFormat formatter = new SimpleDateFormat(CONFIG.getString("SettingsDialog.dataFormat"));
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

			List<VPolygon> obsShapes = obstacles.stream()
					.map(obs -> obs.getShape())
					.map(shape -> new VPolygon(shape))
					.collect(Collectors.toList());

			// this computes the union of intersecting obstacles.
			obsShapes = PSLG.constructHoles(obsShapes);

			// this will help to construct a valid non-rectangular bound.
			List<VPolygon> polygons = PSLG.constructBound(new VPolygon(bound), obsShapes);

			String polyString = PSLGGenerator.toPSLG(
					polygons.get(0),
					polygons.size() > 1 ? polygons.subList(1, polygons.size()) : Collections.emptyList());
			try {
				outputFile.createNewFile();
				Writer out = new OutputStreamWriter(new FileOutputStream(outputFile), "UTF-8");
				out.write(polyString);
				out.flush();
				VadereConfig.getConfig().setProperty("SettingsDialog.snapshotDirectory.path", outputFile.getParentFile().getAbsolutePath());
				logger.info("generate new Poly.file: " + outputFile.getAbsolutePath());
			} catch (IOException e1) {
				logger.error(e1.getMessage());
				e1.printStackTrace();
			}
		}
	}
}
