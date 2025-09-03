package org.vadere.gui.topographycreator.view;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import org.jetbrains.annotations.NotNull;
import org.vadere.gui.components.control.*;
import org.vadere.gui.components.utils.Localization;
import org.vadere.gui.components.utils.ResourceStrings;
import org.vadere.gui.components.utils.Resources;
import org.vadere.gui.components.view.InfoPanel;
import org.vadere.gui.components.view.ScenarioElementView;
import org.vadere.gui.components.view.ScenarioToolBar;
import org.vadere.gui.components.view.ScenarioToolBarSection;
import org.vadere.gui.projectview.control.ActionDeselect;
import org.vadere.gui.projectview.model.ProjectViewModel;
import org.vadere.gui.projectview.view.JsonValidIndicator;
import org.vadere.gui.topographycreator.control.*;
import org.vadere.gui.topographycreator.control.attribtable.ui.AttributeTableContainer;
import org.vadere.gui.topographycreator.model.IDrawPanelModel;
import org.vadere.gui.topographycreator.model.TopographyCreatorModel;
import org.vadere.simulator.projects.Scenario;
import org.vadere.state.types.ScenarioElementType;
import org.vadere.util.config.VadereConfig;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import javax.swing.undo.UndoManager;
import javax.swing.undo.UndoableEditSupport;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

public class TopographyWindow extends JPanel {
	private static final long serialVersionUID = -2472077480081283655L;

	private static final Resources CREATOR_RESOURCES = Resources.getInstance("topographycreator");
	public static final String ICONS_PEDESTRIANS_RND_ICON_PNG = "/icons/pedestrians_rnd_icon.png";
	public static final String ICONS_SUBTRACT_PNG = "/icons/subtract.png";

	private final ScenarioToolBar toolbar;
	private IDrawPanelModel panelModel;
	private InfoPanel infoPanel;
	private TopographyPanel mainPanel;
	private JLabelObserver selectedElementLabel;
	private JTabbedPane tabbedInfoPanel;


	private final UndoableEditSupport undoSupport;
	private final UndoManager undoManager;

	private static final int ICON_SIZE = (int)(VadereConfig.getConfig().getInt("ProjectView.icon.height.value")*VadereConfig.getConfig().getFloat("Gui.scale"));

	private static final int ICON_SIZE_ERASOR = (int)(1.5*VadereConfig.getConfig().getInt("ProjectView.icon.height.value")*VadereConfig.getConfig().getFloat("Gui.scale"));
	public TopographyWindow(final Scenario currentScenario, @NotNull final ProjectViewModel model) {
		toolbar = new ScenarioToolBar("Toolbar");
		// undo-redo installation
		undoSupport = new UndoableEditSupport();
		undoManager = new UndoManager();
		undoSupport.addUndoableEditListener(new UndoAdaptor(undoManager));

		setTopography(new TopographyCreatorModel(currentScenario), model);
	}

