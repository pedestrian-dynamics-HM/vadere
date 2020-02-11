package org.vadere.manager;

import org.vadere.gui.onlinevisualization.OnlineVisualization;
import org.vadere.simulator.projects.Scenario;
import org.vadere.simulator.projects.SingleScenarioFinishedListener;

import java.awt.*;

import javax.swing.*;

/**
 * Simple JFrame to wrap the OnlineVisualization for the server execution.
 */
public class ServerView extends JFrame implements SingleScenarioFinishedListener {

	private static ServerView mainWindow;

	private ServerView() throws HeadlessException {
		ServerView.mainWindow = this;

		setTitle("Vadere GUI - Server");
		setBounds(100, 100, 1000, 600);
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

	}

	public static void close() {
		EventQueue.invokeLater(() -> {
			mainWindow.setVisible(false);
			mainWindow.dispose();
		});
	}

	public static void startServerGui(OnlineVisualization onlineVisualization) {
		EventQueue.invokeLater(() -> {

			ServerView frame = new ServerView();
			frame.setVisible(true);
			frame.setSize(1200, 800);
			frame.add(onlineVisualization.getVisualizationPanel());
			onlineVisualization.getMainPanel().setVisible(true);
		});
	}

	@Override
	public void preScenarioRun(Scenario scenario, int scenariosLeft) {

	}

	@Override
	public void postScenarioRun(Scenario scenario, int scenariosLeft) {

	}

	@Override
	public void scenarioStarted(Scenario scenario, int scenariosLeft) {

	}

	@Override
	public void error(Scenario scenario, int scenariosLeft, Throwable throwable) {

	}

	@Override
	public void scenarioPaused(Scenario scenario, int scenariosLeft) {

	}

	@Override
	public void scenarioInterrupted(Scenario scenario, int scenariosLeft) {

	}

}
