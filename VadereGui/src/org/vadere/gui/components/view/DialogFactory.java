package org.vadere.gui.components.view;

import javax.swing.*;

import org.vadere.gui.components.model.DefaultSimulationConfig;
import org.vadere.gui.components.model.SimulationModel;
import org.vadere.gui.components.utils.Messages;
import org.vadere.gui.components.utils.SwingUtils;
import org.vadere.gui.components.view.SettingsDialog;
import org.vadere.gui.postvisualization.model.PostvisualizationModel;

import java.awt.*;

public class DialogFactory {

	private static SimulationModel<? extends DefaultSimulationConfig> currentModel;
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

	public static JDialog createSettingsDialog(final SimulationModel<? extends DefaultSimulationConfig> model) {
		currentModel = model;

		// Instead of reusing an old "SettingsDialog", create a new one to avoid any problems.
		if(model instanceof PostvisualizationModel) {
			settingsDialog = new org.vadere.gui.postvisualization.view.SettingsDialog((PostvisualizationModel)model);
		}
		else {
			settingsDialog = new SettingsDialog(model);
		}

		settingsDialog.initComponents();

		return settingsDialog;
	}
}
