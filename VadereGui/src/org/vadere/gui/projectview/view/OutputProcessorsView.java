package org.vadere.gui.projectview.view;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.vadere.gui.components.utils.Messages;
import org.vadere.gui.projectview.control.ActionCreateProcessor;
import org.vadere.simulator.projects.ScenarioRunManager;
import org.vadere.simulator.projects.dataprocessing.processors.*;
import org.vadere.simulator.projects.dataprocessing.writer.ProcessorWriter;
import org.vadere.simulator.projects.io.JsonSerializerProcessor;
import org.vadere.util.io.IOUtils;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * 
 * Shows and lets the user edit the output processors of a scenario.
 * 
 * 
 */
public class OutputProcessorsView extends JPanel {
	private Logger logger = LogManager.getLogger(getClass());

	private Frame thisOwner;

	private class OutputProcessorTableModel extends DefaultTableModel {

		public OutputProcessorTableModel() {
			super(new String[] {"Processor"}, 0);
		}

		private static final long serialVersionUID = 1145206652000839654L;

		@Override
		public boolean isCellEditable(int row, int col) {
			return false;
		}

		public void setValues(BlockingQueue<ProcessorWriter> list) {
			// remove all rows
			int rows = this.getRowCount();
			for (int i = 0; i < rows; i++) {
				this.removeRow(0);
			}

			// set rows to new values
			for (ProcessorWriter proc : list) {
				this.addRow(new ProcessorWriter[] {proc});
			}

		}

	}

	private BlockingQueue<ProcessorWriter> writers;

	/**
	 * Adds a given processor to the table
	 * 
	 * @param writer
	 */
	public void addProcessorWriter(final ProcessorWriter writer) {
		writers.add(writer);
		updateProcessorTable();
		selectOutputProcessor(table.getModel().getRowCount() - 1);
	}

	public Frame getOwner() {
		return thisOwner;
	}

	public ProcessorWriter getSelectedProcessorWriter() {
		return selectedWriter;
	}

	/**
	 * Removes a given processor from the table if it exists.
	 * 
	 * @param writer
	 */
	private void removeProcessorWriter(final ProcessorWriter writer) {
		if (writers.contains(writer)) {
			writers.remove(writer);
			updateProcessorTable();
		}
	}

	/**
	 * Sets the processors in the table.
	 * 
	 * @param processors
	 */
	/*
	 * public void setProcessors(final Collection<ProcessWriter> writers) {
	 * writers.clear();
	 * for (ProcessWriter writer : writers) {
	 * addProcessorWriter(writer);
	 * }
	 * }
	 */

	/**
	 * generated serial version uid
	 */
	private static final long serialVersionUID = 6570759037291526514L;

