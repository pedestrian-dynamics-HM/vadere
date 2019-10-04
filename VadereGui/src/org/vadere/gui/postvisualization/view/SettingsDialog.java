package org.vadere.gui.postvisualization.view;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import org.jetbrains.annotations.NotNull;
import org.vadere.gui.components.utils.Messages;
import org.vadere.gui.postvisualization.model.PostvisualizationModel;

import javax.swing.*;

public class SettingsDialog extends org.vadere.gui.components.view.SettingsDialog {

	private PostvisualizationModel model;

	public SettingsDialog(@NotNull final PostvisualizationModel model) {
		super(model);
		this.model = model;
	}

	@Override
	protected JLayeredPane getAdditionalOptionPanel() {
		JLayeredPane additionalLayeredPane = new JLayeredPane();
		additionalLayeredPane.setBorder(
				BorderFactory.createTitledBorder(Messages.getString("PostVis.additional.border.text")));

		FormLayout additionalLayout = new FormLayout("5dlu, pref, 5dlu", // col
				createCellsWithSeparators(4)); // rows
		additionalLayeredPane.setLayout(additionalLayout);

		JCheckBox chCleanPed = new JCheckBox(Messages.getString("PostVis.chbHidePedAtTarget.text"));
		JCheckBox chCleanSnapshot = new JCheckBox(Messages.getString("PostVis.chbCleanSnapshot.text"));
		JCheckBox chCleanTrajecties = new JCheckBox(Messages.getString("PostVis.chbHideTrajAtTarget.text"));
		JCheckBox chShowAllTrajOnSnapshot = new JCheckBox(Messages.getString("PostVis.chShowAllTrajOnSnapshot.text"));

		chCleanPed.setSelected(!model.config.isShowTrajecoriesOnSnapshot());
		chCleanPed.addItemListener(e -> {
			model.config.setShowFaydedPedestrians(!model.config.isShowFaydedPedestrians());
			model.notifyObservers();
		});

		chCleanTrajecties.setSelected(!model.config.isShowAllTrajectories());
		chCleanTrajecties.addItemListener(e -> {
			model.config.setShowAllTrajectories(!model.config.isShowAllTrajectories());
			model.notifyObservers();
		});

		chCleanSnapshot.setSelected(!model.config.isShowTrajecoriesOnSnapshot());
		chCleanSnapshot.addItemListener(
				e -> model.config.setShowTrajecoriesOnSnapshot(!model.config.isShowTrajecoriesOnSnapshot()));

		chShowAllTrajOnSnapshot.setSelected(model.config.isShowAllTrajOnSnapshot());
		chShowAllTrajOnSnapshot.addItemListener(e -> {
			model.config.setShowAllTrajOnSnapshot(!model.config.isShowAllTrajOnSnapshot());
			model.notifyObservers();
		});

		int row = 0;
		int column = 2;
		int colSpan = 8;
		CellConstraints cc = new CellConstraints();

		additionalLayeredPane.add(chCleanPed, cc.xy(column, row += NEXT_CELL));
		additionalLayeredPane.add(chCleanTrajecties, cc.xy(column, row += NEXT_CELL));
		additionalLayeredPane.add(chCleanSnapshot, cc.xy(column, row += NEXT_CELL));
		additionalLayeredPane.add(chShowAllTrajOnSnapshot, cc.xy(column, row += NEXT_CELL));

		PedestrianColorPanel pedestrianColorPanel = new PedestrianColorPanel(model.getPedestrianColorTableModel());
		getColorSettingsPane().add(pedestrianColorPanel, cc.xyw(column, getRowForCriteriaColoring(), colSpan));

		JCheckBox chShowEvacTimeColor = new JCheckBox(Messages.getString("PostVis.chShowEvacTimeColor.text"));
		getColorSettingsPane().add(chShowEvacTimeColor, cc.xyw(column, getRowForEvacuationColoring(), colSpan));

		chShowEvacTimeColor.addItemListener(e -> {
			model.config.setUseEvacuationTimeColor(!model.config.isUseEvacuationTimeColor());
			model.notifyObservers();
		});

		return additionalLayeredPane;
	}
}
