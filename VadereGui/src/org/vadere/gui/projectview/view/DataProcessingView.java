package org.vadere.gui.projectview.view;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import info.clearthought.layout.TableLayout;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rsyntaxtextarea.Theme;
import org.vadere.gui.components.utils.Messages;
import org.vadere.gui.components.view.JComboCheckBox;
import org.vadere.gui.projectview.model.IScenarioChecker;
import org.vadere.gui.projectview.utils.SimpleDocumentListener;
import org.vadere.gui.topographycreator.view.JLinkLabel;
import org.vadere.simulator.projects.Scenario;
import org.vadere.simulator.projects.dataprocessing.DataProcessingJsonManager;
import org.vadere.simulator.projects.dataprocessing.outputfile.OutputFile;
import org.vadere.simulator.projects.dataprocessing.outputfile.OutputFileFactory;
import org.vadere.simulator.projects.dataprocessing.processor.DataProcessor;
import org.vadere.simulator.projects.dataprocessing.processor.DataProcessorFactory;
import org.vadere.simulator.projects.dataprocessing.store.DataProcessorStore;
import org.vadere.simulator.projects.dataprocessing.store.OutputFileStore;
import org.vadere.state.util.StateJsonConverter;
import org.vadere.util.config.VadereConfig;
import org.vadere.util.io.IOUtils;
import org.vadere.util.logging.Logger;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.text.MessageFormat;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;


class DataProcessingView extends JPanel implements IJsonView {

	private static Logger logger = Logger.getLogger(DataProcessingView.class);

	private IJsonView activeJsonView; // gui-mode or expert-mode
	private JLabel switchJsonViewModeLabel = new JLabel();
	private JPanel viewPanel; // hosts the gui-panel or the expert-panel
	private static final String guiViewMode = Messages.getString("ProjectView.gui");
	private static final String jsonViewMode = Messages.getString("ProjectView.json");
	private boolean inGuiViewMode = true;

	private Scenario currentScenario;
	private boolean isEditable;
	private IScenarioChecker scenarioChecker;

	DataProcessingView(IScenarioChecker scenarioChecker) {
		setLayout(new BorderLayout()); // force it to span across the whole available space

		viewPanel = new JPanel(new GridLayout(1, 1));
		add(viewPanel, BorderLayout.CENTER);

		String dataProcessingViewModeStr = VadereConfig.getConfig().getString("Gui.dataProcessingViewMode", "");
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
		setScenarioChecker(scenarioChecker);
		switchMode();
	}

	public void setScenarioChecker(IScenarioChecker scenarioChecker) {
		this.scenarioChecker = scenarioChecker;
	}

