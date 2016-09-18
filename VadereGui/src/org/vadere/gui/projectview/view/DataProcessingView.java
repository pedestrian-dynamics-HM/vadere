package org.vadere.gui.projectview.view;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.vadere.simulator.projects.ScenarioRunManager;
import org.vadere.simulator.projects.dataprocessing.outputfile.OutputFile;
import org.vadere.simulator.projects.dataprocessing.processor.DataProcessor;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;

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
		filesTable = new JTable(filesTableModel);
		filesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		filesTable.getSelectionModel().addListSelectionListener(e -> {
			if (!e.getValueIsAdjusting()) {
				handleOutputFileSelected((OutputFile) filesTableModel.getValueAt(e.getFirstIndex(), 0));
			}
		});

		processorsTableModel = new DefaultTableModel(new DataProcessor[] {null}, 0) {
			@Override
			public boolean isCellEditable(int row, int column) {
				return false;
			}
		};
		processorsTable = new JTable(processorsTableModel);
		processorsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		processorsTable.getSelectionModel().addListSelectionListener(e -> {
			if (!e.getValueIsAdjusting()) {
				handleDataProcessorSelected((DataProcessor) processorsTable.getValueAt(e.getFirstIndex(), 0));
			}
		});

		// top left in 2x2 grid

		JButton addFileBtn = new JButton(new AbstractAction("Add file") {
			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO
			}
		});
		add(buildPanel("Files", filesTable, addFileBtn));

		// top right in 2x2 grid

		JPanel filesDetailsPanel = new JPanel();
		add(filesDetailsPanel);

		// bottom left in 2x2 grid

		JButton addProcessorBtn = new JButton(new AbstractAction("Add processor") {
			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO
			}
		});
		add(buildPanel("Processors", processorsTable, addProcessorBtn));

		// bottom right in 2x2 grid

		JPanel processorsDetailsPanel = new JPanel();
		add(processorsDetailsPanel);
	}

	private JPanel buildPanel(String labelText, JTable table, JButton addBtn) { // used for OutputFile-Table and DataProcessor-Table
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		JLabel label = new JLabel("<html><b>" + labelText + "</b></html>");
		label.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		label.setAlignmentX(Component.RIGHT_ALIGNMENT);
		panel.add(label);
		table.setTableHeader(null);
		JScrollPane tableScrollPane = new JScrollPane(table);
		tableScrollPane.setAlignmentX(Component.RIGHT_ALIGNMENT);
		panel.add(tableScrollPane);
		panel.add(Box.createRigidArea(new Dimension(0, 10)));
		addBtn.setAlignmentX(Component.RIGHT_ALIGNMENT); // for some reason this works only if the two components above are also set to right-align, even so then they left-align :)
		panel.add(addBtn);
		return panel;
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

	private void handleOutputFileSelected(OutputFile outputFile) {
		// TODO
	}

	private void handleDataProcessorSelected(DataProcessor dataProcessor) {
		// TODO
	}

}
