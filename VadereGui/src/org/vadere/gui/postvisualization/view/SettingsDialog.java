package org.vadere.gui.postvisualization.view;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import org.jetbrains.annotations.NotNull;
import org.vadere.gui.components.model.AgentColoring;
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
	public void initComponents() {
		super.initComponents();
		int row = 18; //TODO this is hard coded; must be adapted when further elements are inserted above
		CellConstraints cc = new CellConstraints();
		JRadioButton chShowEvacTimeColor = new JRadioButton(Messages.getString("PostVis.chShowEvacTimeColor.text"));
		agentColorSettingsPane.add(chShowEvacTimeColor, cc.xyw(2, row += NEXT_CELL, 9));
		chShowEvacTimeColor.addItemListener(e -> {
			model.setAgentColoring(AgentColoring.EVACUATION_TIMES);
			model.notifyObservers();
		});


		JRadioButton chShowCriteriaColor = new JRadioButton(Messages.getString("PostVis.chShowCriteriaColor.text") + ":");
		PedestrianColorPanel pedestrianColorPanel = new PedestrianColorPanel(model);
		agentColorSettingsPane.add(chShowCriteriaColor, cc.xy(2, row += NEXT_CELL,  CellConstraints.LEFT, CellConstraints.TOP));
		agentColorSettingsPane.add(pedestrianColorPanel, cc.xyw(4, row, 7));
		chShowCriteriaColor.addItemListener(e -> {
			model.setAgentColoring(AgentColoring.PREDICATE);
			model.notifyObservers();
		});


		group.add(chShowEvacTimeColor);
		group.add(chShowCriteriaColor);
	}

	@Override
	protected JLayeredPane getAdditionalOptionPanel() {
		JLayeredPane additionalLayeredPane = new JLayeredPane();
		additionalLayeredPane.setBorder(
				BorderFactory.createTitledBorder(Messages.getString("PostVis.additional.border.text")));

		FormLayout additionalLayout = new FormLayout("5dlu, pref, 5dlu", // col
				"5dlu, pref, 2dlu, pref, 5dlu"); // rows
		additionalLayeredPane.setLayout(additionalLayout);

		JCheckBox chCleanPed = new JCheckBox(Messages.getString("PostVis.chbHidePedAtTarget.text"));
		JCheckBox chCleanTrajecties = new JCheckBox(Messages.getString("PostVis.chbHideTrajAtTarget.text"));

		chCleanPed.setSelected(!model.config.isShowFaydedPedestrians());
		chCleanPed.addItemListener(e -> {
			model.config.setShowFaydedPedestrians(!model.config.isShowFaydedPedestrians());
			model.notifyObservers();
		});

		chCleanTrajecties.setSelected(!model.config.isShowAllTrajectories());
		chCleanTrajecties.addItemListener(e -> {
			model.config.setShowAllTrajectories(!model.config.isShowAllTrajectories());
			model.notifyObservers();
		});

		CellConstraints cc = new CellConstraints();
		additionalLayeredPane.add(chCleanPed, cc.xy(2, 2));
		additionalLayeredPane.add(chCleanTrajecties, cc.xy(2, 4));

		return additionalLayeredPane;
	}
}
