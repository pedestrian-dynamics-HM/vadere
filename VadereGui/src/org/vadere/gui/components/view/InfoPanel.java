package org.vadere.gui.components.view;

import java.awt.FlowLayout;
import java.awt.geom.Rectangle2D;
import java.util.Observable;
import java.util.Observer;

import javax.swing.JLabel;
import javax.swing.JPanel;

import org.vadere.gui.components.model.IDefaultModel;
import org.vadere.gui.components.utils.Messages;

/**
 * The About-Dialog of the topographycreator.
 * 
 */
public class InfoPanel extends JPanel implements Observer {

	private static final long serialVersionUID = -1865670281109979704L;
	private final IDefaultModel defaultModel;
	private JLabel lblScenarioSizeLabel;
	private JLabel lblGridResulutionLabel;
	private JLabel lblCursorPositionLabel;
	private JLabel lblScaleFactorLabel;

	private JLabel lblScenarioSizeValue;
	private JLabel lblGridResulutionValue;
	private JLabel lblCursorPositionValue;
	private JLabel lblScaleFactorValue;

	public InfoPanel(final IDefaultModel defaultModel) {
		this.defaultModel = defaultModel;
		setLayout(new FlowLayout(FlowLayout.LEFT));
		lblScenarioSizeLabel = new JLabel(Messages.getString("InfoPanel.ScenarioSize.label") + ":");
		lblGridResulutionLabel = new JLabel(Messages.getString("InfoPanel.GridResolution.label") + ":");
		lblCursorPositionLabel = new JLabel(Messages.getString("InfoPanel.CursorPosition.label") + ":");
		lblScaleFactorLabel = new JLabel(Messages.getString("InfoPanel.ScaleFactor.label") + ":");

		lblScaleFactorValue = new JLabel();
		lblScenarioSizeValue = new JLabel();
		lblGridResulutionValue = new JLabel();
		lblCursorPositionValue = new JLabel();

		add(lblGridResulutionLabel);
		add(lblGridResulutionValue);

		add(lblScenarioSizeLabel);
		add(lblScenarioSizeValue);

		add(lblCursorPositionLabel);
		add(lblCursorPositionValue);

		add(lblScaleFactorLabel);
		add(lblScaleFactorValue);
	}

	@Override
	public void update(Observable o, Object arg) {
		if (defaultModel.getTopography() != null) {
			lblGridResulutionValue.setText(String.format("%3.2f | ", defaultModel.getGridResolution()));
			Rectangle2D.Double topographyBound = defaultModel.getTopographyBound();

			lblScenarioSizeValue.setText(String.format("%3.2f %3.2f | ", (float) topographyBound.getWidth(),
					(float) topographyBound.getHeight()));
			lblCursorPositionValue
					.setText(String.format("%3.2f %3.2f | ", (float) defaultModel.getMousePosition().getX(),
							(float) defaultModel.getMousePosition().getY()));
			lblScaleFactorValue.setText(String.format("%3.2f ", (float) defaultModel.getScaleFactor()));
		}
	}
}
