package org.vadere.gui.projectview.view;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import info.clearthought.layout.TableLayout;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rsyntaxtextarea.Theme;
import org.vadere.gui.components.utils.Messages;
import org.vadere.gui.components.view.JComboCheckBox;
import org.vadere.simulator.projects.ScenarioRunManager;
import org.vadere.simulator.projects.dataprocessing.DataProcessingJsonManager;
import org.vadere.simulator.projects.dataprocessing.outputfile.NoDataKeyOutputFile;
import org.vadere.simulator.projects.dataprocessing.outputfile.OutputFile;
import org.vadere.simulator.projects.dataprocessing.outputfile.PedestrianIdOutputFile;
import org.vadere.simulator.projects.dataprocessing.outputfile.TimestepOutputFile;
import org.vadere.simulator.projects.dataprocessing.outputfile.TimestepPedestrianIdOutputFile;
import org.vadere.simulator.projects.dataprocessing.processor.DataProcessor;
import org.vadere.simulator.projects.dataprocessing.store.OutputFileStore;
import org.vadere.simulator.projects.io.JsonConverter;
import org.vadere.util.io.IOUtils;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;


public class DataProcessingView extends JPanel implements IJsonView {

	private static Logger logger = LogManager.getLogger(DataProcessingView.class);

	private IJsonView activeJsonView; // gui-mode or expert-mode
	private JLabel switchJsonViewModeLabel = new JLabel();
	private JPanel viewPanel; // hosts the gui-panel or the expert-panel
	private static final String guiViewMode = "gui";
	private static final String jsonViewMode = "json";
	private boolean inGuiViewMode = true;

	private ScenarioRunManager currentScenario;
	private boolean isEditable;


