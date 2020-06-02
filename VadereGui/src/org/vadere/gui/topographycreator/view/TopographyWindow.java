package org.vadere.gui.topographycreator.view;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import org.jetbrains.annotations.NotNull;
import org.vadere.gui.components.control.ActionGenerateMesh;
import org.vadere.gui.components.control.ActionGeneratePoly;
import org.vadere.gui.components.control.IViewportChangeListener;
import org.vadere.gui.components.control.JViewportChangeListener;
import org.vadere.gui.components.control.PanelResizeListener;
import org.vadere.gui.components.control.ViewportChangeListener;
import org.vadere.gui.components.utils.Messages;
import org.vadere.gui.components.utils.Resources;
import org.vadere.gui.components.view.InfoPanel;
import org.vadere.gui.components.view.ScenarioElementView;
import org.vadere.gui.components.view.ScenarioToolBar;
import org.vadere.gui.projectview.control.ActionDeselect;
import org.vadere.gui.projectview.model.ProjectViewModel;
import org.vadere.gui.projectview.view.JsonValidIndicator;
import org.vadere.gui.topographycreator.control.ActionBasic;
import org.vadere.gui.topographycreator.control.ActionCopyElement;
import org.vadere.gui.topographycreator.control.ActionDeleteElement;
import org.vadere.gui.topographycreator.control.ActionInsertCopiedElement;
import org.vadere.gui.topographycreator.control.ActionMaximizeSize;
import org.vadere.gui.topographycreator.control.ActionMergeObstacles;
import org.vadere.gui.topographycreator.control.ActionOpenDrawOptionMenu;
import org.vadere.gui.topographycreator.control.ActionPlaceRandomPedestrians;
import org.vadere.gui.topographycreator.control.ActionQuickSaveTopography;
import org.vadere.gui.topographycreator.control.ActionRedo;
import org.vadere.gui.topographycreator.control.ActionResetTopography;
import org.vadere.gui.topographycreator.control.ActionResizeTopographyBound;
import org.vadere.gui.topographycreator.control.ActionSelectCut;
import org.vadere.gui.topographycreator.control.ActionSelectSelectShape;
import org.vadere.gui.topographycreator.control.ActionSimplifyObstacles;
import org.vadere.gui.topographycreator.control.ActionSubtractMeasurementArea;
import org.vadere.gui.topographycreator.control.ActionSwitchCategory;
import org.vadere.gui.topographycreator.control.ActionSwitchSelectionMode;
import org.vadere.gui.topographycreator.control.ActionTopographyMakroMenu;
import org.vadere.gui.topographycreator.control.ActionTranslateElements;
import org.vadere.gui.topographycreator.control.ActionTranslateTopography;
import org.vadere.gui.topographycreator.control.ActionUndo;
import org.vadere.gui.topographycreator.control.ActionZoomIn;
import org.vadere.gui.topographycreator.control.ActionZoomOut;
import org.vadere.gui.topographycreator.control.DrawConvexHullMode;
import org.vadere.gui.topographycreator.control.DrawDotMode;
import org.vadere.gui.topographycreator.control.DrawRectangleMode;
import org.vadere.gui.topographycreator.control.DrawSimplePolygonMode;
import org.vadere.gui.topographycreator.control.EraserMode;
import org.vadere.gui.topographycreator.control.SelectElementMode;
import org.vadere.gui.topographycreator.control.TopographyAction;
import org.vadere.gui.topographycreator.control.UndoAdaptor;
import org.vadere.gui.topographycreator.model.IDrawPanelModel;
import org.vadere.gui.topographycreator.model.TopographyCreatorModel;
import org.vadere.simulator.projects.Scenario;
import org.vadere.state.types.ScenarioElementType;
import org.vadere.util.config.VadereConfig;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.*;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import javax.swing.undo.UndoManager;
import javax.swing.undo.UndoableEditSupport;

