package org.vadere.gui.projectview.view;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.vadere.simulator.projects.ScenarioRunManager;
import org.vadere.simulator.projects.dataprocessing.outputfile.OutputFile;
import org.vadere.simulator.projects.dataprocessing.processor.DataProcessor;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class DataProcessingView extends JPanel {

	private static Logger logger = LogManager.getLogger(DataProcessingView.class);

	private JTable filesTable;
	private DefaultTableModel filesTableModel;
	private JTable processorsTable;
	private DefaultTableModel processorsTableModel;

	private ScenarioRunManager currentScenario;
	private boolean isEditable;


	public DataProcessingView() {
		GridLayout gridLayout = new GridLayout(2, 2);
		setLayout(gridLayout);

		// set up table models

		filesTableModel = new DefaultTableModel(new OutputFile[] {null}, 0) {
			@Override
			public boolean isCellEditable(int row, int column) {
				return false;
			}
		};
		processorsTableModel = new DefaultTableModel(new DataProcessor[] {null}, 0) {
			@Override
			public boolean isCellEditable(int row, int column) {
				return false;
			}
		};

		// top left in 2x2 grid

		JPanel filesPanel = new JPanel();
		filesPanel.setLayout(new BoxLayout(filesPanel, BoxLayout.PAGE_AXIS));
		JLabel filesLabel = new JLabel("<html><b>Files</b></html>");
		filesLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		filesPanel.add(filesLabel);
		filesTable = new JTable(filesTableModel);
		filesTable.setAlignmentX(Component.LEFT_ALIGNMENT);
		filesPanel.add(filesTable);
		add(filesPanel);

		// top right in 2x2 grid

		JPanel filesDetailsPanel = new JPanel();
		add(filesDetailsPanel);

		// bottom left in 2x2 grid

		JPanel processorsPanel = new JPanel();
		processorsPanel.setLayout(new BoxLayout(processorsPanel, BoxLayout.PAGE_AXIS));
		JLabel processorsLabel = new JLabel("<html><b>Processors</b></html>");
		processorsLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		processorsPanel.add(processorsLabel);
		processorsTable = new JTable(processorsTableModel);
		processorsTable.setAlignmentX(Component.LEFT_ALIGNMENT);
		processorsPanel.add(processorsTable);
		add(processorsPanel);

		// bottom right in 2x2 grid

		JPanel processorsDetailsPanel = new JPanel();
		add(processorsDetailsPanel);
	}

	public void setVadereScenario(ScenarioRunManager scenario) {
		this.currentScenario = scenario;

		scenario.getDataProcessingJsonManager().getOutputFiles()
				.forEach(outputFile -> filesTableModel.addRow(new OutputFile[] {outputFile}));

		scenario.getDataProcessingJsonManager().getDataProcessors()
				.forEach(dataProcessor -> processorsTableModel.addRow(new DataProcessor[] {dataProcessor}));
	}

	public void isEditable(boolean isEditable) {
		this.isEditable = isEditable;
	}

}