	private void setTopography(final TopographyCreatorModel panelModel, @NotNull final ProjectViewModel model) {

		this.panelModel = panelModel;
		this.panelModel.setMouseSelectionMode(new SelectElementMode(panelModel, undoSupport));
		// info panel
		infoPanel = new InfoPanel(panelModel);
		selectedElementLabel = new JLabelObserver(JLabelObserver.DEFAULT_TEXT);

		JsonValidIndicator jsonValidIndicator = new JsonValidIndicator();
		ScenarioElementView scenarioElementView = new ScenarioElementView(panelModel, jsonValidIndicator, selectedElementLabel);
		AttributeTableContainer attributeTableContainer = new AttributeTableContainer(panelModel);
		TopographyTreeView topographyTreeView = new TopographyTreeView(panelModel);

		panelModel.addObserver(scenarioElementView);
		final JPanel thisPanel = this;

		// TabbedPane
		tabbedInfoPanel = new JTabbedPane(SwingConstants.TOP);
		tabbedInfoPanel.addTab("SelectedElement", scenarioElementView);
		tabbedInfoPanel.addTab("Attribute Table", attributeTableContainer);
		tabbedInfoPanel.addTab("ElementTree", topographyTreeView);
		tabbedInfoPanel.addChangeListener(e ->{
			int index = tabbedInfoPanel.getSelectedIndex();
			if (index == tabbedInfoPanel.indexOfComponent(scenarioElementView)){
				topographyTreeView.update(null, null);
			}

		});


		// 1. get data from the user screen
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		int windowHeight = screenSize.height - 250;
		int windowWidth = screenSize.width - 250;

		/* basic action */
		final TopographyAction basicAction = new ActionBasic("notify", panelModel);

		Action zoomInAction = new ActionZoomIn(panelModel);
		Action zoomOutAction = new ActionZoomOut(panelModel);
		Action zoomFit = new ActionZoomFit(panelModel);
		Action selectCutAction = new ActionSelectCut(panelModel,undoSupport);

		Action resetScenarioAction = new ActionResetTopography(panelModel,undoSupport);
		Action saveScenarioAction = new ActionQuickSaveTopography(panelModel);

		Action undoAction = new ActionUndo(
				undoManager,
				basicAction);
		Action redoAction = new ActionRedo(undoManager,basicAction);
		Action mergeObstaclesAction = new ActionMergeObstacles(panelModel, undoSupport);

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
		Action subtractMeasurementAreaAction = new ActionSubtractMeasurementArea(Localization.getString("TopographyCreator.btnSubtractMeasurementArea.label"), ICONS_SUBTRACT_PNG, panelModel, undoSupport);

		/* Place Random Pedestrians */
		Action placeRandomPedestrians = new ActionPlaceRandomPedestrians(Localization.getString(
				"TopographyCreator.PlaceRandomPedestrians.label"), ICONS_PEDESTRIANS_RND_ICON_PNG, panelModel, undoSupport);

		/* list of actions for the sub-dialog */
		Action pen = new ActionSwitchSelectionMode(
				Localization.getString("TopographyCreator.btnConvexHull.label"),
				"convex_hull",
				"",
				panelModel, new DrawConvexHullMode(panelModel,
				undoSupport),
				basicAction);
		Action pen2 = new ActionSwitchSelectionMode(
				Localization.getString("TopographyCreator.btnSimplePolygon.label"),
				"simple_polygon","", panelModel, new DrawSimplePolygonMode(panelModel,
				undoSupport),
				basicAction);
		Action rectangle = new ActionSwitchSelectionMode(
				Localization.getString("TopographyCreator.btnRectangle.label"),
				"paint_rectangle","", panelModel, new DrawRectangleMode(
				panelModel, undoSupport),
				basicAction);
		Action dot = new ActionSwitchSelectionMode(
				Localization.getString("TopographyCreator.btnCircle.label"),
				"paint_circle","", panelModel, new DrawDotMode(panelModel,
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
		TopographyAction openObstacleDialog = new ActionOpenDrawOptionMenu(
				"Obstacle",
				ResourceStrings.ICONS_OBSTACLE_ICON_PNG,
				ResourceStrings.TOPOGRAPHY_CREATOR_BTN_INSERT_OBSTACLE_TOOLTIP,
				panelModel,
				switchToObstacleAction,
				obstacleAndTargetDrawModes);

		/* open target paint method dialog action */
		TopographyAction openTargetDialog = new ActionOpenDrawOptionMenu(
				"Target",
				ResourceStrings.ICONS_TARGET_ICON_PNG,
				ResourceStrings.TOPOGRAPHY_CREATOR_BTN_INSERT_TARGET_TOOLTIP,
				panelModel,
				switchToTargetAction,
				obstacleAndTargetDrawModes);

		TopographyAction openTargetChangerDialog = new ActionOpenDrawOptionMenu(
				"TargetChanger",
				ResourceStrings.ICONS_TARGET_CHANGER_ICON_PNG,
				ResourceStrings.TOPOGRAPHY_CREATOR_BTN_INSERT_TARGET_CHANGER_TOOLTIP
				,panelModel, switchToTargetChangerAction,
				obstacleAndTargetDrawModes);

		/* open absorbing area paint method dialog action */
		TopographyAction openAbsorbingAreaDialog = new ActionOpenDrawOptionMenu(
				"AbsorbingArea",
				ResourceStrings.ICONS_EMERGENCY_EXIT_PNG,
				ResourceStrings.TOPOGRAPHY_CREATOR_BTN_INSERT_ABSORBING_AREA_TOOLTIP,
				panelModel,
				switchToAbsorbingAreaAction,
				absorbingAreaDrawModes);

		/* open stairs paint method dialog action */
		TopographyAction openStairsDialog = new ActionOpenDrawOptionMenu(
				"Stairs",
				ResourceStrings.ICONS_STAIRS_ICON_PNG,
				ResourceStrings.TOPOGRAPHY_CREATOR_BTN_INSERT_STAIRS_TOOLTIP,
				panelModel,
				switchToStairsAction,
				obstacleAndTargetDrawModes);

		/* open measurement area paint method dialog action*/
		TopographyAction openMeasurementAreaDialog = new ActionOpenDrawOptionMenu(
				"MeasurementArea",
				ResourceStrings.ICONS_MEASUREMENT_AREA_ICON_PNG,
				ResourceStrings.TOPOGRAPHY_CREATOR_BTN_INSERT_MEASUREMENT_AREA_TOOLTIP,
				panelModel,
				switchToMeasurementAreaAction,
				measurementAreaDrawModes,
				measurementAreaMiscActions);


		/* pedestrians */
		TopographyAction switchToPedestrianAction = new ActionSwitchCategory("switch to pedestrian", panelModel,
				ScenarioElementType.PEDESTRIAN, selectDotModeAction);

		TopographyAction openPedestrianDialog = new ActionOpenDrawOptionMenu(
				"Pedestrian",
				ResourceStrings.ICONS_PEDESTRIANS_ICON_PNG,
				ResourceStrings.TOPOGRAPHY_CREATOR_BTN_INSERT_PEDESTRIAN_TOOLTIP,
				panelModel,
				switchToPedestrianAction,
				pedestrianDrawModes,
				pedestrianMiscActions);

		/* switch category to source action */
		TopographyAction switchToSourceAction = new ActionSwitchCategory("switch to source", panelModel,
				ScenarioElementType.SOURCE, selectDotModeAction);

		/* source */
		TopographyAction openSourceDialog = new ActionOpenDrawOptionMenu(
				"Source",
				ResourceStrings.ICONS_SOURCE_ICON_PNG,
				ResourceStrings.TOPOGRAPHY_CREATOR_BTN_INSERT_SOURCE_TOOLTIP,
				panelModel,
				switchToSourceAction,
				sourceDrawModes);

		ActionSelectSelectShape selectShape = new ActionSelectSelectShape(
				panelModel,
				undoSupport);

		/* resize Topography */
		TopographyAction resizeTopographyBound = new ActionResizeTopographyBound(
				panelModel, selectShape, undoSupport);

		TopographyAction simplifyObstacle = new ActionSimplifyObstacles(
				panelModel, selectShape, undoSupport);

		TopographyAction translateTopography =new ActionTranslateTopography(
				panelModel, selectShape, undoSupport);

		TopographyAction translateElements =new ActionTranslateElements(
				panelModel, selectShape, undoSupport);

		/* Makros */
		ActionTopographyMakroMenu actionTopographyMakroMenu =
				new ActionTopographyMakroMenu(
						"TopographyMakros",
						ResourceStrings.ICONS_AUTO_GENERATE_IDS_PNG,
						ResourceStrings.TOPOGRAPHY_CREATOR_BTN_GENERATE_IDS_TOOLTIP,
						panelModel);
		

		AbstractAction polyImg = new ActionGeneratePoly(panelModel);

		AbstractAction generateMesh = new ActionGenerateMesh(model);

		AbstractAction eraseMode = new ActionSwitchSelectionMode(
				"erase mode"
				,Resources.getInstance("global").getIconSVG(ResourceStrings.ICONS_ERASER_ICON_PNG,ICON_SIZE_ERASOR,ICON_SIZE_ERASOR,Color.lightGray)
				,ResourceStrings.TOPOGRAPHY_CREATOR_BTN_ERASE_TOOLTIP,
				panelModel,
				new EraserMode(panelModel, undoSupport),
				basicAction);

		// overlay toolbar
		JPanel pane = new JPanel(){
			@Override
			public boolean isOptimizedDrawingEnabled() {
				return false;
			}
		};
		pane.setLayout(new OverlayLayout(pane));
		var overlayToolBar = new ScenarioToolBar("");
		overlayToolBar.setOrientation(SwingConstants.VERTICAL);
		overlayToolBar.addSections( new ScenarioToolBarSection(selectShape,eraseMode),new ScenarioToolBarSection(zoomInAction,zoomOutAction,zoomFit));
		var color = UIManager.getColor("Component.borderColor");
		var border = new LineBorder(color,1);
		overlayToolBar.setBorder(border);
		overlayToolBar.setOpaque(false);
		pane.add(overlayToolBar);
		overlayToolBar.setAlignmentX(0.99f);
		overlayToolBar.setAlignmentY(0.05f);

		pane.add(scrollPane);


		splitPane.setLeftComponent(pane);


		var sectionB = new ScenarioToolBarSection(openSourceDialog,openTargetDialog);
		var sectionC = new ScenarioToolBarSection(
				openTargetChangerDialog,
				openAbsorbingAreaDialog,
				openMeasurementAreaDialog,
				openObstacleDialog,
				openStairsDialog,
				openPedestrianDialog
				);
		var sectionD = new ScenarioToolBarSection(
				mergeObstaclesAction,
				simplifyObstacle,
				resizeTopographyBound,
				translateTopography,
				translateElements,
				actionTopographyMakroMenu
		);
		var sectionE = new ScenarioToolBarSection(
				resetScenarioAction,
				saveScenarioAction,
				selectCutAction
		);
		var sectionF = new ScenarioToolBarSection(
				undoAction,
				redoAction
		);
		var sectionA = new ScenarioToolBarSection(
				polyImg,
				generateMesh
		);
		toolbar.addSections(sectionE,sectionF,sectionA,sectionB,sectionC,sectionD);

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