public class TopographyWindow extends JPanel {
	private static final long serialVersionUID = -2472077480081283655L;
	private static Resources resources = Resources.getInstance("topographycreator");
	// private JScrollPane scrollpane;
	private final ScenarioToolBar toolbar;
	private IDrawPanelModel panelModel;
	private InfoPanel infoPanel;
	private TopographyPanel mainPanel;
	private JLabelObserver selectedElementLabel;
	private JTabbedPane tabbedInfoPanel;



	private UndoableEditSupport undoSupport;
	private UndoManager undoManager;

	public TopographyWindow(final Scenario currentScenario, @NotNull final ProjectViewModel model) {

		toolbar = new ScenarioToolBar("Toolbar");
		int toolbarSize = VadereConfig.getConfig().getInt("Gui.toolbar.size");
		Dimension prefSize = new Dimension(toolbarSize, toolbarSize);
		//toolbar.setPreferredSize(prefSize);
		toolbar.setBorderPainted(true);
		toolbar.setFloatable(true);
		toolbar.setAlignmentX(Component.LEFT_ALIGNMENT);
		toolbar.setAlignmentY(Component.TOP_ALIGNMENT);

		// setVisible(true);

		// undo-redo installation
		undoSupport = new UndoableEditSupport();
		undoManager = new UndoManager();
		undoSupport.addUndoableEditListener(new UndoAdaptor(undoManager));

		setTopography(new TopographyCreatorModel(currentScenario), model);
	}

	private static JButton addActionToToolbar(final JToolBar toolbar, final Action action,
											  final String toolTipProperty) {
		JButton button = toolbar.add(action);
		button.setBorderPainted(false);
		button.setToolTipText(Messages.getString(toolTipProperty));
		return button;
	}

	private static JButton addActionToToolbar(final JToolBar toolbar, final Action action, final String toolTipProperty,
											  final JButton button) {
		button.setAction(action);
		button.setText("");
		toolbar.add(button);
		button.setBorderPainted(false);
		button.setToolTipText(Messages.getString(toolTipProperty));
		return button;
	}

