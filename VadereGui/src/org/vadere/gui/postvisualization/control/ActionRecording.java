package org.vadere.gui.postvisualization.control;

import java.awt.event.ActionEvent;
import java.io.IOException;

import javax.swing.Icon;
import javax.swing.JButton;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.vadere.gui.components.utils.Resources;
import org.vadere.gui.postvisualization.model.PostvisualizationModel;
import org.vadere.gui.postvisualization.utils.IRecorder;
import org.vadere.gui.postvisualization.utils.MovRecorder;
import org.vadere.gui.postvisualization.view.ImageSizeDialog;
import org.vadere.gui.postvisualization.view.PostvisualizationRenderer;

public class ActionRecording extends ActionVisualization {
	private static Logger logger = LogManager.getLogger(ActionRecording.class);
	private static Resources resources = Resources.getInstance("postvisualization");

	private JButton button;
	private final int iconWidth;
	private final int iconHeight;
	private IRecorder recorder;

	public ActionRecording(final String name, final Icon icon, final PostvisualizationRenderer renderer) {
		super(name, icon, renderer.getModel());
		this.iconWidth = Integer.parseInt(resources.getProperty("View.icon.width.value"));
		this.iconHeight = Integer.parseInt(resources.getProperty("View.icon.height.value"));
		this.recorder = new MovRecorder(renderer);
		model.addObserver(this.recorder);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		try {
			if (button != null) {
				if (model.config.isRecording()) {
					button.setIcon(resources.getIcon("record.png", iconWidth, iconHeight));
					recorder.stopRecording();
				} else {
					ImageSizeDialog imageSizeDialog = new ImageSizeDialog(model);
					if (imageSizeDialog.getState() == ImageSizeDialog.State.Ok) {
						button.setIcon(resources.getIcon("stop_record.png", iconWidth, iconHeight));
						recorder.startRecording(imageSizeDialog.getImageBound());
					}
				}
				model.config.setRecording(!model.config.isRecording());
			}
		} catch (IOException ioe) {
			ioe.printStackTrace();
			logger.error(ioe.getMessage());
		}
	}

	public void setButton(final JButton button) {
		this.button = button;
	}
}
