package org.vadere.gui.topographycreator.view;

import org.vadere.gui.components.view.ISelectScenarioElementListener;
import org.vadere.gui.projectview.view.ProjectView;
import org.vadere.gui.topographycreator.control.ActionSimplifyObstacles;
import org.vadere.gui.topographycreator.model.IDrawPanelModel;
import org.vadere.state.scenario.ScenarioElement;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Dialog which collects the ids of selected obstacles. The collected list will be combined into
 * a single obstacle based on the convex hull.
 *
 * @author Stefan Schuhbäck
 */
public class ActionCombineDialog extends JDialog implements ISelectScenarioElementListener {

	private final JButton ok;
	private final JButton close;
	private final JTextField textField;
	private final IDrawPanelModel<?> panelModel;
	private final ActionSimplifyObstacles action;
	private final ActionListener okBtn;


	public ActionCombineDialog(ActionSimplifyObstacles action, ActionListener okBtn, IDrawPanelModel<?> panelModel) {
		super(new JFrame(), "xxx", false);
		this.panelModel = panelModel;
		this.action = action;
		this.okBtn = okBtn;
		panelModel.addSelectScenarioElementListener(this);
		setLocationRelativeTo(ProjectView.getMainWindow());

		textField = new JTextField("", 40);

		JPanel messagePane = new JPanel();
		messagePane.add(textField);
		getContentPane().add(messagePane);

		textField.getDocument().addDocumentListener(new SimpleDocumentListener() {
			@Override
			public void handle(DocumentEvent e) {
				String text = textField.getText().replace(" ", "");
				String[] tmp = text.split(",");
				try {
					action.setIds(Arrays.stream(tmp).mapToInt(Integer::parseInt).boxed().collect(Collectors.toList()));
					textField.setForeground(Color.BLACK);
				} catch (NumberFormatException ex){
					action.setIds(new ArrayList<>());
					textField.setForeground(Color.red);
				}
			}
		});

		JPanel btnPane = new JPanel();
		ok = new JButton("OK");
		ok.addActionListener(e -> {
			this.okBtn.actionPerformed(e);
			this.textField.setText("");
		});
		close = new JButton("Close");
		close.addActionListener(e -> {
			panelModel.removeSelectScenarioElementListener(this);
			setVisible(false);
			dispose();
		});
		btnPane.add(ok);
		btnPane.add(close);
		getContentPane().add(btnPane, BorderLayout.PAGE_END);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		pack();
		setVisible(true);
		setAlwaysOnTop(true);
	}

	@Override
	public void selectionChange(ScenarioElement element) {
		if(element != null) {
			if (element.getId() != -1) {
				String tmp = textField.getText().strip();
				if (tmp.length() == 0) {
					textField.setText(Integer.toString(element.getId()));
				} else if (tmp.endsWith(",")) {
					textField.setText(tmp + " " + element.getId());
				} else {
					textField.setText(tmp + ", " + element.getId());
				}

			}
		}
	}
}
