package org.vadere.gui.components.control;

import org.apache.commons.configuration2.Configuration;
import org.vadere.gui.components.control.simulation.ActionGeneratePNG;
import org.vadere.gui.components.model.DefaultModel;
import org.vadere.gui.components.model.DefaultSimulationConfig;
import org.vadere.gui.components.utils.Localization;
import org.vadere.gui.components.utils.ResourceStrings;
import org.vadere.gui.components.utils.Resources;
import org.vadere.meshing.mesh.impl.PSLG;
import org.vadere.meshing.utils.io.poly.PSLGGenerator;
import org.vadere.simulator.utils.pslg.PSLGConverter;
import org.vadere.util.config.VadereConfig;
import org.vadere.util.logging.Logger;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ActionGeneratePoly extends AbstractAction {
	private static final Logger logger = Logger.getLogger(ActionGeneratePNG.class);
	private static final Configuration CONFIG = VadereConfig.getConfig();
	private final DefaultModel<? extends DefaultSimulationConfig> model;
	private static final Resources RESOURCE = Resources.getInstance("global");
	private static final int ICON_SIZE = (int)(VadereConfig.getConfig().getInt("ProjectView.icon.height.value")*VadereConfig.getConfig().getFloat("Gui.scale"));

	public ActionGeneratePoly(final DefaultModel<? extends DefaultSimulationConfig> model) {
		super(Localization.getString("ProjectView.btnPolySnapshot.tooltip"), RESOURCE.getIconSVG("camera_poly", ICON_SIZE,ICON_SIZE));
		putValue(SHORT_DESCRIPTION, Localization.getString(ResourceStrings.TOPOGRAPHY_CREATOR_BTN_GENERATE_POLY_TOOLTIP));
		this.model = model;
	}

	@Override
	public void actionPerformed(final ActionEvent e) {
		JFileChooser fileChooser = new JFileChooser(CONFIG.getString("SettingsDialog.snapshotDirectory.path"));

		Date todaysDate = new java.util.Date();
		SimpleDateFormat formatter = new SimpleDateFormat(CONFIG.getString("SettingsDialog.dataFormat"));
		String formattedDate = formatter.format(todaysDate);


		File outputFile = new File(Localization.getString("FileDialog.filenamePrefix") + formattedDate + ".poly");
		fileChooser.setSelectedFile(outputFile);

		int returnVal = fileChooser.showDialog(null, "Save");

		if (returnVal == JFileChooser.APPROVE_OPTION) {

			outputFile = fileChooser.getSelectedFile().toString().endsWith(".poly") ? fileChooser.getSelectedFile()
					: new File(fileChooser.getSelectedFile().toString() + ".poly");

			PSLGConverter pslgConverter = new PSLGConverter();
			PSLG pslg = pslgConverter.toPSLG(model.getTopography());
			String polyString = PSLGGenerator.toPSLG(pslg.getSegmentBound(), pslg.getHoles());

			try {
				outputFile.createNewFile();
				Writer out = new OutputStreamWriter(new FileOutputStream(outputFile), StandardCharsets.UTF_8);
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
