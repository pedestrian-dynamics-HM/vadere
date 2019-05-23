package org.vadere.gui.components.view;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import org.vadere.gui.components.control.simulation.*;
import org.vadere.gui.components.model.DefaultSimulationConfig;
import org.vadere.gui.components.model.SimulationModel;
import org.vadere.gui.components.utils.Messages;
import org.vadere.gui.components.utils.Resources;
import org.vadere.gui.components.utils.SwingUtils;
import org.vadere.gui.postvisualization.PostVisualisation;
import org.vadere.gui.postvisualization.control.ActionCloseSettingDialog;
import org.vadere.util.logging.Logger;

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;
import java.util.Optional;
import java.util.prefs.Preferences;

import javax.swing.*;

/**
 * @author Benedikt Zoennchen
 */
public class SettingsDialog extends JDialog {
	private static Logger logger = Logger.getLogger(SettingsDialog.class);
	private static Resources resources = Resources.getInstance("global");

	private DefaultSimulationConfig config;
	private List<JButton> targetColorButtons;
	private JComboBox<Integer> jComoboTargetIds;
	private final SimulationModel<? extends DefaultSimulationConfig> model;
	private JLayeredPane colorLayeredPane;

	public SettingsDialog(final SimulationModel<? extends DefaultSimulationConfig> model) {
		this.config = model.config;
		this.model = model;
	}

	public JLayeredPane getColorLayeredPane() {
		return colorLayeredPane;
	}