	/**
	 * Called when the user clicks on the "delete" button in the processor table
	 * menu. Deletes the currently selected processor.
	 */
	private ActionListener listMenuDeleteButtonActionListener = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
			int rowIndex = table.getSelectedRow();
			ProcessorWriter writer = findProcessorWriter(rowIndex);
			if (writer != null) {
				removeProcessorWriter(writer);
				selectedWriter = null;
			}
		}
	};

	private ProcessorWriter findProcessorWriter(final int rowIndex) {
		if (rowIndex < 0) {
			return null;
		}

		ProcessorWriter writer = (ProcessorWriter) table.getModel().getValueAt(rowIndex, 0);

		return writer;
	}

	/**
	 * Called when the user clicks on the "new" button in the processor table
	 * menu. Creates a standard processor.
	 */
	private Action createProcessorAction = new ActionCreateProcessor(this);

	private JTable table;

	private MouseListener listMouseListener = new MouseAdapter() {
		@Override
		public void mouseClicked(MouseEvent e) {
			Point p = e.getPoint();
			int row = table.rowAtPoint(p);
			if (row >= 0) {
				selectOutputProcessor(row);
				table.setRowSelectionInterval(row, row);
			}
		}
	};

	/**
	 * Sets the comboboxes to the values of the processor at the given row index
	 * in the table.
	 * 
	 * @param rowIndex
	 */
	private void selectOutputProcessor(final int rowIndex) {
		ProcessorWriter proc = findProcessorWriter(rowIndex);
		if (proc != null) {
			selectedWriter = proc;

			if (selectedWriter.getProcessor() instanceof CombineProcessor) {
				extendedPopupMenu.remove(mntAddProcessor);
				extendedPopupMenu.add(mntAddProcessor);
			} else {
				extendedPopupMenu.remove(mntAddProcessor);
			}

			thisFrame.lblLabelprocessortype.setText(proc.getProcessor().getName());
			thisFrame.txtpnProcessordescriptiontext.setText("missing description");
			String processorText = "";
			String columnsText = "";
			processorText =
					IOUtils.toPrettyPrintJson(JsonSerializerProcessor.toJsonElement(selectedWriter.getProcessor()));
			thisFrame.processorWriterView.getTextArea().setText(processorText);
			thisFrame.processorWriterView.getTextArea().setCaretPosition(0);
			String[] columnNames = proc.getColumnNames();

			// no specification so we use all columns
			if (columnNames == null) {
				columnNames = proc.getProcessor().getAllColumnNames();
				columnsText = IOUtils.toJson(IOUtils.getGson().toJsonTree(columnNames));
			} else {
				columnsText = IOUtils.toJson(IOUtils.getGson().toJsonTree(columnNames));
			}

			thisFrame.columnNamesView.getTextArea().setText(columnsText);

			if (columnNames == null) {
				setColumnNamesButton.setVisible(false);
			} else {
				setColumnNamesButton.setVisible(true);
			}
		}
	}

	private OutputProcessorsView thisFrame = null;
	private ProcessorWriter selectedWriter = null;
	private JLabel lblLabelprocessortype;
	private JTextPane txtpnProcessordescriptiontext;
	private ScenarioRunManager currentScenario;
	private JSONView processorWriterView;
	private JSONView columnNamesView;
	private JButton setColumnNamesButton;
	private JPopupMenu extendedPopupMenu;
	private JMenuItem mntAddProcessor;

	private void updateProcessorTable() {
		((OutputProcessorTableModel) table.getModel()).setValues(writers);

		currentScenario.removeAllWriters();
		for (ProcessorWriter writer : writers) {
			currentScenario.addWriter(writer);
		}
	}

	public void setScenario(final ScenarioRunManager scenario) {
		this.currentScenario = scenario;
		this.selectedWriter = null;
		this.writers.clear();
		columnNamesView.getTextArea().setText(Messages.getString("OutputprocessorsView.columnNames.text"));
		processorWriterView.getTextArea().setText(Messages.getString("OutputprocessorsView.processor.text"));

		for (ProcessorWriter writer : currentScenario.getAllWriters()) {
			try {
				this.writers.put(writer);
			} catch (InterruptedException e) {
				logger.error(e);
			}
		}
		updateProcessorTable();
	}

	/**
	 * Create the panel.
	 * 
	 * @throws IOException
	 */
	public OutputProcessorsView(Frame owner) throws IOException {
		thisOwner = owner;
		thisFrame = this;
		currentScenario = new ScenarioRunManager("");
		writers = new LinkedBlockingQueue<>();

		JPanel leftPanel = new JPanel();
		JPanel rightPanel = new JPanel();
		CellConstraints cc = new CellConstraints();

		FormLayout spiltLayout = new FormLayout("2dlu, 150dlu, 2dlu, default:grow, 2dlu", // col
				"2dlu, fill:default:grow, 2dlu"); // rows

		setLayout(spiltLayout);
		add(leftPanel, cc.xy(2, 2));
		add(rightPanel, cc.xy(4, 2));

		FormLayout rightLayout = new FormLayout("2dlu, default, 2dlu, fill:default:grow, 2dlu, default, 2dlu", // col
				"2dlu, default, 2dlu, default, 2dlu, default, 2dlu, default, 2dlu"); // rows
		rightPanel.setLayout(rightLayout);

		FormLayout leftLayout = new FormLayout("2dlu, default:grow, 2dlu", // col
				"2dlu, fill:default:grow, 2dlu"); // rows
		leftPanel.setLayout(leftLayout);

		table = new JTable();
		table.setModel(new OutputProcessorTableModel());
		table.addMouseListener(listMouseListener);
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		leftPanel.add(table, cc.xy(2, 2));


		extendedPopupMenu = new JPopupMenu();
		addPopup(table, extendedPopupMenu);

		JLabel lblProcessorType_1 = new JLabel("Processor Type:");
		rightPanel.add(lblProcessorType_1, cc.xy(2, 2));
		lblLabelprocessortype = new JLabel("labelProcessorType");
		rightPanel.add(lblLabelprocessortype, cc.xy(4, 2));

		JLabel lblDescription = new JLabel("Description:");
		rightPanel.add(lblDescription, cc.xyw(2, 4, 5));

		txtpnProcessordescriptiontext = new JTextPane();
		txtpnProcessordescriptiontext.setEditable(false);
		txtpnProcessordescriptiontext.setText("processorDescriptionText");
		rightPanel.add(txtpnProcessordescriptiontext, cc.xyw(4, 4, 3));

		JLabel lblColumnNames = new JLabel("Columns:");
		rightPanel.add(lblColumnNames, cc.xy(2, 6));

		columnNamesView = new JSONView(false);
		columnNamesView.getTextArea().setText(Messages.getString("OutputprocessorsView.columnNames.text"));
		columnNamesView.setEditable(false);
		rightPanel.add(columnNamesView, cc.xy(4, 6));
		columnNamesView.addDocumentListener(new DocumentListener() {

			@Override
			public void removeUpdate(DocumentEvent e) {
				update(e);
			}

			@Override
			public void insertUpdate(DocumentEvent e) {
				update(e);
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
				update(e);
			}

			private void update(DocumentEvent event) {
				String json = "";
				try {
					json = event.getDocument().getText(0, event.getDocument().getLength());
					String[] colNames = IOUtils.getGson().fromJson(json, new String[] {}.getClass());
					if (colNames != null && selectedWriter != null) {
						selectedWriter.setColumns(colNames);
					}
					ProjectView.getMainWindow().refreshScenarioNames();
					columnNamesView.setJsonValid(true);
				} catch (Exception e) {
					// logger.warn(e.getMessage() + ", invalid json");
					columnNamesView.setJsonValid(false);
				}
			}
		});

		setColumnNamesButton = new JButton("edit");
		rightPanel.add(setColumnNamesButton, cc.xy(6, 6, CellConstraints.RIGHT, CellConstraints.BOTTOM));

		setColumnNamesButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				ChooseNamesDialog dialog = new ChooseNamesDialog(thisOwner, "Select Columns",
						selectedWriter.getProcessor().getAllColumnNames());

				if (!dialog.isAborted()) {
					String columnsText = "";
					columnsText = IOUtils.toJson(IOUtils.getGson().toJsonTree(dialog.getSelectedNames()));
					thisFrame.columnNamesView.getTextArea().setText(columnsText);
					selectedWriter.setColumns(dialog.getSelectedNames());
					updateProcessorTable();
				}
			}
		});

		processorWriterView = new JSONView(true);
		processorWriterView.getTextArea().setText(Messages.getString("OutputprocessorsView.processor.text"));
		rightPanel.add(processorWriterView, cc.xyw(2, 8, 5));
		processorWriterView.addDocumentListener(new DocumentListener() {

			@Override
			public void removeUpdate(DocumentEvent e) {
				update(e);
			}

			@Override
			public void insertUpdate(DocumentEvent e) {
				update(e);
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
				update(e);
			}

			private void update(DocumentEvent e) {
				String json = "";

				try {
					json = processorWriterView.getTextArea().getText();
					Processor proc = JsonSerializerProcessor.toProcessorFromJson(json);
					if (proc != null && selectedWriter != null) {
						selectedWriter.setProcessor(proc);
						updateProcessorTable();
					}
					processorWriterView.setJsonValid(true);
				} catch (Exception e1) {
					// logger.warn(e.getMessage() + ", invalid json");
					processorWriterView.setJsonValid(false);
				}
			}
		});

		JMenuItem mntmNew = new JMenuItem("New");
		mntmNew.addActionListener(createProcessorAction);
		mntmNew.setIcon(new ImageIcon(OutputProcessorsView.class
				.getResource("/com/sun/java/swing/plaf/windows/icons/File.gif")));
		mntmNew.setSelectedIcon(null);
		extendedPopupMenu.add(mntmNew);

		JMenuItem mntmDelete = new JMenuItem("Delete");
		mntmDelete.addActionListener(listMenuDeleteButtonActionListener);
		mntmDelete.setIcon(new ImageIcon(OutputProcessorsView.class
				.getResource("/javax/swing/plaf/metal/icons/ocean/error.png")));
		mntmDelete.setSelectedIcon(null);
		extendedPopupMenu.add(mntmDelete);

		mntAddProcessor = new JMenuItem("Add Processor");
		mntAddProcessor.setIcon(new ImageIcon(OutputProcessorsView.class
				.getResource("/com/sun/java/swing/plaf/windows/icons/NewFolder.gif")));
		mntAddProcessor.setSelectedIcon(null);
		mntAddProcessor.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (selectedWriter.getProcessor() instanceof CombineProcessor) {
					CombineProcessor combineProc = (CombineProcessor) selectedWriter.getProcessor();
					ForEachPedestrianPositionProcessor processor = generateForEachPedPositionProcessor();

					if (processor != null) {
						List<ForEachPedestrianPositionProcessor> procList = combineProc.getProcessorList();
						procList.add(processor);
						selectedWriter.setProcessor(new CombineProcessor(procList));
					}
				}
			}
		});
		// extendedPopupMenu.add(mntAddProcessor);
	}

	private ForEachPedestrianPositionProcessor generateForEachPedPositionProcessor() {

		ForEachPedestrianPositionProcessor processor = null;
		final ProcessorFactory procFactory = ProcessorFactory.getInstance();
		final ChooseNameDialog nDialog = new ChooseNameDialog(thisOwner, "Select Processor",
				procFactory.getForEachPedestrianPositionProcessorNames().toArray(new String[] {}));

		if (!nDialog.isAborted()) {
			String procName = nDialog.getSelectedName();

			if (procName.equals(PedestrianDensityProcessor.class.getSimpleName())) {
				processor = ProcessorFactory.getInstance().createPedestrianDensityProcessor(generateDensityProcessor());
			} else if (procName.equals(PedestrianFlowProcessor.class.getSimpleName())) {
				processor = procFactory.createPedestrianFlowProcessor(generateDensityProcessor());
			} else {
				processor = (ForEachPedestrianPositionProcessor) procFactory
						.createProcessor(procFactory.toProcessorType(procName));
			}
		}

		return processor;
	}

	private DensityProcessor generateDensityProcessor() {
		DensityProcessor processor = null;
		final ProcessorFactory procFactory = ProcessorFactory.getInstance();
		final ChooseNameDialog densityDialog = new ChooseNameDialog(thisOwner, "Select DensityProcessor",
				procFactory.getDensityProcessorNames().toArray(new String[] {}));
		String densityProcessorName = densityDialog.getSelectedName();
		if (densityProcessorName != null) {
			processor =
					(DensityProcessor) procFactory.createProcessor(procFactory.toProcessorType(densityProcessorName));
		}
		return processor;
	}

	private static void addPopup(Component component, final JPopupMenu popup) {
		component.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				if (e.isPopupTrigger()) {
					showMenu(e);
				}
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				if (e.isPopupTrigger()) {
					showMenu(e);
				}
			}

			private void showMenu(MouseEvent e) {
				if (e.getComponent().isEnabled())
					popup.show(e.getComponent(), e.getX(), e.getY());
			}
		});
	}

	public void isEditable(final boolean isEditable) {
		processorWriterView.setEditable(isEditable);
		columnNamesView.setEditable(isEditable);
		setColumnNamesButton.setEnabled(isEditable);
		table.setEnabled(isEditable);

		table.removeMouseListener(listMouseListener);
		if (isEditable) {
			table.addMouseListener(listMouseListener);
		}
	}
}
