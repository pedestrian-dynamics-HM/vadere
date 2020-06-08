package org.vadere.gui.components.control.simulation;

import org.apache.commons.configuration2.Configuration;
import org.vadere.gui.components.model.SimulationModel;
import org.vadere.gui.components.utils.Resources;
import org.vadere.gui.components.view.SimulationRenderer;
import org.vadere.gui.postvisualization.utils.IRecorder;
import org.vadere.gui.postvisualization.utils.MovRecorder;
import org.vadere.gui.postvisualization.view.ImageSizeDialog;
import org.vadere.util.config.VadereConfig;
import org.vadere.util.logging.Logger;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.IOException;

public class ActionRecording extends ActionVisualization {
	private static Logger logger = Logger.getLogger(ActionRecording.class);
	private static final Configuration CONFIG = VadereConfig.getConfig();
	private static Resources resources = Resources.getInstance("global");

	private final SimulationModel model;
	private JButton button;
	private final int iconWidth;
	private final int iconHeight;
	private IRecorder recorder;

	public ActionRecording(final String name, final Icon icon, final SimulationRenderer renderer) {
		super(name, icon, renderer.getModel());
		this.iconWidth = CONFIG.getInt("ProjectView.icon.width.value");
		this.iconHeight = CONFIG.getInt("ProjectView.icon.height.value");
		this.recorder = new MovRecorder(renderer);
		this.model = renderer.getModel();
		this.model.addObserver(this.recorder);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (button != null) {
			if (model.config.isRecording()) {
				try {
					recorder.stopRecording();
				} catch (IOException e1) {
					e1.printStackTrace();
					logger.warn("recording failed.");
				}
				finally {
					button.setIcon(resources.getIcon("record.png", iconWidth, iconHeight));
					model.config.setRecording(!model.config.isRecording());
				}

			} else {
				ImageSizeDialog imageSizeDialog = new ImageSizeDialog(model);
				if (imageSizeDialog.getState() == ImageSizeDialog.State.Ok) {
					try {
						recorder.startRecording(imageSizeDialog.getImageBound());
					} catch (IOException e1) {
						e1.printStackTrace();
						logger.warn("start recording failed.");
					}
					finally {
						button.setIcon(resources.getIcon("stop_record.png", iconWidth, iconHeight));
						model.config.setRecording(!model.config.isRecording());
					}
				}
			}

		}
	}

	public void setButton(final JButton button) {
		this.button = button;
	}
}
