package org.vadere.gui.postvisualization.control;

import java.awt.event.ActionEvent;
import java.io.IOException;

import javax.swing.Icon;
import javax.swing.JButton;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.vadere.gui.components.control.simulation.ActionVisualization;
import org.vadere.gui.components.utils.Resources;
import org.vadere.gui.postvisualization.model.PostvisualizationModel;
import org.vadere.gui.postvisualization.utils.IRecorder;
import org.vadere.gui.postvisualization.utils.MovRecorder;
import org.vadere.gui.postvisualization.view.ImageSizeDialog;
import org.vadere.gui.postvisualization.view.PostvisualizationRenderer;

public class ActionRecording extends ActionVisualization {
	private static Logger logger = LogManager.getLogger(ActionRecording.class);
	private static Resources resources = Resources.getInstance("postvisualization");
	private final PostvisualizationModel model;
	private JButton button;
	private final int iconWidth;
	private final int iconHeight;
	private IRecorder recorder;

	public ActionRecording(final String name, final Icon icon, final PostvisualizationRenderer renderer) {
		super(name, icon, renderer.getModel());
		this.iconWidth = Integer.parseInt(resources.getProperty("ProjectView.icon.width.value"));
		this.iconHeight = Integer.parseInt(resources.getProperty("ProjectView.icon.height.value"));
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