	private void setTopography(final TopographyCreatorModel panelModel, @NotNull final ProjectViewModel model) {

		this.panelModel = panelModel;
		this.panelModel.setMouseSelectionMode(new SelectElementMode(panelModel, undoSupport));
		// info panel
		infoPanel = new InfoPanel(panelModel);
		selectedElementLabel = new JLabelObserver(JLabelObserver.DEFAULT_TEXT);

		JsonValidIndicator jsonValidIndicator = new JsonValidIndicator();
		ScenarioElementView scenarioElementView = new ScenarioElementView(panelModel, jsonValidIndicator, selectedElementLabel);
		TopographyTreeView topographyTreeView = new TopographyTreeView(panelModel);

		final JPanel thisPanel = this;

		// TabbedPane
		tabbedInfoPanel = new JTabbedPane(SwingConstants.TOP);
		tabbedInfoPanel.addTab("SelectedElment", scenarioElementView);
		tabbedInfoPanel.addTab("ElementTree", topographyTreeView);
		tabbedInfoPanel.addChangeListener(e ->{
			int index = tabbedInfoPanel.getSelectedIndex();
			if (index == 1){
				topographyTreeView.update(null, null);
			}

		});


		// 1. get data from the user screen
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		int windowHeight = screenSize.height - 250;
		int windowWidth = screenSize.width - 250;

		/* basic action */
		final TopographyAction basicAction = new ActionBasic("notify", panelModel);

		Action zoomInAction = new ActionZoomIn("zoom in", new ImageIcon(Resources.class
				.getResource("/icons/zoom_in_icon.png")), panelModel);
		Action zoomOutAction = new ActionZoomOut("zoom out", new ImageIcon(Resources.class
				.getResource("/icons/zoom_out_icon.png")), panelModel);
		/*
		 * Action scrollAction = new ActionScroll("scroll", new ImageIcon(Resources.class
		 * .getResource("/icons/scroll_icon.png")), panelModel);
		 */
		Action selectCutAction = new ActionSelectCut("select zoom", new ImageIcon(Resources.class
				.getResource("/icons/zoom_icon.png")), panelModel, undoSupport);

		Action maximizeAction = new ActionMaximizeSize("maximize", new ImageIcon(Resources.class
				.getResource("/icons/maximize_icon.png")), panelModel);
		/*
		 * Action minimizeAction = new ActionMinimizeSize("minimize", new
		 * ImageIcon(Resources.class
		 * .getResource("/icons/minimize_icon.png")), panelModel);
		 */
		Action resetScenarioAction = new ActionResetTopography("reset scenario", new ImageIcon(Resources.class
				.getResource("/icons/reset_scenario_icon.png")), panelModel, undoSupport);
		Action saveScenarioAction = new ActionQuickSaveTopography("save scenario", new ImageIcon(Resources.class
				.getResource("/icons/save_icon.png")), panelModel);

		Action undoAction = new ActionUndo("undo", new ImageIcon(Resources.class
				.getResource("/icons/undo_icon.png")), undoManager, basicAction);
		Action redoAction = new ActionRedo("redo", new ImageIcon(Resources.class
				.getResource("/icons/redo_icon.png")), undoManager, basicAction);

		Action mergeObstaclesAction = new ActionMergeObstacles("mergeObstacles", new ImageIcon(Resources.class
				.getResource("/icons/merge.png")), panelModel, undoSupport);

		FormLayout layout = new FormLayout("2dlu, default:grow(0.75), 2dlu, default:grow(0.25), 2dlu", // col
				"2dlu, default, 2dlu, default, 2dlu, default, 2dlu"); // rows
		thisPanel.setLayout(layout);


		CellConstraints cc = new CellConstraints();

		// construct the scrollpanel
		JScrollPane scrollPane = new JScrollPane();
		scrollPane.getViewport()
				.addChangeListener(new JViewportChangeListener(panelModel, scrollPane.getVerticalScrollBar()));
		scrollPane.setPreferredSize(new Dimension(1, windowHeight));
		IViewportChangeListener viewportChangeListener = new ViewportChangeListener(panelModel, scrollPane);
		panelModel.addViewportChangeListener(viewportChangeListener);
		panelModel.addScrollPane(scrollPane);

		mainPanel = new TopographyPanel(panelModel, new TopographyCreatorRenderer(panelModel), scrollPane);
		mainPanel.addComponentListener(new PanelResizeListener(panelModel));
		mainPanel.setBorder(BorderFactory.createLineBorder(Color.red));
		panelModel.addScaleChangeListener(mainPanel);
		panelModel.addObserver(mainPanel);
		scrollPane.setViewportView(mainPanel);

		selectedElementLabel.setPanelModel(panelModel);

		panelModel.addObserver(infoPanel);
		panelModel.addObserver(topographyTreeView);
		scenarioElementView.setPreferredSize(new Dimension(1, windowHeight));
		topographyTreeView.setPreferredSize(new Dimension(1, windowHeight));

		panelModel.addObserver(selectedElementLabel);

		scrollPane.setMinimumSize(new Dimension(1, 1));
		scenarioElementView.setMinimumSize(new Dimension(1, 1));
		topographyTreeView.setMinimumSize(new Dimension(1, 1));
		JSplitPane splitPane = new JSplitPane();
		((BasicSplitPaneUI) splitPane.getUI()).getDivider().addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if(e.getClickCount() == 2){
					int newLocation = splitPane.getSize().width - (scenarioElementView.textWidth() + 65); // 65 magic number to take gutter into account
					if (newLocation > 0)
						splitPane.setDividerLocation(newLocation);
				}
			}
		});

		splitPane.setResizeWeight(0.8);
		splitPane.resetToPreferredSizes();
		splitPane.setLeftComponent(scrollPane);
		splitPane.setRightComponent(tabbedInfoPanel);

		thisPanel.add(toolbar, cc.xyw(2, 2, 4));
		thisPanel.add(splitPane, cc.xyw(2, 4, 4));
		thisPanel.add(infoPanel, cc.xyw(2, 6, 4));

		/* close dialog action */

		/* select dot action */
		TopographyAction selectDotModeAction = new ActionSwitchSelectionMode("dot selection mode", panelModel,
				new DrawDotMode(panelModel, undoSupport), basicAction);

		/* select rect action */
		TopographyAction selectRectangleAction =
				new ActionSwitchSelectionMode("rect selection mode", panelModel,
						new DrawRectangleMode(panelModel, undoSupport), basicAction);

		/* switch category to obstacle action */
		TopographyAction switchToObstacleAction = new ActionSwitchCategory("switch to obstacle", panelModel,
				ScenarioElementType.OBSTACLE, selectRectangleAction);

		/* switch category to target action */
		TopographyAction switchToTargetAction = new ActionSwitchCategory("switch to targets", panelModel,
				ScenarioElementType.TARGET, selectRectangleAction);

		/* switch category to target changer action */
		TopographyAction switchToTargetChangerAction = new ActionSwitchCategory("switch to target changer", panelModel,
				ScenarioElementType.TARGET_CHANGER, selectRectangleAction);

		/* switch category to absorbing areas action */
		TopographyAction switchToAbsorbingAreaAction = new ActionSwitchCategory("switch to absorbing areas", panelModel,
				ScenarioElementType.ABSORBING_AREA, selectRectangleAction);

		/* switch category to stairs action */
		TopographyAction switchToStairsAction = new ActionSwitchCategory("switch to stairs", panelModel,
				ScenarioElementType.STAIRS, selectRectangleAction);

		/* switch category to measurement area action */
		TopographyAction switchToMeasurementAreaAction = new ActionSwitchCategory(
				"switch to measurement area", panelModel,
				ScenarioElementType.MEASUREMENT_AREA, selectRectangleAction);

		/* subtract obstacles from measurementArea */
		Action subtractMeasurementAreaAction = new ActionSubtractMeasurementArea(Messages.getString("TopographyCreator.btnSubtractMeasurementArea.label"), new ImageIcon(Resources.class
				.getResource("/icons/subtract.png")), panelModel, undoSupport);

		/* Place Random Pedestrians */
		Action placeRandomPedestrians = new ActionPlaceRandomPedestrians(Messages.getString(
				"TopographyCreator.PlaceRandomPedestrians.label"), new ImageIcon(Resources.class
				.getResource("/icons/pedestrians_rnd_icon.png")), panelModel, undoSupport);

		/* list of actions for the sub-dialog */
		Action pen = new ActionSwitchSelectionMode(
				Messages.getString("TopographyCreator.btnConvexHull.label"), new ImageIcon(Resources.class
				.getResource("/icons/convexHull.png")), panelModel, new DrawConvexHullMode(panelModel,
				undoSupport),
				basicAction);
		Action pen2 = new ActionSwitchSelectionMode(Messages.getString("TopographyCreator.btnSimplePolygon.label"), new ImageIcon(Resources.class
				.getResource("/icons/simplePolygon.png")), panelModel, new DrawSimplePolygonMode(panelModel,
				undoSupport),
				basicAction);
		Action rectangle = new ActionSwitchSelectionMode(Messages.getString("TopographyCreator.btnRectangle.label"), new ImageIcon(Resources.class
				.getResource("/icons/paint_method_rectangle_icon.png")), panelModel, new DrawRectangleMode(
				panelModel, undoSupport),
				basicAction);
		Action dot = new ActionSwitchSelectionMode(Messages.getString("TopographyCreator.btnPedestrian.label"), new ImageIcon(Resources.class
				.getResource("/icons/paint_method_dot_icon.png")), panelModel, new DrawDotMode(panelModel,
				undoSupport),
				basicAction);

		List<Action> obstacleAndTargetDrawModes = new ArrayList<>();
		List<Action> sourceDrawModes = new ArrayList<>();
		List<Action> absorbingAreaDrawModes = new ArrayList<>();
		List<Action> measurementAreaDrawModes = new ArrayList<>();
		List<Action> measurementAreaMiscActions = new ArrayList<>();
		List<Action> pedestrianDrawModes = new ArrayList<>();
		List<Action> pedestrianMiscActions = new ArrayList<>();

		obstacleAndTargetDrawModes.add(rectangle);
		obstacleAndTargetDrawModes.add(pen);
		obstacleAndTargetDrawModes.add(pen2);

		sourceDrawModes.add(rectangle);
		sourceDrawModes.add(pen);
		sourceDrawModes.add(pen2);
		sourceDrawModes.add(dot);

		measurementAreaDrawModes.add(rectangle);
		measurementAreaDrawModes.add(pen);
		measurementAreaDrawModes.add(pen2);
		measurementAreaMiscActions.add(subtractMeasurementAreaAction);

		absorbingAreaDrawModes.add(rectangle);
		absorbingAreaDrawModes.add(pen);
		absorbingAreaDrawModes.add(pen2);

		pedestrianDrawModes.add(dot);
		pedestrianMiscActions.add(placeRandomPedestrians);

		/* open obstacle paint method dialog action */
		JButton obsButton = new JButton();
		TopographyAction openObstacleDialog = new ActionOpenDrawOptionMenu(
				"Obstacle", new ImageIcon(
				Resources.class.getResource("/icons/obstacle_icon.png")), panelModel, switchToObstacleAction,
				obsButton, obstacleAndTargetDrawModes);

		/* open target paint method dialog action */
		JButton targetButton = new JButton();
		TopographyAction openTargetDialog = new ActionOpenDrawOptionMenu("Target", new ImageIcon(Resources.class
				.getResource("/icons/target_icon.png")), panelModel, switchToTargetAction, targetButton,
				obstacleAndTargetDrawModes);

		JButton targetChangerButton = new JButton();
		TopographyAction openTargetChangerDialog = new ActionOpenDrawOptionMenu("TargetChanger", new ImageIcon(Resources.class
				.getResource("/icons/target_changer_icon.png")), panelModel, switchToTargetChangerAction, targetChangerButton,
				obstacleAndTargetDrawModes);

		/* open absorbing area paint method dialog action */
		JButton absorbingAreaButton = new JButton();
		TopographyAction openAbsorbingAreaDialog = new ActionOpenDrawOptionMenu("AbsorbingArea", new ImageIcon(Resources.class
				.getResource("/icons/emergency_exit.png")), panelModel, switchToAbsorbingAreaAction, absorbingAreaButton,
				absorbingAreaDrawModes);

		/* open stairs paint method dialog action */
		JButton stairsButton = new JButton();
		TopographyAction openStairsDialog = new ActionOpenDrawOptionMenu("Stairs", new ImageIcon(Resources.class
				.getResource("/icons/stairs_icon.png")), panelModel, switchToStairsAction, stairsButton,
				obstacleAndTargetDrawModes);

		/* open measurement area paint method dialog action*/
		JButton measurementAreaButton = new JButton();
		TopographyAction openMeasurementAreaDialog = new ActionOpenDrawOptionMenu( "MeasurementArea",
				new ImageIcon(Resources.class.getResource("/icons/measurement_area_icon.png")),
				panelModel, switchToMeasurementAreaAction, measurementAreaButton, measurementAreaDrawModes, measurementAreaMiscActions);


		/* pedestrians */
		TopographyAction switchToPedestrianAction = new ActionSwitchCategory("switch to pedestrian", panelModel,
				ScenarioElementType.PEDESTRIAN, selectDotModeAction);

		JButton pedestrianButton = new JButton();
		TopographyAction openPedestrianDialog = new ActionOpenDrawOptionMenu("Pedestrian", new ImageIcon(Resources.class
				.getResource("/icons/pedestrians_icon.png")), panelModel, switchToPedestrianAction, pedestrianButton,
				pedestrianDrawModes, pedestrianMiscActions);

		/* switch category to source action */
		TopographyAction switchToSourceAction = new ActionSwitchCategory("switch to source", panelModel,
				ScenarioElementType.SOURCE, selectDotModeAction);

		/* source */
		JButton sourceButton = new JButton();
		TopographyAction openSourceDialog = new ActionOpenDrawOptionMenu("Source", new ImageIcon(Resources.class
				.getResource("/icons/source_icon.png")), panelModel, switchToSourceAction, sourceButton,
				sourceDrawModes);

		ActionSelectSelectShape selectShape = new ActionSelectSelectShape("select shape mode", new ImageIcon(
				Resources.class.getResource("/icons/select_shapes_icon.png")), panelModel, undoSupport);

		/* resize Topography */
		TopographyAction resizeTopographyBound = new ActionResizeTopographyBound(Messages.getString("TopographyBoundDialog.tooltip"),
				new ImageIcon(Resources.class.getResource("/icons/topography_icon.png")),
				panelModel, selectShape, undoSupport);

		TopographyAction simplifyObstacle = new ActionSimplifyObstacles("Simplify",
				new ImageIcon(Resources.class.getResource("/icons/merge_convex.png")),
				panelModel, selectShape, undoSupport);

		TopographyAction translateTopography =new ActionTranslateTopography("TranslateTopography",
				new ImageIcon(Resources.class.getResource("/icons/translation_icon.png")),
				panelModel, selectShape, undoSupport);

		TopographyAction translateElements =new ActionTranslateElements("TranslateElements",
				new ImageIcon(Resources.class.getResource("/icons/translation_elements_icon.png")),
				panelModel, selectShape, undoSupport);

		/* Makros */
		ActionTopographyMakroMenu actionTopographyMakroMenu =
				new ActionTopographyMakroMenu("TopographyMakros",
						new ImageIcon(Resources.class.getResource("/icons/auto_generate_ids.png")),
						panelModel);

		int iconHeight = VadereConfig.getConfig().getInt("ProjectView.icon.height.value");
		int iconWidth = VadereConfig.getConfig().getInt("ProjectView.icon.width.value");
		AbstractAction polyImg = new ActionGeneratePoly(Messages.getString("ProjectView.btnPolySnapshot.tooltip"),
				resources.getIcon("camera_poly.png", iconWidth, iconHeight),
				panelModel);

		AbstractAction generateMesh = new ActionGenerateMesh(Messages.getString("ProjectView.btnGenerateMesh.tooltip"),
				resources.getIcon("generate_mesh.png", iconWidth, iconHeight),
				model);


		/* create toolbar*/
		addActionToToolbar(toolbar, selectShape, "select_shape_tooltip");
		addActionToToolbar(
				toolbar,
				new ActionSwitchSelectionMode("erase mode", new ImageIcon(Resources.class
						.getResource("/icons/eraser_icon.png")), panelModel, new EraserMode(panelModel,
						undoSupport),
						basicAction),
				"TopographyCreator.btnErase.tooltip");
		toolbar.addSeparator(new Dimension(5, 50));
		addActionToToolbar(toolbar, openSourceDialog, "TopographyCreator.btnInsertSource.tooltip",
				sourceButton);
		addActionToToolbar(toolbar, openTargetDialog, "TopographyCreator.btnInsertTarget.tooltip",
				targetButton);
		toolbar.addSeparator(new Dimension(5, 50));
		addActionToToolbar(toolbar, openTargetChangerDialog, "TopographyCreator.btnInsertTargetChanger.tooltip",
				targetChangerButton);
		addActionToToolbar(toolbar, openObstacleDialog, "TopographyCreator.btnInsertObstacle.tooltip",
				obsButton);
		addActionToToolbar(toolbar, openAbsorbingAreaDialog, "TopographyCreator.btnInsertAbsorbingArea.tooltip",
				absorbingAreaButton);
		addActionToToolbar(toolbar, openPedestrianDialog, "TopographyCreator.btnInsertPedestrian.tooltip", pedestrianButton);
		addActionToToolbar(toolbar, openStairsDialog, "TopographyCreator.btnInsertStairs.tooltip",
				stairsButton);
		addActionToToolbar(toolbar, openMeasurementAreaDialog,
				"TopographyCreator.btnInsertMeasurementArea.tooltip",
				measurementAreaButton);
		toolbar.addSeparator(new Dimension(5, 50));
		addActionToToolbar(toolbar, mergeObstaclesAction, "TopographyCreator.btnMergeObstacles.tooltip");
		// addActionToToolbar(toolbar, scrollAction, "TopographyCreator.btnScroll.tooltip");
		addActionToToolbar(toolbar, zoomInAction, "TopographyCreator.btnZoomIn.tooltip");
		addActionToToolbar(toolbar, zoomOutAction, "TopographyCreator.btnZoomOut.tooltip");
		// addActionToToolbar(toolbar, minimizeAction,
		// "TopographyCreator.btnMinimizeTopography.tooltip");
		addActionToToolbar(toolbar, maximizeAction, "TopographyCreator.btnMaximizeTopography.tooltip");
		addActionToToolbar(toolbar, resizeTopographyBound, "TopographyCreator.btnTopographyBound.tooltip");
		addActionToToolbar(toolbar, simplifyObstacle, "TopographyCreator.btnSimplifyObstacle.tooltip");
		addActionToToolbar(toolbar, translateTopography, "TopographyCreator.btnTranslation.tooltip");
		addActionToToolbar(toolbar, translateElements, "TopographyCreator.btnElementTranslation.tooltip");
		toolbar.addSeparator(new Dimension(5, 50));
		addActionToToolbar(toolbar, selectCutAction, "TopographyCreator.btnCutTopography.tooltip");
		addActionToToolbar(toolbar, resetScenarioAction, "TopographyCreator.btnNewTopography.tooltip");
		addActionToToolbar(toolbar, saveScenarioAction, "TopographyCreator.btnQuickSave.tooltip");
		addActionToToolbar(toolbar, polyImg, "TopographyCreator.btnGeneratePoly.tooltip");
		addActionToToolbar(toolbar, generateMesh, "TopographyCreator.btnGenerateMesh.tooltip");

		toolbar.addSeparator(new Dimension(5, 50));
		addActionToToolbar(toolbar, undoAction, "TopographyCreator.btnUndo.tooltip");
		addActionToToolbar(toolbar, redoAction, "TopographyCreator.btnRedo.tooltip");
		toolbar.add(Box.createHorizontalGlue());
		addActionToToolbar(toolbar, actionTopographyMakroMenu, "TopographyCreator.btnGenerateIds.tooltip");

		mainPanel.setBorder(BorderFactory.createLineBorder(Color.red));

		// copy element
		TopographyAction copyElementAction = new ActionCopyElement("copy element", panelModel);
		getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
				KeyStroke.getKeyStroke(KeyEvent.VK_C, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()),
				"copy-element");
		getActionMap().put("copy-element", copyElementAction);

		TopographyAction insertCopiedElementAction =
				new ActionInsertCopiedElement("insertVertex copied element", panelModel, undoSupport);
		getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
				KeyStroke.getKeyStroke(KeyEvent.VK_V, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()),
				"insertVertex-copied-element");
		getActionMap().put("insertVertex-copied-element", insertCopiedElementAction);

		// delete element
		TopographyAction deleteElement =
				new ActionDeleteElement("delete element", panelModel, undoSupport, basicAction);
		getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0),
				"delete-element");
		getActionMap().put("delete-element", deleteElement);

		// undo
		getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
				KeyStroke.getKeyStroke(KeyEvent.VK_Z, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()),
				"undo");
		getActionMap().put("undo", undoAction);

		// redo
		getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
				KeyStroke.getKeyStroke(KeyEvent.VK_Z,
						Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() | KeyEvent.SHIFT_DOWN_MASK),
				"redo");
		getActionMap().put("redo", redoAction);

		// deselect selected element on esc
		getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("ESCAPE"), "deselect");
		getActionMap().put("deselect", new ActionDeselect(panelModel, thisPanel, selectShape));

	}

	public IDrawPanelModel getPanelModel() {
		return panelModel;
	}
}
