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

import org.vadere.gui.topographycreator.model.IDrawPanelModel;
import org.vadere.util.geometry.shapes.VRectangle;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;
import com.jgoodies.forms.layout.Sizes;
import com.jgoodies.forms.util.LayoutStyle;

/**
 * Dialog for set the topographyBounds.
 * 
 * 
 */
public class SetScenarioBoundDialog extends JDialog {

	private static final long serialVersionUID = 9011978017793438028L;
	private final JTextField textField;
	private final JTextField textField_1;

	public SetScenarioBoundDialog(final IDrawPanelModel model) {
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
		JLabel lblPleaeseSelectThe = new JLabel("Pleaese select the scenario dimension:");
		getContentPane().add(lblPleaeseSelectThe, "2, 2, 7, 1");

		JLabel lblX = new JLabel("Width:");
		getContentPane().add(lblX, "2, 4");

		textField = new JTextField();
		getContentPane().add(textField, "4, 4, fill, default");
		textField.setColumns(10);

		JLabel lblY = new JLabel("Height:");
		getContentPane().add(lblY, "6, 4, right, default");

		textField_1 = new JTextField();
		getContentPane().add(textField_1, "8, 4, fill, default");
		textField_1.setColumns(10);

		JButton btnOk = new JButton("OK");

		btnOk.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				int width = 0;
				int height = 0;
				try {
					width = Integer.valueOf(textField.getText());
					height = Integer.valueOf(textField_1.getText());
				} catch (NumberFormatException ne) {
					JOptionPane.showMessageDialog(SetScenarioBoundDialog.this,
							"Pleaese enter a natural number bigger than zero.", "Error", JOptionPane.ERROR_MESSAGE);
					throw new NumberFormatException(ne.getMessage());
				}

				model.setTopographyBound(new VRectangle(0, 0, width, height));
				model.notifyObservers();
				SetScenarioBoundDialog.this.dispose();
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
