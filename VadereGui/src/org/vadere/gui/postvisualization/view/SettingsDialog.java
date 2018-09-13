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
		CellConstraints cc = new CellConstraints();
		JLayeredPane additionalLayeredPane = new JLayeredPane();
		additionalLayeredPane.setBorder(
				BorderFactory.createTitledBorder(Messages.getString("PostVis.additional.border.text")));
		FormLayout additionalLayout = new FormLayout("5dlu, pref, 5dlu", // col
				"5dlu, pref, 2dlu, pref, 2dlu, pref, 5dlu"); // rows
		additionalLayeredPane.setLayout(additionalLayout);
		JCheckBox chCleanPed = new JCheckBox(Messages.getString("PostVis.chbHidePedAtTarget.text"));
		JCheckBox chCleanSnapshot = new JCheckBox(Messages.getString("PostVis.chbCleanSnapshot.text"));
		JCheckBox chCleanTrajecties = new JCheckBox(Messages.getString("PostVis.chbHideTrajAtTarget.text"));

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

		additionalLayeredPane.add(chCleanPed, cc.xy(2, 2));
		additionalLayeredPane.add(chCleanTrajecties, cc.xy(2, 4));
		additionalLayeredPane.add(chCleanSnapshot, cc.xy(2, 6));

		JCheckBox chShowEvacTimeColor = new JCheckBox(Messages.getString("PostVis.chShowEvacTimeColor.text"));
		getColorLayeredPane().add(chShowEvacTimeColor, cc.xyw(2, 22, 8));

		chShowEvacTimeColor.addItemListener(e -> {
			model.config.setUseEvacuationTimeColor(!model.config.isUseEvacuationTimeColor());
			model.notifyObservers();
		});

		PedestrianColorPanel pedestrianColorPanel = new PedestrianColorPanel(model.getPedestrianColorTableModel());
		getColorLayeredPane().add(pedestrianColorPanel, cc.xyw(2, 20, 8));



		return additionalLayeredPane;
	}
}
