package org.vadere.gui.components.view;

import com.google.common.base.Strings;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import org.jetbrains.annotations.NotNull;
import org.vadere.gui.components.control.simulation.*;
import org.vadere.gui.components.model.AgentColoring;
import org.vadere.gui.components.model.DefaultSimulationConfig;
import org.vadere.gui.components.model.SimulationModel;
import org.vadere.gui.components.utils.Messages;
import org.vadere.gui.components.utils.SwingUtils;
import org.vadere.gui.postvisualization.control.ActionCloseSettingDialog;
import org.vadere.state.psychology.cognition.SelfCategory;
import org.vadere.state.psychology.information.InformationState;
import org.vadere.state.scenario.Target;
import org.vadere.util.config.VadereConfig;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Optional;

public class SettingsDialog extends JDialog {

	/**
	 * The content is usually placed in every second cell (row or column).
	 * Therefore, increment by two to jump to the next content cell.
	 */
	public final int NEXT_CELL = 2;

	public final int COLOR_JPANEL_WIDTH = 60;

	private DefaultSimulationConfig config;
	private final SimulationModel<? extends DefaultSimulationConfig> model;

	private JLayeredPane colorSettingsPane;
	protected JLayeredPane agentColorSettingsPane;
	private JLayeredPane otherSettingsPane;
	protected ButtonGroup group;

	public SettingsDialog(final SimulationModel<? extends DefaultSimulationConfig> model) {
		this.config = model.config;
		this.model = model;
	}

	public JLayeredPane getColorSettingsPane() {
		return colorSettingsPane;
	}

