package org.vadere.gui.projectview.view;

import org.vadere.gui.components.utils.Resources;
import org.vadere.gui.topographycreator.control.ActionScenarioChecker;
import org.vadere.gui.topographycreator.model.IDrawPanelModel;
import org.vadere.simulator.projects.Scenario;

import java.awt.*;

import javax.swing.*;

public class ScenarioNamePanel extends JPanel {

	private final static ImageIcon iconRed = new ImageIcon(Resources.class.getResource("/icons/light_red1.png"));
	private final static ImageIcon iconYellow = new ImageIcon(Resources.class.getResource("/icons/light_yellow1.png"));
	private final static ImageIcon iconGreen = new ImageIcon(Resources.class.getResource("/icons/light_green1.png"));

	private JLabel lblScenarioName;
	private JButton btnScenarioChecker;
	private ActionScenarioChecker action;

	public ScenarioNamePanel(){
		btnScenarioChecker = new JButton(iconGreen);
		btnScenarioChecker.setBorderPainted(false);
		btnScenarioChecker.setBorder(null);
		btnScenarioChecker.setMargin(new Insets(0, 0, 0, 0));
		btnScenarioChecker.setContentAreaFilled(false);
		btnScenarioChecker.addActionListener(e-> System.out.println("ddd"));
		btnScenarioChecker.setRolloverIcon(iconRed);
		action = new ActionScenarioChecker("ScenarioChecker", this);
		add(btnScenarioChecker);

		lblScenarioName = new JLabel();
		lblScenarioName.setHorizontalAlignment(SwingConstants.CENTER);
		add(lblScenarioName);
	}

	public void setScenarioNameLabel(JLabel label){
		removeAll();
		lblScenarioName = label;
		add(btnScenarioChecker);
		add(lblScenarioName);
	}

	public void setScenarioName(String scenarioName){
		lblScenarioName.setText(scenarioName);
		lblScenarioName.setHorizontalAlignment(SwingConstants.CENTER);
	}

	public void observerIDrawPanelModel(IDrawPanelModel model){
		action.observerModel(model);
	}

	public void check(final Scenario scenario){
		action.check(scenario);
	}

	public void stopObserver(){
		btnScenarioChecker.removeActionListener(action);
		action = new ActionScenarioChecker("ScenarioChecker", this);
		btnScenarioChecker.addActionListener(action);
	}

	public void setRed(){
		btnScenarioChecker.setIcon(iconRed);
	}

	public void setYellow(){
		btnScenarioChecker.setIcon(iconYellow);
	}

	public void setGreen(){
		btnScenarioChecker.setIcon(iconGreen);
	}

}
