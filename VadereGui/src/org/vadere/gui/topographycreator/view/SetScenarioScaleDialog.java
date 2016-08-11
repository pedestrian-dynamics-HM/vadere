package org.vadere.gui.topographycreator.view;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.undo.UndoableEditSupport;

import org.vadere.gui.topographycreator.control.ActionScaleTopography;
import org.vadere.gui.topographycreator.model.IDrawPanelModel;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;
import com.jgoodies.forms.layout.Sizes;
import com.jgoodies.forms.util.LayoutStyle;

/**
 * Dialog for scaling the scenario.
 * 
 *
 */
public class SetScenarioScaleDialog extends JDialog {

	private static final long serialVersionUID = -4302266727134122320L;
	private final JTextField textField;

	public SetScenarioScaleDialog(final IDrawPanelModel model, final UndoableEditSupport undoSupport) {
		getContentPane()
				.setLayout(
						new FormLayout(new ColumnSpec[] {ColumnSpec.decode("2dlu"), ColumnSpec.decode("left:pref"),
								ColumnSpec.decode("left:2dlu"), ColumnSpec.decode("left:pref"),
								ColumnSpec.decode("left:2dlu"), new ColumnSpec(Sizes.PREFERRED),
								ColumnSpec.decode("2dlu"), new ColumnSpec(Sizes.PREFERRED),
								new ColumnSpec(Sizes.PREFERRED), ColumnSpec.decode("2dlu"),}, new RowSpec[] {
										RowSpec.createGap(LayoutStyle.getCurrent().getNarrowLinePad()),
										new RowSpec(Sizes.PREFERRED), RowSpec.decode("10dlu"),
										new RowSpec(Sizes.PREFERRED),
										RowSpec.decode("10dlu"), new RowSpec(Sizes.PREFERRED),
										RowSpec.createGap(LayoutStyle.getCurrent().getNarrowLinePad())}));
		JLabel lblPleaeseSelectThe = new JLabel("Pleaese select a scale factor:");
		getContentPane().add(lblPleaeseSelectThe, "2, 2, 7, 1");

		JLabel lblX = new JLabel("Scale:");
		getContentPane().add(lblX, "2, 4");

		textField = new JTextField();
		getContentPane().add(textField, "4, 4, fill, default");
		textField.setColumns(10);

		JButton btnOk = new JButton("OK");

		btnOk.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				double scale = 0;
				try {
					scale = Double.valueOf(textField.getText());
				} catch (NumberFormatException ne) {
					JOptionPane.showMessageDialog(SetScenarioScaleDialog.this,
							"Pleaese enter a floating number bigger than zero.", "Error", JOptionPane.ERROR_MESSAGE);
					throw new NumberFormatException(ne.getMessage());
				}

				model.setScalingFactor(scale);
				new ActionScaleTopography("Scale Scenario", model, undoSupport).actionPerformed(event);
				model.notifyObservers();
				SetScenarioScaleDialog.this.dispose();
			}
		});

		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		setResizable(false);
		setTitle("Scenario Creator");

		getContentPane().add(btnOk, "8, 6");
		pack();
		setVisible(true);
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		setLocation(screenSize.width / 2 - this.getSize().width / 2, screenSize.height / 2 - this.getSize().height / 2);
	}
}
