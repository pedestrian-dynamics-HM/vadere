package org.vadere.gui.projectview.view;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rsyntaxtextarea.Theme;
import org.vadere.simulator.projects.ScenarioRunManager;
import org.vadere.simulator.projects.dataprocessing.DataProcessingJsonManager;
import org.vadere.simulator.projects.dataprocessing.outputfile.NoDataKeyOutputFile;
import org.vadere.simulator.projects.dataprocessing.outputfile.OutputFile;
import org.vadere.simulator.projects.dataprocessing.outputfile.PedestrianIdOutputFile;
import org.vadere.simulator.projects.dataprocessing.outputfile.TimestepOutputFile;
import org.vadere.simulator.projects.dataprocessing.outputfile.TimestepPedestrianIdOutputFile;
import org.vadere.simulator.projects.dataprocessing.processor.DataProcessor;
import org.vadere.simulator.projects.io.JsonConverter;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;


public class DataProcessingView extends JPanel {

	private static Logger logger = LogManager.getLogger(DataProcessingView.class);

	private JTable filesTable;
	private DefaultTableModel filesTableModel;
	private JTable processorsTable;
	private DefaultTableModel processorsTableModel;
	private JPanel filesDetailsPanel;
	private JPanel processorsDetailsPanel;

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
			if (e.getValueIsAdjusting()) {
				handleOutputFileSelected((OutputFile) filesTableModel.getValueAt(filesTable.getSelectedRow(), 0));
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
			if (e.getValueIsAdjusting()) {
				handleDataProcessorSelected((DataProcessor) processorsTable.getValueAt(processorsTable.getSelectedRow(), 0));
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

		filesDetailsPanel = new JPanel();
		filesDetailsPanel.setLayout(new GridBagLayout());
		filesDetailsPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
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

		processorsDetailsPanel = new JPanel();
		processorsDetailsPanel.setLayout(new GridBagLayout());
		processorsDetailsPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
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

		filesTableModel.setRowCount(0);
		filesDetailsPanel.removeAll();
		scenario.getDataProcessingJsonManager().getOutputFiles()
				.forEach(outputFile -> filesTableModel.addRow(new OutputFile[] {outputFile}));

		processorsTableModel.setRowCount(0);
		processorsDetailsPanel.removeAll();
		scenario.getDataProcessingJsonManager().getDataProcessors()
				.forEach(dataProcessor -> processorsTableModel.addRow(new DataProcessor[] {dataProcessor}));
	}

	public void isEditable(boolean isEditable) {
		this.isEditable = isEditable;
	}

	private void handleOutputFileSelected(OutputFile outputFile) {
		filesDetailsPanel.removeAll();

		GridBagConstraints c = new GridBagConstraints();
		c.anchor = GridBagConstraints.WEST; // alignment
		c.ipady = 15; // y-gap between components

		c.gridx = 0;
		c.gridy = 0;
		c.gridwidth = 2;
		filesDetailsPanel.add(new JLabel("<html><b>" + outputFile.getFileName() + "</b></html>"), c);

		c.gridx = 0;
		c.gridy = 1;
		c.gridwidth = 1;
		filesDetailsPanel.add(new JLabel("Type: "), c);

		String[] options = new String[]{
				NoDataKeyOutputFile.class.getSimpleName(),
				PedestrianIdOutputFile.class.getSimpleName(),
				TimestepOutputFile.class.getSimpleName(),
				TimestepPedestrianIdOutputFile.class.getSimpleName()
		};
		JComboBox typesComboBox = new JComboBox(options);
		typesComboBox.setSelectedItem(outputFile.getClass().getSimpleName());

		c.gridx = 1;
		c.gridy = 1;
		filesDetailsPanel.add(typesComboBox, c);

		StringBuilder sb = new StringBuilder();
		outputFile.getProcessorIds().forEach(id -> sb.append(", ").append(id));
		JTextField processorIDsTextField = new JTextField();
		processorIDsTextField.setText(sb.length() > 2 ? sb.substring(2) : "");

		c.gridx = 0;
		c.gridy = 2;
		filesDetailsPanel.add(new JLabel("Processors: "), c);

		c.gridx = 1;
		c.gridy = 2;
		c.fill = GridBagConstraints.HORIZONTAL;
		filesDetailsPanel.add(processorIDsTextField, c);

		revalidate();
		repaint(); // inelegantly, it needs both revalidate() and repaint() stackoverflow.com/a/5812780
	}

	private void handleDataProcessorSelected(DataProcessor dataProcessor) {
		processorsDetailsPanel.removeAll();

		GridBagConstraints c = new GridBagConstraints();
		c.ipady = 15;

		c.gridx = 0;
		c.gridy = 0;
		processorsDetailsPanel.add(new JLabel("<html><b>" + dataProcessor.getType() + "</b></html>"), c);

		RSyntaxTextArea attributesTextArea = new RSyntaxTextArea();
		attributesTextArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JSON);
		InputStream in = getClass().getResourceAsStream("/syntaxthemes/idea.xml");
		try {
			Theme syntaxTheme = Theme.load(in);
			syntaxTheme.apply(attributesTextArea);
			attributesTextArea.setText(JsonConverter.serializeJsonNode(DataProcessingJsonManager.serializeProcessor(dataProcessor)));
		} catch (IOException e) {
			e.printStackTrace();
		}

		c.gridx = 0;
		c.gridy = 1;
		processorsDetailsPanel.add(attributesTextArea, c);

		revalidate();
		repaint();
	}

}
