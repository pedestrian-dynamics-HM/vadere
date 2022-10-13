package org.vadere.gui.components.control;

import org.apache.commons.configuration2.Configuration;
import org.vadere.gui.components.control.simulation.ActionGeneratePNG;
import org.vadere.gui.components.model.DefaultModel;
import org.vadere.gui.components.model.DefaultSimulationConfig;
import org.vadere.gui.components.utils.Messages;
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

	public ActionGeneratePoly(final String name, Icon icon,String shortDescription, final DefaultModel<? extends DefaultSimulationConfig> model) {
		super(name, icon);
		putValue(SHORT_DESCRIPTION,Messages.getString(shortDescription));
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
