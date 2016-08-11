package org.vadere.gui.projectview.view;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.vadere.simulator.projects.dataprocessing.processors.ProcessorFactory;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.util.Collection;

public class ChooseProcessorTypeDialog extends JDialog {

	private Logger logger = LogManager.getLogger(getClass());

	/**
	 * generated uid
	 */
	private static final long serialVersionUID = -1805248549919350762L;
	private final JPanel contentPanel = new JPanel();
	private JComboBox<String> comboBoxProcessorType;
	private ChooseProcessorTypeDialog thisFrame;
	private JTextPane txtpnProcessordescription;
	private String selectedProcessor = "";
	private boolean aborted;

	public String getSelectedProcessorSimpleName() {
		return selectedProcessor;
	}

	private void setProcText(final String simpleProcName) {
		// Processor proc;
		// proc =
		// ProcessorFactory.getInstance().createProcessor(ProcessorFactory.getInstance().toProcessorType(simpleProcName));
		txtpnProcessordescription.setText("missing description");

	}

	/**
	 * Create the dialog.
	 */
	public ChooseProcessorTypeDialog(Frame owner) {
		super(owner, true);
		thisFrame = this;
		this.aborted = false;

		setTitle("Choose Processor Type");
		setBounds(100, 100, 450, 300);
		getContentPane().setLayout(new BorderLayout());
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(contentPanel, BorderLayout.CENTER);

		// change the processor description when the user selected a new
		// processor
		contentPanel.setLayout(new BorderLayout(0, 0));
		{
			comboBoxProcessorType = new JComboBox<>();
			Collection<String> simpleProcessorNames = ProcessorFactory.getInstance().getSimpleProcessorNamesForGui();
			comboBoxProcessorType
					.setModel(new DefaultComboBoxModel<String>(simpleProcessorNames.toArray(new String[] {})));
			comboBoxProcessorType.addItemListener(new ItemListener() {

				@Override
				public void itemStateChanged(ItemEvent arg0) {
					String simpleProcName = (String) (arg0.getItem());
					setProcText(simpleProcName);
				}
			});
			contentPanel.add(comboBoxProcessorType, BorderLayout.NORTH);
		}
		// the text pane with the processor description
		{
			txtpnProcessordescription = new JTextPane();
			contentPanel.add(txtpnProcessordescription, BorderLayout.CENTER);
			setProcText((String) comboBoxProcessorType.getSelectedItem());
		}


		// the panel where you can actually click ok after having selected an
		// IOutputProcessor type
		{
			JPanel buttonPane = new JPanel();
			buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
			getContentPane().add(buttonPane, BorderLayout.SOUTH);
			{
				JButton okButton = new JButton("OK");
				okButton.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent arg0) {
						selectedProcessor =
								(String) ChooseProcessorTypeDialog.this.comboBoxProcessorType.getSelectedItem();
						ChooseProcessorTypeDialog.this.dispose();
					}
				});
				okButton.setActionCommand("OK");
				buttonPane.add(okButton);
				getRootPane().setDefaultButton(okButton);
			}
		}

		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				aborted = true;
			}
		});
	}

	public boolean isAborted() {
		return aborted;
	}
}