	/**
	 * The settings dialog consists of several rows:
	 * 1. Color settings for static scenario elements like sources or targets.
	 * 2. Color settings for agents.
	 * 3. Other settings consisting of:
	 *   * Visibility checkboxes to control whether scenario elements should be visible or not.
	 *   * Snapshot options (for PNG, SVG and TikZ export).
	 * 4. Post-visualization options (also called additional options).
	 * 5. A row containing a close button.
	 */
	public void initComponents() {
		this.setTitle(Messages.getString("SettingsDialog.title"));
		this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		this.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				new ActionCloseSettingDialog(SettingsDialog.this).actionPerformed(null);
			}
		});

		JPanel mainPanel = createMainPanel();
		JScrollPane scrollPane = createScrollPane(mainPanel);

		colorSettingsPane = new JLayeredPane();
		agentColorSettingsPane = new JLayeredPane();
		otherSettingsPane = new JLayeredPane();
		JPanel closePane = createClosePane();

		initColorSettingsPane(colorSettingsPane);
		initAgentColorSettingsPane(agentColorSettingsPane);
		initOtherSettingsPane(otherSettingsPane);

		// Note:
		// - Form layout indices start from 1 (and not 0)!
		// - First cell is a separator.
		// - Every second row/column is a cell representing a separator.
		// - The first statement increments "row" directly to get to the 2nd content row.
		int row = 0;
		int column = 2;
		CellConstraints cc = new CellConstraints();

		mainPanel.add(colorSettingsPane, cc.xy(column, row += NEXT_CELL));
		mainPanel.add(agentColorSettingsPane, cc.xy(column, row += NEXT_CELL));
		mainPanel.add(otherSettingsPane, cc.xy(column, row += NEXT_CELL));
		mainPanel.add(getAdditionalOptionPanel(), cc.xy(column, row += NEXT_CELL));
		mainPanel.add(closePane, cc.xy(column, row += NEXT_CELL));

		scrollPane.setPreferredSize(
				new Dimension(mainPanel.getPreferredSize().width+10,
						Math.min(mainPanel.getPreferredSize().height,
								Toolkit.getDefaultToolkit().getScreenSize().height - 150)));

		pack();
		setResizable(true);
		SwingUtils.centerComponent(this);
		setVisible(true);
	}

	@NotNull
	private JPanel createMainPanel() {
		JPanel mainPanel = new JPanel();
		FormLayout mainLayout = new FormLayout("5dlu, [300dlu,pref,600dlu], 5dlu", // cols
				createCellsWithSeparators(5)); // rows
		mainPanel.setLayout(mainLayout);
		return mainPanel;
	}

	@NotNull
	private JScrollPane createScrollPane(JPanel mainPanel) {
		JScrollPane scrollPane = new JScrollPane(mainPanel);
		scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		getContentPane().add(scrollPane);
		return scrollPane;
	}

	private JPanel createClosePane() {
		JPanel closePane = new JPanel();
		closePane.setLayout(new FlowLayout(FlowLayout.RIGHT));

		JButton closeButton = new JButton(Messages.getString("SettingsDialog.btnClose.text"));
		closeButton.addActionListener(new ActionCloseSettingDialog(this));

		closePane.add(closeButton);

		return closePane;
	}

	private void initColorSettingsPane(JLayeredPane colorSettingsPane){
		colorSettingsPane
				.setBorder(BorderFactory.createTitledBorder(Messages.getString("SettingsDialog.colors.border.text")));

		FormLayout colorSettingsLayout = new FormLayout("5dlu, pref:grow, 2dlu, pref, 2dlu, pref, 2dlu, pref, 2dlu, pref, 5dlu", // col
				createCellsWithSeparators(9)); // rows
		colorSettingsPane.setLayout(colorSettingsLayout);

		// For each scenario element, add a color preview canvas and a button to change the color.
		int row = 0;
		int column = 2;
		CellConstraints cc = new CellConstraints();

		colorSettingsPane.add(new JLabel(Messages.getString("SettingsDialog.lblObstacle.text") + ":"), cc.xy(column, row += NEXT_CELL));
		colorSettingsPane.add(new JLabel(Messages.getString("SettingsDialog.lblTarget.text") + ":"), cc.xy(column, row += NEXT_CELL));
		colorSettingsPane.add(new JLabel(Messages.getString("SettingsDialog.lblSource.text") + ":"), cc.xy(column, row += NEXT_CELL));
		colorSettingsPane.add(new JLabel(Messages.getString("SettingsDialog.lblStair.text") + ":"), cc.xy(column, row += NEXT_CELL));
		colorSettingsPane.add(new JLabel(Messages.getString("SettingsDialog.lblDensityColor.text") + ":"), cc.xy(column, row += NEXT_CELL));
		colorSettingsPane.add(new JLabel(Messages.getString("SettingsDialog.lblAbsorbingAreaColor.text") + ":"), cc.xy(column, row += NEXT_CELL));
		colorSettingsPane.add(new JLabel(Messages.getString("SettingsDialog.lblTargetChanger.text") + ":"), cc.xy(column, row += NEXT_CELL));
		colorSettingsPane.add(new JLabel(Messages.getString("SettingsDialog.lblAerosolCloudColor.text") + ":"), cc.xy(column, row += NEXT_CELL));
		colorSettingsPane.add(new JLabel(Messages.getString("SettingsDialog.lblDropletCloudColor.text") + ":"), cc.xy(column, row += NEXT_CELL));

		createColorCanvasesAndChangeButtonsOnPane(colorSettingsPane);
	}

	private void createColorCanvasesAndChangeButtonsOnPane(JLayeredPane colorSettingsPane) {
		int row = 0;
		int column2 = 4;
		int column3 = 6;
		int column4 = 8;
		int column5 = 10;
		CellConstraints cc = new CellConstraints();

		final JButton bObstColor = new JButton(Messages.getString("SettingsDialog.btnEditColor.text"));
		final JPanel pObstacleColor = new JPanel();
		pObstacleColor.setBackground(model.config.getObstacleColor());
		pObstacleColor.setPreferredSize(new Dimension(COLOR_JPANEL_WIDTH, 20));
		bObstColor.addActionListener(new ActionSetObstacleColor("Set Obstacle Color", model, pObstacleColor));
		colorSettingsPane.add(pObstacleColor, cc.xy(column4, row += NEXT_CELL));
		colorSettingsPane.add(bObstColor, cc.xy(column5, row));

		final JButton bTarColor = new JButton(Messages.getString("SettingsDialog.btnEditColor.text"));
		final JPanel pTargetColor = new JPanel();
		pTargetColor.setBackground(model.config.getTargetColor());
		pTargetColor.setPreferredSize(new Dimension(COLOR_JPANEL_WIDTH, 20));
		bTarColor.addActionListener(new ActionSetTargetColor("Set Target Color", model, pTargetColor));
		colorSettingsPane.add(pTargetColor, cc.xy(column4, row += NEXT_CELL));
		colorSettingsPane.add(bTarColor, cc.xy(column5, row));

		final JButton bSrcColor = new JButton(Messages.getString("SettingsDialog.btnEditColor.text"));
		final JPanel pSourceColor = new JPanel();
		pSourceColor.setBackground(model.config.getSourceColor());
		pSourceColor.setPreferredSize(new Dimension(COLOR_JPANEL_WIDTH, 20));
		bSrcColor.addActionListener(new ActionSetSourceColor("Set Source Color", model, pSourceColor));
		colorSettingsPane.add(pSourceColor, cc.xy(column4, row += NEXT_CELL));
		colorSettingsPane.add(bSrcColor, cc.xy(column5, row));

		final JButton bStairsColor = new JButton(Messages.getString("SettingsDialog.btnEditColor.text"));
		final JPanel pStairsColor = new JPanel();
		pStairsColor.setBackground(model.config.getStairColor());
		pStairsColor.setPreferredSize(new Dimension(COLOR_JPANEL_WIDTH, 20));
		bStairsColor.addActionListener(new ActionSetStairsColor("Set Stairs Color", model, pStairsColor));
		colorSettingsPane.add(pStairsColor, cc.xy(column4, row += NEXT_CELL));
		colorSettingsPane.add(bStairsColor, cc.xy(column5, row));

		final JButton bDensityColor = new JButton(Messages.getString("SettingsDialog.btnEditColor.text"));
		final JPanel pDensityColor = new JPanel();
		pDensityColor.setBackground(model.config.getDensityColor());
		pDensityColor.setPreferredSize(new Dimension(COLOR_JPANEL_WIDTH, 20));
		bDensityColor.addActionListener(new ActionSetDensityColor("Set Density Color", model, pDensityColor));
		colorSettingsPane.add(pDensityColor, cc.xy(column4, row += NEXT_CELL));
		colorSettingsPane.add(bDensityColor, cc.xy(column5, row));

		final JButton bAbsorbingAreaColor = new JButton(Messages.getString("SettingsDialog.btnEditColor.text"));
		final JPanel pAbsorbingAreaColor = new JPanel();
		pAbsorbingAreaColor.setBackground(model.config.getAbsorbingAreaColor());
		pAbsorbingAreaColor.setPreferredSize(new Dimension(COLOR_JPANEL_WIDTH, 20));
		bAbsorbingAreaColor.addActionListener(new ActionSetAbsorbingAreaColor("Set Absorbing Area Color", model, pAbsorbingAreaColor));
		colorSettingsPane.add(pAbsorbingAreaColor, cc.xy(column4, row += NEXT_CELL));
		colorSettingsPane.add(bAbsorbingAreaColor, cc.xy(column5, row));

		final JButton bTargetChangerColor = new JButton(Messages.getString("SettingsDialog.btnEditColor.text"));
		final JPanel pTargetChangerColor = new JPanel();
		pTargetChangerColor.setBackground(model.config.getTargetChangerColor());
		pTargetChangerColor.setPreferredSize(new Dimension(COLOR_JPANEL_WIDTH, 20));
		bTargetChangerColor.addActionListener(new ActionSetTargetChangerColor("Set Target Changer Color", model, pTargetChangerColor));
		colorSettingsPane.add(pTargetChangerColor, cc.xy(column4, row += NEXT_CELL));
		colorSettingsPane.add(bTargetChangerColor, cc.xy(column5, row));

		final JButton bAerosolCloudColor = new JButton(Messages.getString("SettingsDialog.btnEditColor.text"));
		final JPanel pAerosolCloudColor = new JPanel();
		pAerosolCloudColor.setBackground(model.config.getAerosolCloudColor());
		pAerosolCloudColor.setPreferredSize(new Dimension(COLOR_JPANEL_WIDTH, 20));
		bAerosolCloudColor.addActionListener(new ActionSetAerosolCloudColor("Set Aerosol Cloud Color", model, pAerosolCloudColor));
		colorSettingsPane.add(pAerosolCloudColor, cc.xy(column4, row += NEXT_CELL));
		colorSettingsPane.add(bAerosolCloudColor, cc.xy(column5, row));
		final JSpinner spinnerAerosolCloudAlpha = new JSpinner();
		spinnerAerosolCloudAlpha.setPreferredSize(new Dimension(60, 20));
		final SpinnerNumberModel sModelAerosolCloudAlpha = new SpinnerNumberModel(model.config.getAerosolCloudAlphaMax(),
				0, 255, 1);
		spinnerAerosolCloudAlpha.setModel(sModelAerosolCloudAlpha);
		spinnerAerosolCloudAlpha.addChangeListener(e -> {
			model.config.setAerosolCloudAlphaMax((int) sModelAerosolCloudAlpha.getValue());
			model.notifyObservers();
		});
		colorSettingsPane.add(new JLabel(Messages.getString("SettingsDialog.lblAerosolCloudAlpha.text") + ":"), cc.xy(column2, row));
		colorSettingsPane.add(spinnerAerosolCloudAlpha, cc.xy(column3, row));

		final JButton bDropletCloudColor = new JButton(Messages.getString("SettingsDialog.btnEditColor.text"));
		final JPanel pDropletCloudColor = new JPanel();
		pDropletCloudColor.setBackground(model.config.getDropletsColor());
		pDropletCloudColor.setPreferredSize(new Dimension(COLOR_JPANEL_WIDTH, 20));
		bDropletCloudColor.addActionListener(new ActionSetDropletsColor("Set Droplet Cloud Color", model, pDropletCloudColor));
		colorSettingsPane.add(pDropletCloudColor, cc.xy(column4, row += NEXT_CELL));
		colorSettingsPane.add(bDropletCloudColor, cc.xy(column5, row));
	}

	private void initAgentColorSettingsPane(JLayeredPane colorSettingsPane){
		int nOnlineVisualizationCells = 9;
		int nPostVisualizationCells = 6; // org.vadere.gui.postvisualization.view.SettingsDialog adds n cells (rows) to settingsPane
		FormLayout pedColorLayout = new FormLayout("5dlu, pref, 2dlu, pref, 2dlu, pref:grow, 2dlu, pref, 2dlu, pref, 5dlu",
				createCellsWithSeparators(nOnlineVisualizationCells + nPostVisualizationCells));

		colorSettingsPane.setLayout(pedColorLayout);
		colorSettingsPane.setBorder(BorderFactory.createTitledBorder(Messages.getString("SettingsDialog.pedcolors.border.text")));

		JRadioButton rbTargetColoring = createRadioButtonWithListener(AgentColoring.TARGET, Messages.getString("SettingsDialog.lblTargetColoring.text")+ ":");
		JRadioButton rbRandomColoring = createRadioButtonWithListener(AgentColoring.RANDOM, Messages.getString("SettingsDialog.chbUseRandomColors.text"));
		JRadioButton rbGroupColoring = createRadioButtonWithListener(AgentColoring.GROUP, Messages.getString("SettingsDialog.chbGroupColors.text"));
		JRadioButton rbSelfCategoryColoring = createRadioButtonWithListener(AgentColoring.SELF_CATEGORY, Messages.getString("SettingsDialog.lblSelfCategoryColoring.text")+ ":");
		JRadioButton rbHealthStatusColoring = createRadioButtonWithListener(AgentColoring.HEALTH_STATUS, Messages.getString("SettingsDialog.lblHealthStatusColoring.text")+ ":");
		JRadioButton rbInformationColoring = createRadioButtonWithListener(AgentColoring.INFORMATION_STATE, Messages.getString("SettingsDialog.lblInformationColoring.text")+ ":");


		rbTargetColoring.setSelected(true);
		model.setAgentColoring(AgentColoring.TARGET);

		group = new ButtonGroup();
		group.add(rbTargetColoring);
		group.add(rbRandomColoring);
		group.add(rbGroupColoring);
		group.add(rbSelfCategoryColoring);
		group.add(rbHealthStatusColoring);
		group.add(rbInformationColoring);

		JComboBox<Integer> cbTargetIds = createTargetIdsComboBoxAndAddIds();
		final JPanel pTargetColor = new JPanel();
		final JButton bChangeTargetColor = new JButton(Messages.getString("SettingsDialog.btnEditColor.text"));
		final JPanel pPedestrianColorNoTarget = new JPanel();
		final JButton bChangePedestrianColorNoTarget = new JButton(Messages.getString("SettingsDialog.btnEditColor.text"));

		initColoringByTargetId(cbTargetIds, pTargetColor, bChangeTargetColor, bChangePedestrianColorNoTarget, pPedestrianColorNoTarget);

		JComboBox<SelfCategory> cbSelfCategories = createSelfCategoriesComboBox();
		final JPanel pSelfCategoryColor = new JPanel();
		final JButton bChangeSelfCategoryColor = new JButton(Messages.getString("SettingsDialog.btnEditColor.text"));

		initColoringBySelfCategory(cbSelfCategories, pSelfCategoryColor, bChangeSelfCategoryColor);

		final JPanel pPedestrianColorLowerExposure = new JPanel();
		final JButton bChangePedestrianColorLowerExposure = new JButton(Messages.getString("SettingsDialog.btnEditColor.text"));

		final JPanel pPedestrianColorUpperExposure = new JPanel();
		final JButton bChangePedestrianColorUpperExposure = new JButton(Messages.getString("SettingsDialog.btnEditColor.text"));


		final JPanel pPedestrianColorInfectious = new JPanel();
		final JButton bChangePedestrianColorInfectious = new JButton(Messages.getString("SettingsDialog.btnEditColor.text"));

		initColoringByHealthStatus(bChangePedestrianColorLowerExposure, pPedestrianColorLowerExposure,
				bChangePedestrianColorUpperExposure, pPedestrianColorUpperExposure,
				bChangePedestrianColorInfectious, pPedestrianColorInfectious);

		final JSpinner spinnerLowerExposure = new JSpinner();
		spinnerLowerExposure.setPreferredSize(new Dimension(60, 20));
		final SpinnerNumberModel sModelLowerExposure = new SpinnerNumberModel(model.config.getLowerVisualizedExposure(), 0, model.config.getUpperVisualizedExposure(), 1);
		spinnerLowerExposure.setModel(sModelLowerExposure);
		spinnerLowerExposure.addChangeListener(e -> {
			model.config.setLowerVisualizedExposure((double) sModelLowerExposure.getValue());
			model.notifyObservers();
		});


		final JSpinner spinnerUpperExposure = new JSpinner();
		spinnerUpperExposure.setPreferredSize(new Dimension(60, 20));
		final SpinnerNumberModel sModelUpperExposure = new SpinnerNumberModel(model.config.getUpperVisualizedExposure(), model.config.getLowerVisualizedExposure(), null, 1);
		spinnerUpperExposure.setModel(sModelUpperExposure);
		spinnerUpperExposure.addChangeListener(e -> {
			model.config.setUpperVisualizedExposure((double) sModelUpperExposure.getValue());
			model.notifyObservers();
		});


		JComboBox<InformationState> cbInformationStates = createInformationStateComboBox();
		final JPanel pInformationStateColor = new JPanel();
		final JButton bChangeInformationStateColor = new JButton(Messages.getString("SettingsDialog.btnEditColor.text"));

		initColoringByInformationState(cbInformationStates, pInformationStateColor, bChangeInformationStateColor);


		int row = 0;
		int column1 = 2;
		int column2 = 4;
		int column3 = 6;
		int column4 = 8;
		int column5 = 10;

		CellConstraints cc = new CellConstraints();

		colorSettingsPane.add(rbTargetColoring, cc.xy(column1, row += NEXT_CELL));
		colorSettingsPane.add(cbTargetIds, cc.xy(column2, row));
		colorSettingsPane.add(pTargetColor, cc.xy(column4, row));
		colorSettingsPane.add(bChangeTargetColor, cc.xy(column5, row));

		colorSettingsPane.add(new JLabel(Messages.getString("SettingsDialog.lblPedestrianNoTarget.text") + ":"), cc.xy(column2, row += NEXT_CELL));
		colorSettingsPane.add(pPedestrianColorNoTarget, cc.xy(column4, row));
		colorSettingsPane.add(bChangePedestrianColorNoTarget, cc.xy(column5, row));

		colorSettingsPane.add(rbRandomColoring, cc.xyw(column1, row += NEXT_CELL, 9));
		colorSettingsPane.add(rbGroupColoring, cc.xyw(column1, row += NEXT_CELL, 9));

		colorSettingsPane.add(rbSelfCategoryColoring, cc.xy(column1, row += NEXT_CELL));
		colorSettingsPane.add(cbSelfCategories, cc.xy(column2, row));
		colorSettingsPane.add(pSelfCategoryColor, cc.xy(column4, row));
		colorSettingsPane.add(bChangeSelfCategoryColor, cc.xy(column5, row));

		colorSettingsPane.add(rbHealthStatusColoring, cc.xy(column1, row += NEXT_CELL));
		colorSettingsPane.add(new JLabel(Messages.getString("SettingsDialog.lblPedestrianLowerExposure.text") + ":"), cc.xy(column2, row));
		colorSettingsPane.add(spinnerLowerExposure, cc.xy(column3, row));
		colorSettingsPane.add(pPedestrianColorLowerExposure, cc.xy(column4, row));
		colorSettingsPane.add(bChangePedestrianColorLowerExposure, cc.xy(column5, row));

		colorSettingsPane.add(new JLabel(Messages.getString("SettingsDialog.lblPedestrianUpperExposure.text") + ":"), cc.xy(column2, row += NEXT_CELL));
		colorSettingsPane.add(spinnerUpperExposure, cc.xy(column3, row));
		colorSettingsPane.add(pPedestrianColorUpperExposure, cc.xy(column4, row));
		colorSettingsPane.add(bChangePedestrianColorUpperExposure, cc.xy(column5, row));

		colorSettingsPane.add(new JLabel(Messages.getString("SettingsDialog.lblPedestrianInfectious.text") + ":"), cc.xy(column2, row += NEXT_CELL));
		colorSettingsPane.add(pPedestrianColorInfectious, cc.xy(column4, row));
		colorSettingsPane.add(bChangePedestrianColorInfectious, cc.xy(column5, row));

		colorSettingsPane.add(rbInformationColoring, cc.xy(column1, row += NEXT_CELL));
		colorSettingsPane.add(cbInformationStates, cc.xy(column2, row));
		colorSettingsPane.add(pInformationStateColor, cc.xy(column4, row));
		colorSettingsPane.add(bChangeInformationStateColor, cc.xy(column5, row));

		// Evacuation time and criteria coloring comes in the next row see "postvisualization/.../SettingsDialog.java".
	}

	private JRadioButton createRadioButtonWithListener(AgentColoring colorScheme, String buttonText) {
		JRadioButton radioButton = new JRadioButton(buttonText);
		radioButton.addItemListener(e -> {
			model.setAgentColoring(colorScheme);
			model.notifyObservers();
		});

		return radioButton;
	}

	private JComboBox<Integer> createTargetIdsComboBoxAndAddIds() {
		java.util.List<Target> targets = model.getTopography().getTargets();
		Integer[] selectableTargets = new Integer[targets.size()];

		for (int i = 0; i < targets.size(); i++) {
			selectableTargets[i] = targets.get(i).getId();
		}

		JComboBox<Integer> comboBox = new JComboBox<>(selectableTargets);

		return comboBox;
	}

	private JComboBox<SelfCategory> createSelfCategoriesComboBox() {
		JComboBox<SelfCategory> comboBox = new JComboBox<>(SelfCategory.values());
		return comboBox;
	}

	private JComboBox<InformationState> createInformationStateComboBox() {
		JComboBox<InformationState> comboBox = new JComboBox<>(InformationState.values());
		return comboBox;
	}

	private void initColoringByTargetId(JComboBox<Integer> cbTargetIds, JPanel pTargetColor, JButton bChangeTargetColor, JButton bChangePedestrianColorNoTarget, JPanel pPedestrianColorNoTarget) {
		cbTargetIds.setSelectedIndex(0);

		Integer selectedTargetIdOuter = cbTargetIds.getItemAt(cbTargetIds.getSelectedIndex());
		if (selectedTargetIdOuter == null) {
			selectedTargetIdOuter = 1;
		}

		Optional<Color> colorByTargetId = model.config.getColorByTargetId(selectedTargetIdOuter);
		pTargetColor.setBackground(colorByTargetId.orElseGet(() -> model.config.getPedestrianDefaultColor()));
		pTargetColor.setPreferredSize(new Dimension(COLOR_JPANEL_WIDTH, 20));

		// When user changes a color, save it in the model.
		bChangeTargetColor.addActionListener(new ActionSetPedestrianColor("Set Pedestrian Color", model, pTargetColor,
				cbTargetIds));

		// Retrieve configured color from "model" or use default color.
		cbTargetIds.addActionListener(e -> {
			Integer selectedTargetIdInner = cbTargetIds.getItemAt(cbTargetIds.getSelectedIndex());
			if (selectedTargetIdInner == null) {
				selectedTargetIdInner = 1;
			}

			Optional<Color> colorByTarget = config.getColorByTargetId(selectedTargetIdInner);
			pTargetColor.setBackground(colorByTarget.orElseGet(() -> model.config.getPedestrianDefaultColor()));
		});

		Optional<Color> notTargetPedCol = config.getColorByTargetId((-1));
		pPedestrianColorNoTarget.setBackground(notTargetPedCol.orElseGet(() -> model.config.getPedestrianDefaultColor()));
		pPedestrianColorNoTarget.setPreferredSize(new Dimension(COLOR_JPANEL_WIDTH, 20));
		bChangePedestrianColorNoTarget.addActionListener(new ActionSetPedestrianWithoutTargetColor(
				"Set Pedestrian without Target Color", model, pPedestrianColorNoTarget));
	}

	private void initColoringBySelfCategory(JComboBox<SelfCategory> cbSelfCategories, JPanel pSelfCategoryColor, JButton bChangeSelfCategoryColor) {
		cbSelfCategories.setSelectedIndex(0);

		SelfCategory selectedSelfCategory = cbSelfCategories.getItemAt(cbSelfCategories.getSelectedIndex());
		Color selfCategoryColor = model.config.getSelfCategoryColor(selectedSelfCategory);

		pSelfCategoryColor.setBackground(selfCategoryColor);
		pSelfCategoryColor.setPreferredSize(new Dimension(COLOR_JPANEL_WIDTH, 20));

		// When user changes a color, save it in the model.
		bChangeSelfCategoryColor.addActionListener(new ActionSetSelfCategoryColor("Set Self Category Color", model, pSelfCategoryColor,
				cbSelfCategories));

		// Retrieve configured color from "model".
		cbSelfCategories.addActionListener(e -> {
			SelfCategory selectedSelfCategoryInner = cbSelfCategories.getItemAt(cbSelfCategories.getSelectedIndex());
			pSelfCategoryColor.setBackground(model.config.getSelfCategoryColor(selectedSelfCategoryInner));
		});
	}

	private void initColoringByHealthStatus(JButton bChangePedestrianColorLowerExposure, JPanel pPedestrianColorLowerExposure,
											JButton bChangePedestrianColorUpperExposure, JPanel pPedestrianColorUpperExposure,
											JButton bChangePedestrianColorInfectious, JPanel pPedestrianColorInfectious) {

		pPedestrianColorLowerExposure.setBackground(config.getPedestrianDefaultColor());
		pPedestrianColorLowerExposure.setPreferredSize(new Dimension(COLOR_JPANEL_WIDTH, 20));
		// When user changes a color, save it in the model.
		bChangePedestrianColorLowerExposure.addActionListener(new ActionSetPedestrianDefaultColor(
				"Set Pedestrian Exposure Color (lower limit)", model, pPedestrianColorLowerExposure));

		pPedestrianColorUpperExposure.setBackground(config.getExposedColor());
		pPedestrianColorUpperExposure.setPreferredSize(new Dimension(COLOR_JPANEL_WIDTH, 20));
		// When user changes a color, save it in the model.
		bChangePedestrianColorUpperExposure.addActionListener(new ActionSetPedestrianExposedColor(
				"Set Pedestrian Exposure Color (Upper Limit)", model, pPedestrianColorUpperExposure));

		pPedestrianColorInfectious.setBackground(config.getInfectiousColor());
		pPedestrianColorInfectious.setPreferredSize(new Dimension(COLOR_JPANEL_WIDTH, 20));
		// When user changes a color, save it in the model.
		bChangePedestrianColorInfectious.addActionListener(new ActionSetPedestrianInfectiousColor(
				"Set Pedestrian Infectious Color", model, pPedestrianColorInfectious));
	}

	private void initColoringByInformationState(JComboBox<InformationState> cbInformationStates, JPanel pInformationStateColor, JButton bChangeInformationStateColor) {

		cbInformationStates.setSelectedIndex(0);

		InformationState selectedInformationState = cbInformationStates.getItemAt(cbInformationStates.getSelectedIndex());
		Color informationStateColor = model.config.getInformationStateColor(selectedInformationState);

		pInformationStateColor.setBackground(informationStateColor);
		pInformationStateColor.setPreferredSize(new Dimension(COLOR_JPANEL_WIDTH, 20));

		// When user changes a color, save it in the model.
		bChangeInformationStateColor.addActionListener(new ActionSetInformationStateColor("Set Information State Color", model, pInformationStateColor,
				cbInformationStates));

		// Retrieve configured color from "model".
		cbInformationStates.addActionListener(e -> {
			InformationState selectedInformationStateInner = cbInformationStates.getItemAt(cbInformationStates.getSelectedIndex());
			pInformationStateColor.setBackground(model.config.getInformationStateColor(selectedInformationStateInner));
		});

		int i;
	}


	private void initOtherSettingsPane(JLayeredPane otherSettingsPane) {
		otherSettingsPane.setBorder(
				BorderFactory.createTitledBorder(Messages.getString("SettingsDialog.additional.border.text")));

		FormLayout otherSettingsLayout = new FormLayout(createCellsWithSeparators(4), // col
				createCellsWithSeparators(18)); // rows
		otherSettingsPane.setLayout(otherSettingsLayout);

		// For each scenario element, add a checkbox to toggle its visibility.
		JCheckBox chInterpolatePositions = new JCheckBox((Messages.getString("SettingsDialog.chbInterpolatePositions.text")));
		JCheckBox chShowObstacles = new JCheckBox((Messages.getString("SettingsDialog.chbShowObstacles.text")));
		JCheckBox chShowTargets = new JCheckBox((Messages.getString("SettingsDialog.chbShowTargets.text")));
		JCheckBox chShowSources = new JCheckBox((Messages.getString("SettingsDialog.chbShowSources.text")));
		JCheckBox chShowAbsorbingAreas = new JCheckBox((Messages.getString("SettingsDialog.chbShowAbsorbingAreas.text")));
		JCheckBox chShowAerosolClouds = new JCheckBox((Messages.getString("SettingsDialog.chbShowAerosolClouds.text")));
		JCheckBox chShowDropletClouds = new JCheckBox((Messages.getString("SettingsDialog.chbShowDropletClouds.text")));
		JCheckBox chShowMeasurementAreas = new JCheckBox((Messages.getString("SettingsDialog.chbShowMeasurementAreas.text")));
		JCheckBox chShowStairs = new JCheckBox((Messages.getString("SettingsDialog.chbShowStairs.text")));
		JCheckBox chShowTargetChangers = new JCheckBox((Messages.getString("SettingsDialog.chbShowTargetChangers.text")));
		JCheckBox chShowPedIds = new JCheckBox((Messages.getString("SettingsDialog.chbShowPedestrianIds.text")));
		JCheckBox chShowPedestrianInOutGroup = new JCheckBox((Messages.getString("SettingsDialog.chbShowPedestrianInOutGroup.text")));
		JCheckBox chHideVoronoiDiagram = new JCheckBox((Messages.getString("SettingsDialog.chbHideVoronoiDiagram.text")));

		chInterpolatePositions.setSelected(model.config.isInterpolatePositions());
		chInterpolatePositions.addItemListener(e -> {
			model.config.setInterpolatePositions(!model.config.isInterpolatePositions());
			model.notifyObservers();
		});

		chHideVoronoiDiagram.setSelected(!model.isVoronoiDiagramVisible());
		chHideVoronoiDiagram.addItemListener(e -> {
			if (model.isVoronoiDiagramVisible()) {
				model.hideVoronoiDiagram();
				model.notifyObservers();
			} else {
				model.showVoronoiDiagram();
				model.notifyObservers();
			}
		});

		chShowObstacles.setSelected(model.config.isShowObstacles());
		chShowObstacles.addItemListener(e -> {
			model.config.setShowObstacles(!model.config.isShowObstacles());
			model.notifyObservers();
		});

		chShowTargets.setSelected(model.config.isShowTargets());
		chShowTargets.addItemListener(e -> {
			model.config.setShowTargets(!model.config.isShowTargets());
			model.notifyObservers();
		});

		chShowSources.setSelected(model.config.isShowSources());
		chShowSources.addItemListener(e -> {
			model.config.setShowSources(!model.config.isShowSources());
			model.notifyObservers();
		});

		chShowAbsorbingAreas.setSelected(model.config.isShowAbsorbingAreas());
		chShowAbsorbingAreas.addItemListener(e -> {
			model.config.setShowAbsorbingAreas(!model.config.isShowAbsorbingAreas());
			model.notifyObservers();
		});

		chShowAerosolClouds.setSelected(model.config.isShowAerosolClouds());
		chShowAerosolClouds.addItemListener(e -> {
			model.config.setShowAerosolClouds(!model.config.isShowAerosolClouds());
			model.notifyObservers();
		});

		chShowDropletClouds.setSelected(model.config.isShowDroplets());
		chShowDropletClouds.addItemListener(e -> {
			model.config.setShowDroplets(!model.config.isShowDroplets());
			model.notifyObservers();
		});

		chShowMeasurementAreas.setSelected(model.config.isShowMeasurementArea());
		chShowMeasurementAreas.addItemListener(e -> {
			model.config.setShowMeasurementArea(!model.config.isShowMeasurementArea());
			model.notifyObservers();
		});

		chShowStairs.setSelected(model.config.isShowSources());
		chShowStairs.addItemListener(e -> {
			model.config.setShowStairs(!model.config.isShowStairs());
			model.notifyObservers();
		});

		chShowTargetChangers.setSelected(model.config.isShowTargetChangers());
		chShowTargetChangers.addItemListener(e -> {
			model.config.setShowTargetChangers(!model.config.isShowTargetChangers());
			model.notifyObservers();
		});

		chShowPedIds.setSelected(model.config.isShowPedestrianIds());
		chShowPedIds.addItemListener(e -> {
			model.config.setShowPedestrianIds(!model.config.isShowPedestrianIds());
			model.notifyObservers();
		});

        chShowPedestrianInOutGroup.setSelected(model.config.isShowPedestrianInOutGroup());
        chShowPedestrianInOutGroup.addItemListener(e -> {
            model.config.setShowPedestrianInOutGroup(!model.config.isShowPedestrianInOutGroup());
            model.notifyObservers();
        });

		int row = 0;
		int column = 2;
		int colSpan = 5;
		CellConstraints cc = new CellConstraints();

		otherSettingsPane.add(chInterpolatePositions, cc.xyw(column, row += NEXT_CELL, colSpan));
		otherSettingsPane.add(chHideVoronoiDiagram, cc.xyw(column, row += NEXT_CELL, colSpan));
		otherSettingsPane.add(chShowObstacles, cc.xyw(column, row += NEXT_CELL, colSpan));
		otherSettingsPane.add(chShowTargets, cc.xyw(column, row += NEXT_CELL, colSpan));
		otherSettingsPane.add(chShowSources, cc.xyw(column, row += NEXT_CELL, colSpan));
		otherSettingsPane.add(chShowStairs, cc.xyw(column, row += NEXT_CELL, colSpan));
		otherSettingsPane.add(chShowAbsorbingAreas, cc.xyw(column, row += NEXT_CELL, colSpan));
		otherSettingsPane.add(chShowMeasurementAreas, cc.xyw(column, row += NEXT_CELL, colSpan));
		otherSettingsPane.add(chShowTargetChangers, cc.xyw(column, row += NEXT_CELL, colSpan));
		otherSettingsPane.add(chShowPedIds, cc.xyw(column, row += NEXT_CELL, colSpan));
        otherSettingsPane.add(chShowPedestrianInOutGroup, cc.xyw(column, row += NEXT_CELL, colSpan));
		otherSettingsPane.add(chShowAerosolClouds, cc.xyw(column, row += NEXT_CELL, colSpan));
		otherSettingsPane.add(chShowDropletClouds, cc.xyw(column, row += NEXT_CELL, colSpan));

		JCheckBox chChowLogo = new JCheckBox(Messages.getString("SettingsDialog.chbLogo.text"));
		chChowLogo.setSelected(model.config.isShowLogo());
		chChowLogo.addItemListener(e -> {
			model.config.setShowLogo(!model.config.isShowLogo());
			model.notifyObservers();
		});
		otherSettingsPane.add(chChowLogo, cc.xyw(2, row += NEXT_CELL, 5));

		otherSettingsPane.add(new JLabel(Messages.getString("SettingsDialog.lblSnapshotDir.text") + ":"),
				cc.xy(2, row += NEXT_CELL));

		// Add text box to change snapshot directory
		JTextField tSnapshotDir = new JTextField(VadereConfig.getConfig().getString("SettingsDialog.snapshotDirectory.path", "."));
		tSnapshotDir.setEditable(false);
		tSnapshotDir.setPreferredSize(new Dimension(130, 20));
		otherSettingsPane.add(tSnapshotDir, cc.xy(4, row));
		final JButton bSnapshotDir = new JButton(Messages.getString("SettingsDialog.btnEditSnapshot.text"));
		bSnapshotDir.addActionListener(new ActionSetSnapshotDirectory("Set Snapshot Directory", model, tSnapshotDir, this));
		otherSettingsPane.add(bSnapshotDir, cc.xy(6, row));

		final JSpinner spinnerCellWidth = new JSpinner();
		final SpinnerNumberModel sModelCellWidth = new SpinnerNumberModel(model.config.getGridWidth(),
				model.config.getMinCellWidth(), model.config.getMaxCellWidth(), 0.1);
		spinnerCellWidth.setModel(sModelCellWidth);

		spinnerCellWidth.addChangeListener(e -> {
			model.config.setGridWidth((double) sModelCellWidth.getValue());
			model.notifyObservers();
		});

		otherSettingsPane.add(new JLabel(Messages.getString("SettingsDialog.lblCellWidth.text") + ":"),
				cc.xy(2, row += NEXT_CELL));
		otherSettingsPane.add(spinnerCellWidth, cc.xy(4, row));
	}

	/**
	 * FormLayout's mini language for column and row specification (or cells) is described here:
	 * http://www.informit.com/articles/article.aspx?p=655425&seqNum=2
	 *
	 * <ul>
	 *    <li>
	 *        Content columns are denoted with "pref".
	 *        They are wide as the widest component (based on the component’s preferred size).
	 *    </li>
	 *    <li>
	 *        Columns are separated by a comma character.
	 *    </li>
	 *   <li>
	 *       "dlu" means dialog units. Dialog units are based on the pixel size of the dialog box font.
	 *       They grow and shrink with the font and resolution. These elements represent thin separators.
	 *       "dlu" elements are typically used as separator.
	 *    </li>
	 * </ul>
	 */
	private String createCellsWithSeparators(int totalCells, String prefix, String suffix) {
		String cellWidth = "pref";
		String cellSeparator = ", ";
		String separatorWidth = "2dlu";

		// Watch out: "Strings.reapeat()" throws exception if count <= 1!
		// Thus, maybe, use another library instead in the long run.
		String cells = Strings.repeat(cellWidth + cellSeparator + separatorWidth + cellSeparator, totalCells - 1);
		cells += cellWidth;

		String finalLayout = prefix + cellSeparator + cells + cellSeparator + suffix;

		return finalLayout;
	}

	private String createCellsWithSeparators(int totalCells) {
		String separatorWidth = "5dlu";
		String cellsWidthPrefixAndSuffix = createCellsWithSeparators(totalCells, separatorWidth, separatorWidth);

		return cellsWidthPrefixAndSuffix;
	}

	protected JLayeredPane getAdditionalOptionPanel() {
		return new JLayeredPane();
	}

}