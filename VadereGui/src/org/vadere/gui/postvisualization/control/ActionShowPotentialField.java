package org.vadere.gui.postvisualization.control;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.vadere.gui.components.utils.Resources;
import org.vadere.gui.postvisualization.model.PostvisualizationModel;
import org.vadere.gui.postvisualization.utils.PotentialFieldContainer;
import org.vadere.gui.postvisualization.view.DialogFactory;
import org.vadere.gui.projectview.VadereApplication;
import org.vadere.state.scenario.Topography;
import org.vadere.util.potential.CellGrid;
import org.vadere.util.potential.CellGridConverter;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.List;
import java.util.prefs.Preferences;

public class ActionShowPotentialField extends ActionVisualization {

	private static Logger logger = LogManager.getLogger(ActionShowPotentialField.class);
	private static Resources resources = Resources.getInstance("postvisualization");

	public ActionShowPotentialField(final String name, final Icon icon, final PostvisualizationModel model) {
		super(name, icon, model);
	}

	@Override
	public void actionPerformed(final ActionEvent event) {

		if (!model.config.isShowPotentialField()) {
			final JFileChooser fc = new JFileChooser(resources.getProperty("View.outputDirectory.path"));

			int returnVal = fc.showOpenDialog(null);

			if (returnVal == JFileChooser.APPROVE_OPTION) {
				final File file = fc.getSelectedFile();
				resources.setProperty("View.outputDirectory.path", file.getParent());

				final JFrame dialog = DialogFactory.createLoadingDialog();
				dialog.setVisible(true);

				Runnable runnable = () -> {
					Player.getInstance(model).stop();

					try {
						Topography topography = model.getTopography();
						PotentialFieldContainer container = new PotentialFieldContainer(file,
								topography.getBounds().getWidth(), topography.getBounds().getHeight(), true);
						model.setPotentialFieldContainer(container);
						model.config.setShowPotentialField(true);
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
			model.config.setShowPotentialField(false);
			model.notifyObservers();
		}
	}
}
