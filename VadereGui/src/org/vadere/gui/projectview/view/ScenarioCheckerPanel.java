package org.vadere.gui.projectview.view;

import org.vadere.gui.components.utils.Messages;
import org.vadere.gui.components.utils.Resources;
import org.vadere.gui.topographycreator.control.ActionScenarioChecker;
import org.vadere.gui.topographycreator.model.IDrawPanelModel;
import org.vadere.simulator.projects.Scenario;

import java.awt.*;

import javax.swing.*;

public class ScenarioCheckerPanel extends JPanel {
	private final static ImageIcon iconRed = new ImageIcon(Resources.class.getResource("/icons/light_red1.png"));
	private final static ImageIcon iconYellow = new ImageIcon(Resources.class.getResource("/icons/light_yellow1.png"));
	private final static ImageIcon iconGreen = new ImageIcon(Resources.class.getResource("/icons/light_green1.png"));


	private JLabel lblmsg;
	private JButton btnChecker;
	private ActionScenarioChecker action;

	public ScenarioCheckerPanel(){
		setLayout(new FlowLayout(FlowLayout.RIGHT, 5, 0));

		lblmsg = new JLabel();
		add(lblmsg);

		btnChecker = new JButton(iconGreen);
		btnChecker.setText("");
		add(btnChecker);

		action = new ActionScenarioChecker("ScenarioChecker", this);

		btnChecker.addActionListener(action);

		setRed();
		setMsgErr();
	}

	public void check(final Scenario scenario){
		action.check(scenario);
	}

	public void setRed(){
		btnChecker.setIcon(iconRed);
	}

	public void setYellow(){
		btnChecker.setIcon(iconYellow);
	}

	public void setGreen(){
		btnChecker.setIcon(iconGreen);
	}

	public void setMsgOK(){
		lblmsg.setText(Messages.getString("ScenarioChecker.msg.ok"));
	}

	public void setMsgWarn(){
		lblmsg.setText(Messages.getString("ScenarioChecker.msg.warn"));
	}

	public void setMsgErr(){
		lblmsg.setText(Messages.getString("ScenarioChecker.msg.err"));
	}

	public void observerIDrawPanelModel(IDrawPanelModel model){
		action.observerModel(model);
	}

	public void stopObserver(){
		btnChecker.removeActionListener(action);
		action = new ActionScenarioChecker("ScenarioChecker", this);
		btnChecker.addActionListener(action);
	}

	private JButton addActionToToolbar(final JToolBar toolbar, final Action action,
											  final String toolTipProperty) {
		JButton button = toolbar.add(action);
		button.setBorderPainted(false);
		button.setToolTipText(Messages.getString(toolTipProperty));
		return button;
	}

}
