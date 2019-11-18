package org.vadere.gui.components.view;

import org.vadere.gui.components.model.IDefaultModel;
import org.vadere.gui.components.utils.Messages;
import org.vadere.gui.topographycreator.model.TopographyCreatorModel;
import org.vadere.state.attributes.models.AttributesFloorField;
import org.vadere.state.util.StateJsonConverter;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.util.Observable;
import java.util.Observer;

import javax.swing.*;

/**
 * The About-Dialog of the topographycreator.
 * 
 */
public class InfoPanel extends JPanel implements Observer {

	private static final long serialVersionUID = -1865670281109979704L;
	public static final String NO_HASH = "--------";

	private final IDefaultModel defaultModel;
	private JLabel lblScenarioSizeLabel;
	private JLabel lblGridResulutionLabel;
	private JLabel lblCursorPositionLabel;
	private JLabel lblScaleFactorLabel;
	private JLabel lblScenarioHashLabel;

	private JLabel lblScenarioSizeValue;
	private JLabel lblGridResulutionValue;
	private JLabel lblCursorPositionValue;
	private JLabel lblScaleFactorValue;
	private JLabel lblScenarioHashValue;

	private String hash;

	public InfoPanel(final IDefaultModel defaultModel) {
		this.defaultModel = defaultModel;
		setLayout(new FlowLayout(FlowLayout.LEFT));
		lblScenarioSizeLabel = new JLabel(Messages.getString("InfoPanel.ScenarioSize.label") + ":");
		lblGridResulutionLabel = new JLabel(Messages.getString("InfoPanel.GridResolution.label") + ":");
		lblCursorPositionLabel = new JLabel(Messages.getString("InfoPanel.CursorPosition.label") + ":");
		lblScaleFactorLabel = new JLabel(Messages.getString("InfoPanel.ScaleFactor.label") + ":");
		lblScenarioHashLabel = new JLabel(Messages.getString("InfoPanel.ScenarioHash.label") + ":");

		lblScaleFactorValue = new JLabel();
		lblScenarioSizeValue = new JLabel();
		lblGridResulutionValue = new JLabel();
		lblCursorPositionValue = new JLabel();
		lblScenarioHashValue = new JLabel();

		add(lblGridResulutionLabel);
		add(lblGridResulutionValue);

		add(lblScenarioSizeLabel);
		add(lblScenarioSizeValue);

		add(lblCursorPositionLabel);
		add(lblCursorPositionValue);

		add(lblScaleFactorLabel);
		add(lblScaleFactorValue);

		add(lblScenarioHashLabel);
		add(lblScenarioHashValue);

		hash = NO_HASH;

		// copy full FloorFieldHash to clipboard after clicking on the short version of the hash
		lblScenarioHashValue.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				StringSelection stringSelection = new StringSelection(hash);
				Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
				clipboard.setContents(stringSelection, null);
			}
		});
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
			lblScaleFactorValue.setText(String.format("%3.2f | ", (float) defaultModel.getScaleFactor()));

			// show short version of FloorFieldHash in label and full hash as tooltip.
			if (defaultModel instanceof TopographyCreatorModel){
				TopographyCreatorModel m = (TopographyCreatorModel)defaultModel;
				AttributesFloorField attFF = m.getScenario().getModelAttributes()
						.stream()
						.filter(a -> a instanceof AttributesFloorField)
						.map(a ->(AttributesFloorField)a)
						.findFirst().orElse(null);
				if (attFF != null){
					hash = 	StateJsonConverter.getFloorFieldHash(m.getTopography(), attFF);
				}
			} else  {
				hash = NO_HASH;
			}

			int hashSubstringEnd = Math.min(8, hash.length());
			lblScenarioHashValue.setText(hash.substring(0, hashSubstringEnd) + " | ");
			lblScenarioHashValue.setToolTipText(hash);
		}
	}
}
