package org.vadere.gui.projectview.view;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.vadere.simulator.projects.ScenarioRunManager;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class DataProcessingView extends JPanel {

	private static Logger logger = LogManager.getLogger(DataProcessingView.class);

	private JTable filesTable;
	private JTable processorsTable;
	private ScenarioRunManager currentScenario;
	private boolean isEditable;


	public DataProcessingView() {
		GridLayout gridLayout = new GridLayout(2, 2);
		setLayout(gridLayout);

		DefaultTableModel filesTableModel = new DefaultTableModel(new String[] {"Files"}, 0);
		filesTableModel.addRow(new String[] {"test.txt"});
		filesTable = new JTable(filesTableModel);
		JPanel filesPanel = new JPanel();
		filesPanel.add(new JLabel("Files:"));
		filesPanel.add(filesTable);
		add(filesPanel);

		JPanel filesDetailsPanel = new JPanel();
		filesDetailsPanel.setBorder(BorderFactory.createLineBorder(Color.red));
		add(filesDetailsPanel);

		DefaultTableModel processorsTableModel = new DefaultTableModel(new String[] {"ID", "Name"}, 0);
		processorsTableModel.addRow(new String[] {"0", "PedestrianPositionProcessor"});
		processorsTable = new JTable(processorsTableModel);
		JPanel processorsPanel = new JPanel();
		processorsPanel.add(new JLabel("Processors:"));
		processorsPanel.add(processorsTable);
		add(processorsPanel);

		JPanel processorsDetailsPanel = new JPanel();
		processorsDetailsPanel.setBorder(BorderFactory.createLineBorder(Color.blue));
		add(processorsDetailsPanel);
	}

	public void setVadereScenario(ScenarioRunManager scenario) {
		this.currentScenario = scenario;
	}

	public void isEditable(boolean isEditable) {
		this.isEditable = isEditable;
	}

}