	private void switchMode() {
		String link = MessageFormat.format(Messages.getString("ProjectView.JSONSwitch.link"), (inGuiViewMode ? jsonViewMode : guiViewMode));
		switchJsonViewModeLabel.setText("<html><span style='font-size:8px'><font color='blue'>" +
				link+"</font></span></html>");
		VadereConfig.getConfig().setProperty("Gui.dataProcessingViewMode",
				inGuiViewMode ? guiViewMode : jsonViewMode);
		viewPanel.removeAll();

		if (inGuiViewMode) {
            logger.info("switch to gui view");
			GuiView guiView = new GuiView();
			guiView.setScenarioChecker(scenarioChecker);
			activeJsonView = guiView;
			viewPanel.add(guiView);
		} else {
            logger.info("switch to expert view");
			TextView expertView = buildExpertView();
			expertView.setScenarioChecker(scenarioChecker);
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

	private JMenu processorsMenu = new JMenu();

	private TextView buildExpertView() {
		TextView panel = new TextView("ProjectView.defaultDirectoryOutputProcessors", AttributeType.OUTPUTPROCESSOR);
		JMenuBar processorsMenuBar = new JMenuBar();
		processorsMenu = new JMenu(Messages.getString("Tab.Model.loadTemplateMenu.title"));
		processorsMenu.setEnabled(isEditable);
		processorsMenuBar.add(processorsMenu);

		try {
		    //TODO [bug, jar]: the file name is hard coded and it is not possible to add more presets! This is because listFile() does not work inside a jar.
            String fileName = "output_default";
            String templateJson = org.apache.commons.io.IOUtils.toString(this.getClass().getResourceAsStream("/outputTemplates/"+fileName+".json"), "UTF-8");
            processorsMenu.add(new JMenuItem(new AbstractAction(fileName) {
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
			panel.getPanelTop().add(processorsMenuBar, 0);
		} catch (IOException e) {
			e.printStackTrace();
            logger.error("could not initialize output processor expert view: " + e.getMessage());
		}
		return panel;
	}

	@Override
	public void setVadereScenario(Scenario scenario) {
		this.currentScenario = scenario;
		activeJsonView.setVadereScenario(scenario);
	}

	@Override
	public void isEditable(boolean isEditable) {
		this.isEditable = isEditable;
		activeJsonView.isEditable(isEditable);
		processorsMenu.setEnabled(isEditable); // this is a bit of a hack, it would be nicer to place all menus inside TextView and control enablement there
	}


	private class GuiView extends JPanel implements IJsonView {

		private Scenario currentScenario;
		private boolean isEditable;

		private JCheckBox isTimestampedCheckBox;
		private JCheckBox isWriteMetaData;
		private JTable outputFilesTable;
		private DefaultTableModel outputFilesTableModel;
		private JTable dataProcessorsTable;
		private DefaultTableModel dataProcessorsTableModel;
		private JPanel outputFilesDetailsPanel;
		private JPanel dataProcessorsDetailsPanel;
		private List<Component> editableComponents = new ArrayList<>();
		private JButton deleteFileBtn, deleteProcessorBtn;
		private OutputFile selectedOutputFile;
		private DataProcessor selectedDataProcessor;
		private String latestJsonParsingError;
		private Set<Integer> dataProcessIdsInUse = new HashSet<>();
		private IScenarioChecker scenarioChecker;

		GuiView() {
			/* via www.oracle.com/technetwork/java/tablelayout-141489.html,
			I don't like adding a maven dependency for a swing layout,
			but all the native ones can't seem to ensure relative column width */
			double size[][] = {{0.35, 0.65}, {TableLayout.FILL}};
			setLayout(new TableLayout(size));

			// left tables side

			JPanel tableSide = new JPanel(new GridLayout(2, 1)); // one column, two equally sized rows
			add(tableSide, "0, 0");

			JPanel filesPanel = new JPanel();
			filesPanel.setLayout(new BoxLayout(filesPanel, BoxLayout.PAGE_AXIS));
			isTimestampedCheckBox = new JCheckBox(Messages.getString("DataProcessingView.chbAddTimeStamp"));
			isTimestampedCheckBox.addActionListener(new AbstractAction() {
				@Override
				public void actionPerformed(ActionEvent e) {
					currentScenario.getDataProcessingJsonManager().setTimestamped(isTimestampedCheckBox.isSelected());
				}
			});

			isTimestampedCheckBox.setAlignmentX(Component.LEFT_ALIGNMENT);
			addEditableComponent(isTimestampedCheckBox);
			filesPanel.add(isTimestampedCheckBox);

			isWriteMetaData = new JCheckBox(Messages.getString("DataProcessingView.chbAddMetaData"));
			isWriteMetaData.addActionListener(new AbstractAction() {
				@Override
				public void actionPerformed(ActionEvent e) {
					currentScenario.getDataProcessingJsonManager().setWriteMetaData(isWriteMetaData.isSelected());
				}
			});

			isWriteMetaData.setAlignmentX(Component.LEFT_ALIGNMENT);
			addEditableComponent(isWriteMetaData);
			filesPanel.add(isWriteMetaData);

			JButton addFileBtn = new JButton(new AbstractAction(Messages.getString("DataProcessingView.btnAdd")) {
				@Override
				public void actionPerformed(ActionEvent e) {

					String filename = getDefaultFilename();
					OutputFileStore outputFileStore = new OutputFileStore();
					outputFileStore.setFilename(filename);
					currentScenario.getDataProcessingJsonManager().addOutputFile(outputFileStore);
					updateOutputFilesTable();
					int index = outputFilesTableModel.getRowCount() - 1;
					outputFilesTable.setRowSelectionInterval(index, index);
					refreshGUI();
				}
			});
			deleteFileBtn = new JButton(new AbstractAction(Messages.getString("DataProcessingView.btnDelete")) {
				@Override
				public void actionPerformed(ActionEvent e) {
					if (selectedOutputFile == null) {
						JOptionPane.showMessageDialog(ProjectView.getMainWindow(),
								Messages.getString("DataProcessingView.msgFileSelected"));
					} else {
						currentScenario.getDataProcessingJsonManager().getOutputFiles().remove(selectedOutputFile);
						selectedOutputFile = null;
						outputFilesDetailsPanel.removeAll();
						updateOutputFilesTable();
						revalidate();
						repaint();
						refreshGUI();
					}
				}
			});

			setupTables();

			JPanel filesTable = buildPanel(Messages.getString("DataProcessingView.files.label"), outputFilesTable, addFileBtn, deleteFileBtn);
			filesTable.setAlignmentX(Component.LEFT_ALIGNMENT);
			filesPanel.add(filesTable);
			tableSide.add(filesPanel);

			JButton addProcessorBtn = new JButton(new AbstractAction(Messages.getString("DataProcessingView.btnAdd")) {
				@Override
				public void actionPerformed(ActionEvent e) {
					DataProcessorFactory factory = DataProcessorFactory.instance();
					// map label of processors to class string
					Map<String, String> processorLableToClass = factory.getLabelMap();
					String[] processors = processorLableToClass.keySet().stream().sorted().toArray(String[]::new);
					JComboBox processorOptions = new JComboBox<>(processors);

					if (JOptionPane.showConfirmDialog(ProjectView.getMainWindow(), processorOptions,
							Messages.getString("DataProcessingView.dialogChoseProcessor.label"), JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
						String processorClass = processorLableToClass.get(processorOptions.getSelectedItem());
//						System.out.println("Selected Processor is: " + processorClass);
						DataProcessor newDataProcessor = null;
						try {
							newDataProcessor = factory.getInstanceOf(processorClass);
						} catch (ClassNotFoundException e1) {
							e1.printStackTrace();
						}
						assert newDataProcessor != null;
						newDataProcessor.setId(currentScenario.getDataProcessingJsonManager().getMaxProcessorsId() + 1);
						currentScenario.getDataProcessingJsonManager().addInstantiatedProcessor(newDataProcessor);
						updateDataProcessorsTable();
						int index = dataProcessorsTable.getRowCount() - 1;
						dataProcessorsTable.setRowSelectionInterval(index, index);
					}
					refreshGUI();
				}
			});
			deleteProcessorBtn = new JButton(new AbstractAction(Messages.getString("DataProcessingView.btnDelete")) {
				@Override
				public void actionPerformed(ActionEvent e) {
					if (selectedDataProcessor == null) {
						JOptionPane.showMessageDialog(ProjectView.getMainWindow(), Messages.getString("DataProcessingView.msgFileSelected"));
					} else {
						Integer id = selectedDataProcessor.getId();
						currentScenario.getDataProcessingJsonManager().getDataProcessors().remove(selectedDataProcessor);
						selectedDataProcessor = null;
						dataProcessorsDetailsPanel.removeAll();
						currentScenario.getDataProcessingJsonManager().getOutputFiles().forEach(outputFile -> {
							if (outputFile.getProcessorIds().remove(id) && outputFile == selectedOutputFile) {
								handleOutputFileSelected(selectedOutputFile);
							}
						});
						updateDataProcessorsTable();
						refreshGUI();
						revalidate();
						repaint();
					}
				}
			});
			tableSide.add(buildPanel(Messages.getString("DataProcessingView.dialogProcessors.label"), dataProcessorsTable, addProcessorBtn, deleteProcessorBtn));

			// right details side

			JPanel detailsSide = new JPanel(new GridLayout(2, 1)); // one column, two equally sized rows
			add(detailsSide, "1, 0");

			outputFilesDetailsPanel = new JPanel();
			outputFilesDetailsPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
			outputFilesDetailsPanel.setBorder(BorderFactory.createEmptyBorder(40, 10, 0, 0));
			detailsSide.add(outputFilesDetailsPanel);

			dataProcessorsDetailsPanel = new JPanel();
			dataProcessorsDetailsPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
			dataProcessorsDetailsPanel.setBorder(BorderFactory.createEmptyBorder(40, 10, 0, 0));
			detailsSide.add(dataProcessorsDetailsPanel);
		}

		private void refreshGUI() {
			currentScenario.updateCurrentStateSerialized();
			scenarioChecker.checkScenario(currentScenario);
			ProjectView.getMainWindow().refreshScenarioNames();
		}

		public void setScenarioChecker(IScenarioChecker scenarioChecker) {
			this.scenarioChecker = scenarioChecker;
		}

		private String getDefaultFilename(){
			String filename = "out.txt";
			int count = 1;
			while (outputFileNameAlreadyExists(filename) > 0) { // ensure unique suggested filename
				filename = "out" + (count ++) + ".txt";
			}
			return filename;
		}

		@Override
		public void setVadereScenario(Scenario scenario) {
			this.currentScenario = scenario;
			latestJsonParsingError = null;
			selectedOutputFile = null;
			selectedDataProcessor = null;
			isTimestampedCheckBox.setSelected(scenario.getDataProcessingJsonManager().isTimestamped());
			isWriteMetaData.setSelected(scenario.getDataProcessingJsonManager().isWriteMetaData());
			updateOutputFilesTable();
			updateDataProcessorsTable();
			updateDataProcessIdsInUse();
		}

		private void updateOutputFilesTable() {
			outputFilesTableModel.setRowCount(0);
			outputFilesDetailsPanel.removeAll();
			currentScenario.getDataProcessingJsonManager().getOutputFiles()
					.forEach(outputFile -> outputFilesTableModel.addRow(new OutputFile[] {outputFile}));
		}

		private void updateDataProcessIdsInUse() {
			dataProcessIdsInUse.clear();
			currentScenario.getDataProcessingJsonManager().getOutputFiles().forEach(oFile -> dataProcessIdsInUse.addAll(oFile.getProcessorIds()));
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

		private void addEditableComponent(Component comp) {
			comp.setEnabled(isEditable);
			editableComponents.add(comp);
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
			outputFilesTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
				@Override
				public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
					Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
					if (((OutputFile) value).getProcessorIds().isEmpty()) {
						c.setForeground(Color.gray);
					} else {
						c.setForeground(Color.black); // has at least one DataProcessor selected
					}
					return c;
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
			dataProcessorsTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
				@Override
				public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
					Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
					if (dataProcessIdsInUse.contains(((DataProcessor) value).getId())) {
						c.setForeground(Color.black); // at least one OutputFile is using this DataProcessor
					} else {
						c.setForeground(Color.gray);
					}
					return c;
				}
			});
		}

		private JPanel buildPanel(String labelText, JTable table, JButton addBtn, JButton deleteBtn) { // used for OutputFile-Table and DataProcessor-Table
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
			btnPanel.add(deleteBtn);
			panel.add(btnPanel);

			addEditableComponent(addBtn);
			addEditableComponent(deleteBtn);
			return panel;
		}

		private void passFocusOn() {
			KeyboardFocusManager.getCurrentKeyboardFocusManager().focusNextComponent(); // get rid of the focus
		}

		private void handleOutputFileSelected(OutputFile outputFile) {
			selectedOutputFile = outputFile;
			Type outputFileDataKey = getDataKeyForOutputFile(outputFile);

			outputFilesDetailsPanel.removeAll();

			JPanel panel = new JPanel(new GridBagLayout());
			outputFilesDetailsPanel.add(panel);

			GridBagConstraints c = new GridBagConstraints();
			c.anchor = GridBagConstraints.WEST; // alignment
			c.insets = new Insets(6, 6, 6, 6);
			c.fill = GridBagConstraints.HORIZONTAL;

			c.gridx = 0;
			c.gridy = 0;
			panel.add(new JLabel(Messages.getString("DataProcessingView.dialogOutputFileSelection.label")+":"), c);

			c.gridx = 1;
			c.gridy = 0;
			JTextField nameField = new JTextField(outputFile.toString());
			// add DocumentLister to update outputFiles character at a time
			nameField.getDocument().addDocumentListener(new SimpleDocumentListener() {
				@Override
				public void update(DocumentEvent e){
					SwingUtilities.invokeLater(() -> {
						String oldName = outputFile.toString();
						String newName = nameField.getText();
						if (!oldName.equals(newName)) {
							outputFile.setRelativeFileName(newName);
							outputFilesTable.repaint();
							refreshGUI();
						}
					});
				}
			});

			// check at focus loss for invalid file names. If found reset to a default free value.
			nameField.addFocusListener(new FocusAdapter() {
				@Override
				public void focusLost(FocusEvent e) {
					SwingUtilities.invokeLater(() -> {
						String newName = nameField.getText();

						String msg = "";
						if (newName.isEmpty()) {
							msg = Messages.getString("DataProcessingView.msgFileEmpty");
							newName = getDefaultFilename();
						} else if (outputFileNameAlreadyExists(newName) > 1) { // is automatically updated.
							msg = Messages.getString("DataProcessingView.msgFileInUse");
							newName = getDefaultFilename();
						}
						if (!msg.isEmpty()) {
							nameField.setText(newName);
							JOptionPane.showMessageDialog(ProjectView.getMainWindow(), msg,
									Messages.getString("DataProcessingView.dialogInvalidFile.label"), JOptionPane.WARNING_MESSAGE);
						}
					});
				}
			});

			// got to next element. Note: this will trigger the focus loss event above.
			nameField.addActionListener(ae -> {
				passFocusOn();
			});


			addEditableComponent(nameField);
			panel.add(nameField, c);

			c.gridx = 0;
			c.gridy = 1;
			panel.add(new JLabel(Messages.getString("DataProcessingView.dialogOutputDataKeySelection.label")), c);

			c.gridx = 1;
			c.gridy = 1;
			String outputFileDataKeyName = extractSimpleName(outputFileDataKey);

			Map<String, String> dataKeysOutputFiles = OutputFileFactory.instance().getDataKeyOutputFileMap();
//			Map<String, Class> dataKeysOutputFiles = ClassFinder.getDataKeysOutputFileRelation();
			JComboBox dataKeysChooser = new JComboBox<>(dataKeysOutputFiles.keySet().toArray());

			dataKeysChooser.setSelectedItem(outputFileDataKeyName);
			dataKeysChooser.addActionListener(ae -> {
				String newDataKey = (String) dataKeysChooser.getSelectedItem();
				assert newDataKey != null;
				if (!newDataKey.equals(outputFileDataKeyName)) {
					OutputFileStore outputFileStore = new OutputFileStore();
					outputFileStore.setFilename(outputFile.getFileName());
					System.out.println("Selected KeyFile: " + newDataKey + " | " + dataKeysOutputFiles.get(newDataKey));
					outputFileStore.setType(dataKeysOutputFiles.get(newDataKey)); // Choose corresponding outputfile type
					int index = currentScenario.getDataProcessingJsonManager().replaceOutputFile(outputFileStore);
					updateOutputFilesTable();
					outputFilesTable.setRowSelectionInterval(index, index);
					refreshGUI();
				}
			});
			addEditableComponent(dataKeysChooser);
			panel.add(dataKeysChooser, c);

			c.gridx = 0;
			c.gridy = 2;
			panel.add(new JLabel(Messages.getString("DataProcessingView.dialogOutputIndicesSelection.label")+":"), c);

			// Only the indices are shown, not the entire header. > Entire header can be quite a lot of text (which may
			// not fit in the GUI) + the processors (with the header information) are not inserted and removed on the fly,
			// but only when the simulation is started.
			c.gridx = 1;
			c.gridy = 2;
			panel.add(new JLabel(outputFile.getIndicesLine()), c);

			c.gridx = 0;
			c.gridy = 3;
			panel.add(new JLabel(Messages.getString("DataProcessingView.dialogProcessors.label")+":"), c);

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
					updateDataProcessIdsInUse();
					refreshGUI();
					passFocusOn();
				}
			});
			panel.add(comboBox, c);
			addEditableComponent(comboBox);

			revalidate();
			repaint(); // inelegantly, it needs both revalidate() and repaint() stackoverflow.com/a/5812780
		}


		private void handleDataProcessorSelected(DataProcessor dataProcessor) {
			selectedDataProcessor = dataProcessor;
			dataProcessorsDetailsPanel.removeAll();

			JPanel panel = new JPanel(new GridBagLayout());
			dataProcessorsDetailsPanel.add(panel);

			GridBagConstraints c = new GridBagConstraints();
			c.anchor = GridBagConstraints.WEST;
			c.insets = new Insets(6, 6, 6, 6);

			c.gridx = 0;
			c.gridy = 0;
			c.gridwidth = 3;
			panel.add(new JLinkLabel(dataProcessor.getClass().getName(), "<html><b>", "</b></html>"), c);
			c.gridwidth = 1;

			c.gridx = 0;
			c.gridy = 1;
			panel.add(new JLabel(Messages.getString("DataProcessingView.dialogOutputDataKeySelection.label")+":"), c);

			c.gridx = 1;
			c.gridy = 1;
			panel.add(new JLinkLabel(getDataKeyForDataProcessor(dataProcessor).getTypeName()), c);

			c.gridx = 2;
			c.gridy = 1;
			c.anchor = GridBagConstraints.EAST;
			JLabel jsonInvalidLabel = new JLabel("<html><font color='red'>"+Messages.getString("DataProcessingView.msgInvalidJson")+"</font> <font color=gray size=-2><a href=#>" +
					Messages.getString("DataProcessingView.msgShowError")+ "</a></font></html>");
			jsonInvalidLabel.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseReleased(MouseEvent e) {
					VDialogManager.showMessageDialogWithTextArea(
							Messages.getString("TextView.lbljsoninvalid.errorMsgPopup.title"),
							latestJsonParsingError,
							JOptionPane.ERROR_MESSAGE);
				}
			});

			jsonInvalidLabel.setVisible(false);
			panel.add(jsonInvalidLabel, c);
			c.anchor = GridBagConstraints.WEST;

			// processor json
			c.gridx = 0;
			c.gridy = 2;
			c.gridwidth = 3;
			c.fill = GridBagConstraints.HORIZONTAL;

			RSyntaxTextArea attributesTextArea = new RSyntaxTextArea();

			JScrollPane jsonScrollPane = new JScrollPane(attributesTextArea);
			jsonScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
			jsonScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
			jsonScrollPane.setPreferredSize(new Dimension(dataProcessorsDetailsPanel.getWidth() - 30, 145)); // hackish, but didn't find another way from avoiding the JScrollPane to break through the east border with full length
			panel.add(jsonScrollPane, c);

			attributesTextArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JSON);
			InputStream in = getClass().getResourceAsStream("/syntaxthemes/idea.xml");
			try {
				Theme syntaxTheme = Theme.load(in);
				syntaxTheme.apply(attributesTextArea);

				ObjectNode node = (ObjectNode) DataProcessingJsonManager.serializeProcessor(dataProcessor);
				JsonNode idNode = node.remove("id");
				JsonNode typeNode = node.remove("type");
				attributesTextArea.setText(StateJsonConverter.serializeJsonNode(node));

				attributesTextArea.getDocument().addDocumentListener(new DocumentListener() {
					@Override public void changedUpdate(DocumentEvent e) { setScenarioContent(); }
					@Override public void removeUpdate(DocumentEvent e) { setScenarioContent(); }
					@Override  public void insertUpdate(DocumentEvent e) { setScenarioContent(); }
					void setScenarioContent() {
						if (isEditable) {
							String json = attributesTextArea.getText();
							if (json.length() == 0)
								return;
							try {
								ObjectNode processorNode = (ObjectNode) StateJsonConverter.deserializeToNode(json);
								processorNode.set("id", idNode);
								processorNode.set("type", typeNode);
								DataProcessorStore dataProcessorStore = DataProcessingJsonManager.deserializeProcessorStore(processorNode);
								dataProcessor.setAttributes(dataProcessorStore.getAttributes());
								jsonInvalidLabel.setVisible(false);
								jsonScrollPane.setBorder(null);
								refreshGUI();
							} catch (Exception e) {
								latestJsonParsingError = e.getMessage();
								jsonInvalidLabel.setVisible(true);
								jsonScrollPane.setBorder(BorderFactory.createLineBorder(Color.red, 2));
							}
						}
					}
				});
			} catch (IOException e) {
				e.printStackTrace();
			}
			attributesTextArea.setEditable(isEditable);
			attributesTextArea.setBackground(isEditable ? Color.WHITE : Color.LIGHT_GRAY);

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

		private long outputFileNameAlreadyExists(String filename) {
			return currentScenario.getDataProcessingJsonManager().getOutputFiles().stream()
					.filter(oFile -> oFile.getFileName().equals(filename)).count();
		}
	}
}
