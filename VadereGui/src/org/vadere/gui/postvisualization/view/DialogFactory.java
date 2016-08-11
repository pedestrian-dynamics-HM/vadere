package org.vadere.gui.postvisualization.view;

import javax.swing.*;

import org.vadere.gui.components.utils.Messages;
import org.vadere.gui.components.utils.Resources;
import org.vadere.gui.postvisualization.model.PostvisualizationModel;

import java.awt.*;

public class DialogFactory {

	private static Resources resources = Resources.getInstance("postvisualization");
	private static PostvisualizationModel currentModel;
	private static SettingsDialog settingsDialog;

	public static JFrame createLoadingDialog() {
		final JFrame frame = new JFrame(Messages.getString("LoadingDialog.title"));
		final JProgressBar progressBar = new JProgressBar();
		progressBar.setIndeterminate(true);
		final JPanel contentPane = new JPanel();
		contentPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		contentPane.setLayout(new BorderLayout());
		contentPane.add(new JLabel("Loading..."), BorderLayout.NORTH);
		contentPane.add(progressBar, BorderLayout.CENTER);
		frame.setContentPane(contentPane);
		frame.pack();
		frame.setLocationRelativeTo(null);
		frame.setAlwaysOnTop(true);
		frame.setResizable(false);
		return frame;
	}

	public static JDialog createSettingsDialog(final PostvisualizationModel model) {
		if (settingsDialog == null || currentModel == null || !currentModel.equals(model)) {
			currentModel = model;
			settingsDialog = new SettingsDialog(model);
		}
		return settingsDialog;
	}
}
