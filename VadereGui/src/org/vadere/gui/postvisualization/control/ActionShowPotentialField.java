package org.vadere.gui.postvisualization.control;


import org.vadere.gui.components.control.simulation.ActionVisualization;
import org.vadere.gui.components.utils.Resources;
import org.vadere.gui.components.view.DialogFactory;
import org.vadere.gui.postvisualization.model.PostvisualizationModel;
import org.vadere.gui.postvisualization.utils.PotentialFieldContainer;
import org.vadere.state.scenario.Topography;
import org.vadere.util.logging.Logger;

import java.awt.event.ActionEvent;
import java.io.File;

import javax.swing.*;

public class ActionShowPotentialField extends ActionVisualization {

	private static Logger logger = Logger.getLogger(ActionShowPotentialField.class);
	private static Resources resources = Resources.getInstance("postvisualization");
	private final PostvisualizationModel model;

	public ActionShowPotentialField(final String name, final Icon icon, final PostvisualizationModel model) {
		super(name, icon, model);
		this.model = model;
	}

	@Override
	public void actionPerformed(final ActionEvent event) {

		if (!model.config.isShowTargetPotentialField()) {
			//final JFileChooser fc = new JFileChooser(resources.getProperty("View.outputDirectory.path"));
			final JFileChooser fc = new JFileChooser(model.getOutputPath());
			int returnVal = fc.showOpenDialog(null);

			if (returnVal == JFileChooser.APPROVE_OPTION) {
				final File file = fc.getSelectedFile();
				resources.setProperty("SettingsDialog.outputDirectory.path", file.getParent());

				final JFrame dialog = DialogFactory.createLoadingDialog();
				dialog.setVisible(true);

				Runnable runnable = () -> {
					Player.getInstance(model).stop();

					try {
						Topography topography = model.getTopography();
						PotentialFieldContainer container = new PotentialFieldContainer(file,
								topography.getBounds().getWidth(), topography.getBounds().getHeight(), false);
						model.setPotentialFieldContainer(container);
						model.config.setShowTargetPotentialField(true);
						model.notifyObservers();
						// logger.info("read: \n" + data);
					} catch (Exception e) {
						e.printStackTrace();
						logger.error(e.getMessage());
						JOptionPane.showMessageDialog(
								null,
								resources.getProperty("InformationDialogFileError") + " - "
										+ e.getMessage(),
								resources.getProperty("InformationDialogError.title"),
								JOptionPane.ERROR_MESSAGE);
					}

					// when loading is finished, make frame disappear
					SwingUtilities.invokeLater(() -> dialog.dispose());

				};
				new Thread(runnable).start();
			}
		} else {
			model.config.setShowTargetPotentialField(false);
			model.notifyObservers();
		}
	}
}