	public void initComponents() {
		this.setTitle(Messages.getString("SettingsDialog.title"));
		this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		this.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				new ActionCloseSettingDialog(SettingsDialog.this).actionPerformed(null);
			}
		});

		// ################################# Build main structure
		// #############################
		FormLayout mainLayout = new FormLayout("5dlu, [300dlu,pref,600dlu], 5dlu", // col
				"5dlu, pref, 2dlu, pref, 2dlu, pref, 2dlu, pref, 5dlu"); // rows
		CellConstraints cc = new CellConstraints();

		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(mainLayout);
		JScrollPane scrollPane = new JScrollPane(mainPanel);
		scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		getContentPane().add(scrollPane);

		colorLayeredPane = new JLayeredPane();
		JLayeredPane additionalLayeredPane = new JLayeredPane();
		colorLayeredPane
				.setBorder(BorderFactory.createTitledBorder(Messages.getString("SettingsDialog.colors.border.text")));
		mainPanel.add(colorLayeredPane, cc.xy(2, 2));
		additionalLayeredPane.setBorder(
				BorderFactory.createTitledBorder(Messages.getString("SettingsDialog.additional.border.text")));
		mainPanel.add(additionalLayeredPane, cc.xy(2, 4));
		JButton closeButton = new JButton(Messages.getString("SettingsDialog.btnClose.text"));

		JPanel controlPanel = new JPanel();
		controlPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
		controlPanel.add(closeButton);
		closeButton.addActionListener(new ActionCloseSettingDialog(this));
		//mainPanel.add(controlPanel, cc.xyw(2, 6, 2));

		// ######################################################################################

		// Layout definition for sub panels
		FormLayout additionalLayout = new FormLayout("5dlu, pref, 2dlu, pref, 2dlu, pref, 2dlu, pref, 5dlu", // col
				"5dlu, pref, 2dlu, pref, 2dlu, pref, 2dlu, pref, 2dlu, pref, 2dlu, pref, 2dlu, pref, 2dlu, pref, 2dlu, pref, 2dlu, pref, 2dlu, pref, 2dlu, pref, 2dlu, pref, 5dlu"); // rows
		FormLayout colorLayout = new FormLayout("5dlu, pref, 2dlu, pref:grow, 2dlu, pref, 2dlu, pref, 5dlu", // col
				"5dlu, pref, 2dlu, pref, 2dlu, pref, 2dlu, pref, 2dlu, pref, 2dlu, pref, 2dlu, pref, 2dlu, pref, 2dlu, pref, 2dlu, pref, 2dlu, pref, 2dlu, pref, 2dlu, pref, 2dlu, pref, 2dlu, pref, 2dlu, pref, 2dlu, pref, 2dlu, pref, 5dlu"); // rows
		colorLayeredPane.setLayout(colorLayout);
		additionalLayeredPane.setLayout(additionalLayout);

		Integer[] colorIds = new Integer[Integer.parseInt(resources.getProperty("SettingsDialog.maxNumberOfTargets"))];
		for (int index = 1; index <= colorIds.length; index++) {
			colorIds[index - 1] = index;
		}

		jComoboTargetIds = new JComboBox<>(colorIds);
		jComoboTargetIds.setSelectedIndex(0);

		//init();

		JCheckBox chShowObstacles = new JCheckBox((Messages.getString("SettingsDialog.chbShowObstacles.text")));
		JCheckBox chShowTargets = new JCheckBox((Messages.getString("SettingsDialog.chbShowTargets.text")));
		JCheckBox chShowSources = new JCheckBox((Messages.getString("SettingsDialog.chbShowSources.text")));
		JCheckBox chShowAbsorbingAreas = new JCheckBox((Messages.getString("SettingsDialog.chbShowAbsorbingAreas.text")));
		JCheckBox chShowMeasurementAreas = new JCheckBox((Messages.getString("SettingsDialog.chbShowMeasurementAreas.text")));
		JCheckBox chShowStairs = new JCheckBox((Messages.getString("SettingsDialog.chbShowStairs.text")));
		JCheckBox chShowPedIds = new JCheckBox((Messages.getString("SettingsDialog.chbShowPedestrianIds.text")));

		JCheckBox chHideVoronoiDiagram = new JCheckBox((Messages.getString("SettingsDialog.chbHideVoronoiDiagram.text")));

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

		chShowPedIds.setSelected(model.config.isShowPedestrianIds());
		chShowPedIds.addItemListener(e -> {
			model.config.setShowPedestrianIds(!model.config.isShowPedestrianIds());
			model.notifyObservers();
		});

		final JButton bChange = new JButton(Messages.getString("SettingsDialog.btnEditColor.text"));
		final JPanel pPedestrian = new JPanel();
		Optional<Color> c = model.config.getColorByTargetId(1);
		pPedestrian.setBackground(c.orElseGet(() -> model.config.getPedestrianDefaultColor()));
		pPedestrian.setPreferredSize(new Dimension(130, 20));
		bChange.addActionListener(new ActionSetPedestrianColor("Set Pedestrian Color", model, pPedestrian,
				jComoboTargetIds));

		jComoboTargetIds.addActionListener(e -> {
			Optional<Color> c1 = config.getColorByTargetId(jComoboTargetIds.getSelectedIndex() + 1);
			pPedestrian.setBackground(c1.orElseGet(() -> model.config.getPedestrianDefaultColor()));
		});

		colorLayeredPane.add(new JLabel(Messages.getString("SettingsDialog.lblObstacle.text") + ":"), cc.xy(2, 2));
		colorLayeredPane.add(new JLabel(Messages.getString("SettingsDialog.lblTarget.text") + ":"), cc.xy(2, 4));
		colorLayeredPane.add(new JLabel(Messages.getString("SettingsDialog.lblSource.text") + ":"), cc.xy(2, 6));
		colorLayeredPane.add(new JLabel(Messages.getString("SettingsDialog.lblStair.text") + ":"), cc.xy(2, 8));
		colorLayeredPane.add(new JLabel(Messages.getString("SettingsDialog.lblDensityColor.text") + ":"), cc.xy(2, 10));
		colorLayeredPane.add(new JLabel(Messages.getString("SettingsDialog.lblAbsorbingAreaColor.text") + ":"), cc.xy(2, 12));
		colorLayeredPane.add(new JLabel(Messages.getString("SettingsDialog.lblPedestrianNoTarget.text") + ":"),
				cc.xy(2, 20));

		final JButton bObstColor = new JButton(Messages.getString("SettingsDialog.btnEditColor.text"));
		final JPanel pObstacleColor = new JPanel();
		pObstacleColor.setBackground(model.config.getObstacleColor());
		pObstacleColor.setPreferredSize(new Dimension(130, 20));
		bObstColor.addActionListener(new ActionSetObstacleColor("Set Obstacle Color", model, pObstacleColor));
		colorLayeredPane.add(pObstacleColor, cc.xy(4, 2));
		colorLayeredPane.add(bObstColor, cc.xy(6, 2));

		final JButton bTarColor = new JButton(Messages.getString("SettingsDialog.btnEditColor.text"));
		final JPanel pTargetColor = new JPanel();
		pTargetColor.setBackground(model.config.getTargetColor());
		pTargetColor.setPreferredSize(new Dimension(130, 20));
		bTarColor.addActionListener(new ActionSetTargetColor("Set Target Color", model, pTargetColor));
		colorLayeredPane.add(pTargetColor, cc.xy(4, 4));
		colorLayeredPane.add(bTarColor, cc.xy(6, 4));

		final JButton bSrcColor = new JButton(Messages.getString("SettingsDialog.btnEditColor.text"));
		final JPanel pSourceColor = new JPanel();
		pSourceColor.setBackground(model.config.getSourceColor());
		pSourceColor.setPreferredSize(new Dimension(130, 20));
		bSrcColor.addActionListener(new ActionSetSourceColor("Set Source Color", model, pSourceColor));
		colorLayeredPane.add(pSourceColor, cc.xy(4, 6));
		colorLayeredPane.add(bSrcColor, cc.xy(6, 6));

		final JButton bStairsColor = new JButton(Messages.getString("SettingsDialog.btnEditColor.text"));
		final JPanel pStairsColor = new JPanel();
		pStairsColor.setBackground(model.config.getStairColor());
		pStairsColor.setPreferredSize(new Dimension(130, 20));
		bStairsColor.addActionListener(new ActionSetStairsColor("Set Stairs Color", model, pStairsColor));
		colorLayeredPane.add(pStairsColor, cc.xy(4, 8));
		colorLayeredPane.add(bStairsColor, cc.xy(6, 8));

		final JButton bDensityColor = new JButton(Messages.getString("SettingsDialog.btnEditColor.text"));
		final JPanel pDensityColor = new JPanel();
		pDensityColor.setBackground(model.config.getDensityColor());
		pDensityColor.setPreferredSize(new Dimension(130, 20));
		bDensityColor.addActionListener(new ActionSetDensityColor("Set Density Color", model, pDensityColor));
		colorLayeredPane.add(pDensityColor, cc.xy(4, 10));
		colorLayeredPane.add(bDensityColor, cc.xy(6, 10));

		final JButton bAbsorbingAreaColor = new JButton(Messages.getString("SettingsDialog.btnEditColor.text"));
		final JPanel pAbsorbingAreaColor = new JPanel();
		pAbsorbingAreaColor.setBackground(model.config.getAbsorbingAreaColor());
		pAbsorbingAreaColor.setPreferredSize(new Dimension(130, 20));
		bAbsorbingAreaColor.addActionListener(new ActionSetAbsorbingAreaColor("Set Absorbing Area Color", model, pAbsorbingAreaColor));
		colorLayeredPane.add(pAbsorbingAreaColor, cc.xy(4, 12));
		colorLayeredPane.add(bAbsorbingAreaColor, cc.xy(6, 12));

		colorLayeredPane.add(new JSeparator(), cc.xyw(1, 14, 9));
		colorLayeredPane.add(new JLabel(Messages.getString("SettingsDialog.lblPedTrajColor.text") + ":"),
				cc.xyw(2, 16, 5));
		colorLayeredPane.add(jComoboTargetIds, cc.xy(2, 18));
		colorLayeredPane.add(pPedestrian, cc.xy(4, 18));
		colorLayeredPane.add(bChange, cc.xy(6, 18));

		final JButton bPedestrianNoTarget = new JButton(Messages.getString("SettingsDialog.btnEditColor.text"));
		final JPanel pPedestrianNoTarget = new JPanel();
		Optional<Color> notTargetPedCol = config.getColorByTargetId((-1));
		pPedestrianNoTarget.setBackground(notTargetPedCol.orElseGet(() -> model.config.getPedestrianDefaultColor()));
		pPedestrianNoTarget.setPreferredSize(new Dimension(130, 20));
		bPedestrianNoTarget.addActionListener(new ActionSetPedestrianWithoutTargetColor(
				"Set Pedestrian without Target Color", model, pPedestrianNoTarget));
		colorLayeredPane.add(pPedestrianNoTarget, cc.xy(4, 20));
		colorLayeredPane.add(bPedestrianNoTarget, cc.xy(6, 20));

		// 20 is free

		JCheckBox chRandomColors = new JCheckBox(Messages.getString("SettingsDialog.chbUseRandomColors.text"));
		chRandomColors.setSelected(model.config.isUseRandomPedestrianColors());
		chRandomColors.addItemListener(e -> {
			model.config.setUseRandomPedestrianColors(!model.config.isUseRandomPedestrianColors());
			model.notifyObservers();
		});
		colorLayeredPane.add(chRandomColors, cc.xyw(2, 24, 8));


		additionalLayeredPane.add(chHideVoronoiDiagram, cc.xyw(2, 2, 5));
		additionalLayeredPane.add(chShowObstacles, cc.xyw(2, 4, 5));
		additionalLayeredPane.add(chShowTargets, cc.xyw(2, 6, 5));
		additionalLayeredPane.add(chShowSources, cc.xyw(2, 8, 5));
		additionalLayeredPane.add(chShowStairs, cc.xyw(2, 10, 5));
		additionalLayeredPane.add(chShowAbsorbingAreas, cc.xyw(2, 12, 5));
		additionalLayeredPane.add(chShowMeasurementAreas, cc.xyw(2, 14, 5));
		additionalLayeredPane.add(chShowPedIds, cc.xyw(2, 16, 5));

		JCheckBox chChowLogo = new JCheckBox(Messages.getString("SettingsDialog.chbLogo.text"));
		chChowLogo.setSelected(model.config.isShowLogo());
		chChowLogo.addItemListener(e -> {
			model.config.setShowLogo(!model.config.isShowLogo());
			model.notifyObservers();
		});
		additionalLayeredPane.add(chChowLogo, cc.xyw(2, 18, 5));

		additionalLayeredPane.add(new JLabel(Messages.getString("SettingsDialog.lblSnapshotDir.text") + ":"),
				cc.xy(2, 20));

		JTextField tSnapshotDir = new JTextField(
				Preferences.userNodeForPackage(PostVisualisation.class).get("SettingsDialog.snapshotDirectory.path", "."));
		tSnapshotDir.setEditable(false);
		tSnapshotDir.setPreferredSize(new Dimension(130, 20));
		additionalLayeredPane.add(tSnapshotDir, cc.xy(4, 20));
		final JButton bSnapshotDir = new JButton(Messages.getString("SettingsDialog.btnEditSnapshot.text"));
		bSnapshotDir.addActionListener(new ActionSetSnapshotDirectory("Set Snapshot Directory", model, tSnapshotDir));
		additionalLayeredPane.add(bSnapshotDir, cc.xy(6, 22));

		final JSpinner spinnerCellWidth = new JSpinner();
		final SpinnerNumberModel sModelCellWidth = new SpinnerNumberModel(model.config.getGridWidth(),
				model.config.getMinCellWidth(), model.config.getMaxCellWidth(), 0.1);
		spinnerCellWidth.setModel(sModelCellWidth);

		spinnerCellWidth.addChangeListener(e -> {
			model.config.setGridWidth((double) sModelCellWidth.getValue());
			model.notifyObservers();
		});

		additionalLayeredPane.add(new JLabel(Messages.getString("SettingsDialog.lblCellWidth.text") + ":"),
				cc.xy(2, 22));
		additionalLayeredPane.add(spinnerCellWidth, cc.xy(4, 22));

		mainPanel.add(getAdditionalOptionPanel(), cc.xy(2, 6));

		mainPanel.add(controlPanel, cc.xy(2, 8));

		scrollPane.setPreferredSize(new Dimension(mainPanel.getPreferredSize().width+10, Math.min(mainPanel.getPreferredSize().height, Toolkit.getDefaultToolkit().getScreenSize().height - 50)));
		pack();
		setResizable(true);
		SwingUtils.centerComponent(this);
		setVisible(true);
	}

	protected JLayeredPane getAdditionalOptionPanel() {
		return new JLayeredPane();
	}
}