	public DataProcessingView() {
		setLayout(new BorderLayout()); // force it to span across the whole available space

		viewPanel = new JPanel(new GridLayout(1, 1));
		add(viewPanel, BorderLayout.CENTER);

		String dataProcessingViewModeStr = Preferences.userNodeForPackage(DataProcessingView.class).get("dataProcessingViewMode", "");
		if (dataProcessingViewModeStr.length() > 0) {
			inGuiViewMode = dataProcessingViewModeStr.equals(guiViewMode);
		}

		switchJsonViewModeLabel.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				inGuiViewMode = !inGuiViewMode;
				switchMode();
			}
		});
		JPanel togglePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		togglePanel.add(switchJsonViewModeLabel);
		add(togglePanel, BorderLayout.SOUTH);

		switchMode();
	}

	private void switchMode() {
		switchJsonViewModeLabel.setText("<html><span style='font-size:8px'><font color='gray'>" +
				"switch to <b>" + (inGuiViewMode ? jsonViewMode : guiViewMode) + "</b> mode<font>");
		Preferences.userNodeForPackage(DataProcessingView.class).put("dataProcessingViewMode", inGuiViewMode ? guiViewMode : jsonViewMode);

		viewPanel.removeAll();

		if (inGuiViewMode) {
			GuiView guiView = new GuiView();
			activeJsonView = guiView;
			viewPanel.add(guiView);
		} else {
			TextView expertView = buildExpertView();
			activeJsonView = expertView;
			viewPanel.add(expertView);
		}

		if (currentScenario != null) {
			activeJsonView.setVadereScenario(currentScenario);
			activeJsonView.isEditable(isEditable);
		}

		revalidate();
		repaint();
	}

	private TextView buildExpertView() {
		TextView panel = new TextView("/" + IOUtils.OUTPUT_DIR, "default_directory_outputprocessors", AttributeType.OUTPUTPROCESSOR);

		JMenuBar processorsMenuBar = new JMenuBar();
		JMenu processorsMenu = new JMenu(Messages.getString("Tab.Model.loadTemplateMenu.title"));
		processorsMenuBar.add(processorsMenu);

		try {
			File[] templateFiles = new File(this.getClass().getResource("/outputTemplates/").getPath()).listFiles();

			for (File templateFile : Arrays.stream(templateFiles).filter(f -> f.isFile()).collect(Collectors.toList())) {
				String templateFileName = templateFile.getName();
				String templateJson = org.apache.commons.io.IOUtils.toString(this.getClass().getResourceAsStream("/outputTemplates/" + templateFileName), "UTF-8");

				processorsMenu.add(new JMenuItem(new AbstractAction(templateFileName) {
					@Override
					public void actionPerformed(ActionEvent e) {
						if (JOptionPane.showConfirmDialog(ProjectView.getMainWindow(),
								Messages.getString("Tab.Model.confirmLoadTemplate.text"),
								Messages.getString("Tab.Model.confirmLoadTemplate.title"),
								JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
							try {
								panel.setText(templateJson);
							} catch (Exception e1) {
								e1.printStackTrace();
							}
						}
					}
				}));
			}
			panel.getPanelTop().add(processorsMenuBar, 0);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return panel;
	}

	@Override
	public void setVadereScenario(ScenarioRunManager scenario) {
		this.currentScenario = scenario;
		activeJsonView.setVadereScenario(scenario);
	}

	@Override
	public void isEditable(boolean isEditable) {
		this.isEditable = isEditable;
		activeJsonView.isEditable(isEditable);
	}


	private class GuiView extends JPanel implements IJsonView {

		private ScenarioRunManager currentScenario;
		private boolean isEditable;

		private JTable outputFilesTable;
		private DefaultTableModel outputFilesTableModel;
		private JTable dataProcessorsTable;
		private DefaultTableModel dataProcessorsTableModel;
		private JPanel outputFilesDetailsPanel;
		private JPanel dataProcessorsDetailsPanel;
		private List<Component> editableComponents = new ArrayList<>();

		GuiView() {
			/* via www.oracle.com/technetwork/java/tablelayout-141489.html,
			I don't like adding a maven dependency for a swing layout,
			but all the native ones can't seem to ensure relative column width */
			double size[][] = {{0.35, 0.65}, {TableLayout.FILL}};
			setLayout(new TableLayout(size));

			JPanel tableSide = new JPanel(new GridLayout(2, 1)); // one column, two equally sized rows
			add(tableSide, "0, 0");

			JPanel detailsSide = new JPanel(new GridLayout(2, 1)); // one column, two equally sized rows
			add(detailsSide, "1, 0");

			// tables side

			setupTables();

			JButton addFileBtn = new JButton(new AbstractAction("Add") {
				@Override
				public void actionPerformed(ActionEvent e) {
					String filename = "out.txt";
					int count = 1;
					while (outputFileNameAlreadyExists(filename)) { // ensure unique suggested filename
						filename = "out" + (count ++) + ".txt";
					}
					OutputFileStore outputFileStore = new OutputFileStore();
					outputFileStore.setFilename(filename);
					currentScenario.getDataProcessingJsonManager().addOutputFile(outputFileStore);
					updateOutputFilesTable();
					int index = outputFilesTableModel.getRowCount() - 1;
					outputFilesTable.setRowSelectionInterval(index, index);
				}
			});
			tableSide.add(buildPanel("Files", outputFilesTable, addFileBtn));

			JButton addProcessorBtn = new JButton(new AbstractAction("Add") {
				@Override
				public void actionPerformed(ActionEvent e) {
					// TODO
				}
			});
			tableSide.add(buildPanel("Processors", dataProcessorsTable, addProcessorBtn));

			// details side

			outputFilesDetailsPanel = new JPanel();
			outputFilesDetailsPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
			outputFilesDetailsPanel.setBorder(BorderFactory.createEmptyBorder(40, 10, 0, 0));
			detailsSide.add(outputFilesDetailsPanel);

			dataProcessorsDetailsPanel = new JPanel();
			dataProcessorsDetailsPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
			dataProcessorsDetailsPanel.setBorder(BorderFactory.createEmptyBorder(40, 10, 0, 0));
			detailsSide.add(dataProcessorsDetailsPanel);
		}

		@Override
		public void setVadereScenario(ScenarioRunManager scenario) {
			this.currentScenario = scenario;
			updateOutputFilesTable();
			updateDataProcessorsTable();
		}

		private void updateOutputFilesTable() {
			outputFilesTableModel.setRowCount(0);
			outputFilesDetailsPanel.removeAll();
			currentScenario.getDataProcessingJsonManager().getOutputFiles()
					.forEach(outputFile -> outputFilesTableModel.addRow(new OutputFile[] {outputFile}));
		}

		private void updateDataProcessorsTable() {
			dataProcessorsTableModel.setRowCount(0);
			dataProcessorsDetailsPanel.removeAll();
			currentScenario.getDataProcessingJsonManager().getDataProcessors()
					.forEach(dataProcessor -> dataProcessorsTableModel.addRow(new DataProcessor[] {dataProcessor}));
		}

		@Override
		public void isEditable(boolean isEditable) {
			this.isEditable = isEditable;
			editableComponents.forEach(comp -> comp.setEnabled(isEditable));
		}

		private void setupTables() {
			outputFilesTableModel = new DefaultTableModel(new OutputFile[] {null}, 0) {
				@Override
				public boolean isCellEditable(int row, int column) {
					return false;
				}
			};
			outputFilesTable = new JTable(outputFilesTableModel);
			outputFilesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			outputFilesTable.getSelectionModel().addListSelectionListener(e -> {
				if (!e.getValueIsAdjusting() && outputFilesTable.getSelectedRow() > -1) {
					handleOutputFileSelected((OutputFile) outputFilesTableModel.getValueAt(outputFilesTable.getSelectedRow(), 0));
				}
			});

			dataProcessorsTableModel = new DefaultTableModel(new DataProcessor[] {null}, 0) {
				@Override
				public boolean isCellEditable(int row, int column) {
					return false;
				}
			};
			dataProcessorsTable = new JTable(dataProcessorsTableModel);
			dataProcessorsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			dataProcessorsTable.getSelectionModel().addListSelectionListener(e -> {
				if (!e.getValueIsAdjusting() && dataProcessorsTable.getSelectedRow() > -1) {
					handleDataProcessorSelected((DataProcessor) dataProcessorsTable.getValueAt(dataProcessorsTable.getSelectedRow(), 0));
				}
			});
		}

		private JPanel buildPanel(String labelText, JTable table, JButton addBtn) { // used for OutputFile-Table and DataProcessor-Table
			JPanel panel = new JPanel();
			panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

			// label
			JLabel label = new JLabel("<html><b>" + labelText + "</b></html>");
			label.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
			JPanel labelPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
			labelPanel.add(label);
			panel.add(labelPanel);

			// table
			table.setTableHeader(null);
			JScrollPane tableScrollPane = new JScrollPane(table);
			panel.add(tableScrollPane);
			panel.add(Box.createRigidArea(new Dimension(0, 10)));

			// button
			JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
			btnPanel.add(addBtn);
			panel.add(btnPanel);

			editableComponents.add(addBtn);
			return panel;
		}

		private void handleOutputFileSelected(OutputFile outputFile) {
			Type outputFileDataKey = getDataKeyForOutputFile(outputFile);

			outputFilesDetailsPanel.removeAll();

			JPanel panel = new JPanel(new GridBagLayout());
			outputFilesDetailsPanel.add(panel);

			GridBagConstraints c = new GridBagConstraints();
			c.anchor = GridBagConstraints.WEST; // alignment
			c.ipady = 15; // y-gap between components
			c.fill = GridBagConstraints.HORIZONTAL;

			c.gridx = 0;
			c.gridy = 0;
			panel.add(new JLabel("<html><i>File name:</i></html>"), c);

			c.gridx = 1;
			c.gridy = 0;
			JTextField nameField = new JTextField(outputFile.getFileName());
			nameField.setEditable(false);
			nameField.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseReleased(MouseEvent e) {
					nameField.setEditable(true);
					nameField.getCaret().setVisible(true);
				}
			});
			nameField.addActionListener(ae -> {
				String oldName = outputFile.getFileName();
				String newName = nameField.getText();
				if (!oldName.equals(newName)) {
					String msg = "";
					if (newName.isEmpty()) {
						msg = "File name can't be empty";
					}
					if (outputFileNameAlreadyExists(newName)) {
						msg = "File name is already in use";
					}
					if (msg.isEmpty()) {
						outputFile.setFileName(newName);
						outputFilesTable.repaint();
					} else {
						nameField.setText(oldName);
						JOptionPane.showMessageDialog(ProjectView.getMainWindow(), msg,
								"Invalid file name", JOptionPane.WARNING_MESSAGE);
					}
				}
				nameField.setEditable(false);
				nameField.getCaret().setVisible(false);
			});

			panel.add(nameField, c);

			c.gridx = 0;
			c.gridy = 1;
			panel.add(new JLabel("<html><i>Data key:</i></html>"), c);

			c.gridx = 1;
			c.gridy = 1;
			String outputFileDataKeyName = extractSimpleName(outputFileDataKey);
			String[] dataKeys = {"NoDataKey", "PedestrianIdDataKey", "TimestepDataKey", "TimestepPedestrianIdDataKey"}; // TODO find a more direct way of collecting these and toString()ing them
			JComboBox dataKeysChooser = new JComboBox<>(dataKeys);
			dataKeysChooser.setSelectedItem(outputFileDataKeyName);
			dataKeysChooser.addActionListener(ae -> {
				String newDataKey = (String) dataKeysChooser.getSelectedItem();
				if (!newDataKey.equals(outputFileDataKeyName)) {
					OutputFileStore outputFileStore = new OutputFileStore();
					outputFileStore.setFilename(outputFile.getFileName());
					switch (newDataKey) {
						case "NoDataKey":
							outputFileStore.setType(NoDataKeyOutputFile.class.getName());
							break;
						case "PedestrianIdDataKey":
							outputFileStore.setType(PedestrianIdOutputFile.class.getName());
							break;
						case "TimestepDataKey":
							outputFileStore.setType(TimestepOutputFile.class.getName());
							break;
						case "TimestepPedestrianIdDataKey":
							outputFileStore.setType(TimestepPedestrianIdOutputFile.class.getName());
							break;
					}
					int index = currentScenario.getDataProcessingJsonManager().replaceOutputFile(outputFileStore);
					updateOutputFilesTable();
					outputFilesTable.setRowSelectionInterval(index, index);
				}
			});
			panel.add(dataKeysChooser, c);

			c.gridx = 0;
			c.gridy = 2;
			panel.add(new JLabel("<html><i>Header:</i></html>"), c);

			c.gridx = 1;
			c.gridy = 2;
			panel.add(new JLabel(outputFile.getHeader()), c);

			c.gridx = 0;
			c.gridy = 3;
			panel.add(new JLabel("<html><i>Processors:</i></html>"), c);

			c.gridx = 1;
			c.gridy = 3;
			c.gridwidth = 2;
			JComboCheckBox<Integer> comboBox =
					new JComboCheckBox<>(currentScenario.getDataProcessingJsonManager()
							.getDataProcessors().stream()
							.filter(dataProcessor -> getDataKeyForDataProcessor(dataProcessor) == outputFileDataKey) // only show processors with same DataKey as outputFile
							.map(DataProcessor::getId).collect(Collectors.toList()));
			comboBox.setCheckedItems(outputFile.getProcessorIds());
			comboBox.addActionListener(e -> {
				if (e.getActionCommand().equals("inputComplete")) {
					outputFile.setProcessorIds(comboBox.getCheckedItems());
					currentScenario.updateCurrentStateSerialized();
					ProjectView.getMainWindow().refreshScenarioNames();
				}
			});
			panel.add(comboBox, c);
			editableComponents.add(comboBox);
			comboBox.setEnabled(isEditable);

			revalidate();
			repaint(); // inelegantly, it needs both revalidate() and repaint() stackoverflow.com/a/5812780
		}

		private void handleDataProcessorSelected(DataProcessor dataProcessor) {
			dataProcessorsDetailsPanel.removeAll();

			JPanel panel = new JPanel(new GridBagLayout());
			dataProcessorsDetailsPanel.add(panel);

			GridBagConstraints c = new GridBagConstraints();
			c.anchor = GridBagConstraints.WEST;
			c.ipady = 15;

			c.gridx = 0;
			c.gridy = 0;
			c.gridwidth = 2;
			panel.add(new JLabel("<html><b>" + dataProcessor.getType() + "</b></html>"), c);
			c.gridwidth = 1;

			c.gridx = 0;
			c.gridy = 1;
			panel.add(new JLabel("<html><i>DataKey: </i></html>"), c);

			c.gridx = 1;
			c.gridy = 1;
			panel.add(new JLabel(extractSimpleName(getDataKeyForDataProcessor(dataProcessor))), c);

			// processor json
			c.gridx = 0;
			c.gridy = 2;
			c.gridwidth = 2;
			c.fill = GridBagConstraints.HORIZONTAL;
			RSyntaxTextArea attributesTextArea = new RSyntaxTextArea();
			attributesTextArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JSON);
			InputStream in = getClass().getResourceAsStream("/syntaxthemes/idea.xml");
			try {
				Theme syntaxTheme = Theme.load(in);
				syntaxTheme.apply(attributesTextArea);

				ObjectNode node = (ObjectNode) DataProcessingJsonManager.serializeProcessor(dataProcessor);
				JsonNode idNode = node.remove("id");
				JsonNode typeNode = node.remove("type");

				/*if (node.get("attributesType") != null) {
					JsonNode attributesTypeNode = node.get("attributesType");
					JsonNode attributesNode = node.get("attributes");
				}*/

				attributesTextArea.setText(JsonConverter.serializeJsonNode(node));

				/* TODO
				attributesTextArea.getDocument().addDocumentListener(new DocumentListener() {
					@Override
					public void changedUpdate(DocumentEvent e) {
						setScenarioContent();
					}

					@Override
					public void removeUpdate(DocumentEvent e) {
						setScenarioContent();
					}

					@Override
					public void insertUpdate(DocumentEvent e) {
						setScenarioContent();
					}

					public void setScenarioContent() {
						if (isEditable) {
							String json = attributesTextArea.getText();
							if (json.length() == 0)
								return;
							try {
								ObjectNode processorNode = (ObjectNode) JsonConverter.deserializeToNode(json);
								processorNode.set("id", idNode);
								processorNode.set("type", typeNode);
								DataProcessorStore dataProcessorStore = DataProcessingJsonManager.deserializeProcessorStore(processorNode);
								currentScenario.getDataProcessingJsonManager().updateDataProcessor(dataProcessor, dataProcessorStore);
								currentScenario.updateCurrentStateSerialized();
								ProjectView.getMainWindow().refreshScenarioNames();
								//jsonValidIndicator.setValid();
							} catch (Exception e) {
								e.printStackTrace();
								//jsonValidIndicator.setInvalid();
							}
						}
					}
				});*/
			} catch (IOException e) {
				e.printStackTrace();
			}
			attributesTextArea.setEditable(false);
			JScrollPane scrollPane = new JScrollPane(attributesTextArea);
			scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
			scrollPane.setPreferredSize(new Dimension(dataProcessorsDetailsPanel.getWidth() - 30, 100)); // hackish, but didn't find another way from avoiding the JScrollPane to break through the east border with full length
			panel.add(scrollPane, c);

			revalidate();
			repaint();
		}

		private Type getDataKeyForDataProcessor(Object object) {
			Class cls = object.getClass();
			while (cls.getSuperclass() != DataProcessor.class) { // climb up until we can get the DataKey from the highest class DataProcessor
				cls = cls.getSuperclass();
			}
			return ((ParameterizedType) cls.getGenericSuperclass()).getActualTypeArguments()[0];
		}

		private Type getDataKeyForOutputFile(Object object) {
			return ((ParameterizedType) object.getClass().getGenericSuperclass()).getActualTypeArguments()[0];
		}

		private String extractSimpleName(Type type) {
			return type.getTypeName().substring(type.getTypeName().lastIndexOf(".") + 1);
		}

		private boolean outputFileNameAlreadyExists(String filename) {
			return currentScenario.getDataProcessingJsonManager().getOutputFiles().stream()
					.filter(oFile -> oFile.getFileName().equals(filename)).findAny().isPresent();
		}
	}
}
