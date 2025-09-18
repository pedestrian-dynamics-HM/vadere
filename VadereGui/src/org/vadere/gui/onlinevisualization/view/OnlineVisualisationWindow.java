package org.vadere.gui.onlinevisualization.view;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import org.apache.commons.configuration2.Configuration;
import org.vadere.gui.components.control.*;
import org.vadere.gui.components.control.simulation.*;
import org.vadere.gui.components.utils.Localization;
import org.vadere.gui.components.utils.Resources;
import org.vadere.gui.components.utils.SwingUtils;
import org.vadere.gui.components.view.DialogFactory;
import org.vadere.gui.components.view.ScenarioElementView;
import org.vadere.gui.components.view.ScenarioScrollPane;
import org.vadere.gui.components.view.SimulationInfoPanel;
import org.vadere.gui.onlinevisualization.control.ActionOnlineVisMenu;
import org.vadere.gui.onlinevisualization.control.ActionShowMesh;
import org.vadere.gui.onlinevisualization.control.ActionShowPotentialField;
import org.vadere.gui.onlinevisualization.model.OnlineVisualizationModel;
import org.vadere.util.config.VadereConfig;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Observable;
import java.util.Observer;

public class OnlineVisualisationWindow extends JPanel implements Observer {

	private static final long serialVersionUID = 3522170593760789565L;
	private static final Resources resources = Resources.getInstance("global");
	private static final Configuration CONFIG = VadereConfig.getConfig();

	private static final int ICON_SIZE = (int)(VadereConfig.getConfig().getInt("ProjectView.icon.height.value")*VadereConfig.getConfig().getFloat("Gui.scale"));
	private final Resources RESOURCE = Resources.getInstance("global");

	private final JToolBar toolbar;				// top
	private final SimulationInfoPanel infoPanel;	// footer
	private final JScrollPane scenarioScrollPane; // left
	private final ScenarioElementView jsonPanel;  // right
	private final JSplitPane splitPaneForTopographyAndJsonPane; // container left/right
	private final MainPanel mainPanel;
	private final OnlineVisualizationModel model;

	public OnlineVisualisationWindow(final MainPanel mainPanel, final OnlineVisualizationModel model) {
		this.mainPanel = mainPanel;
		this.model = model;
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		int windowHeight = screenSize.height - 250;

		CellConstraints cc = new CellConstraints();
		FormLayout spiltLayout = new FormLayout("2dlu, default:grow(0.75), 2dlu, default:grow(0.25), 2dlu", // col
				"2dlu, default, 2dlu, fill:default:grow, 2dlu, default, 2dlu"); // rows

		scenarioScrollPane = new ScenarioScrollPane(mainPanel, model);
		model.addScaleChangeListener(mainPanel);
		mainPanel.addComponentListener(new PanelResizeListener(model));
		mainPanel.setScrollPane(scenarioScrollPane);
		scenarioScrollPane.getViewport()
				.addChangeListener(new JViewportChangeListener(model, scenarioScrollPane.getVerticalScrollBar()));
		model.addScrollPane(scenarioScrollPane);

		IViewportChangeListener viewportChangeListener = new ViewportChangeListener(model, scenarioScrollPane);
		model.addViewportChangeListener(viewportChangeListener);

		jsonPanel = new ScenarioElementView(model);
		jsonPanel.setEditable(false);

		model.addObserver(mainPanel);
		model.addSelectScenarioElementListener(jsonPanel);

		this.toolbar = new JToolBar("OnlineVisualizationToolbar");
		toolbar.setFloatable(false);
		toolbar.setBorderPainted(false);
		toolbar.setAlignmentX(Component.LEFT_ALIGNMENT);
		toolbar.setAlignmentY(Component.TOP_ALIGNMENT);
		// TODO: Should this be really configurable in a config file?
		int toolbarSize = CONFIG.getInt("Gui.toolbar.size");
		toolbar.setPreferredSize(new Dimension(toolbarSize, toolbarSize));

		infoPanel = new SimulationInfoPanel(model);

		model.addObserver(this);
		model.addObserver(infoPanel);

		setLayout(spiltLayout);

		// TODO: Should this be really configurable in a config file?
		int iconHeight = CONFIG.getInt("ProjectView.icon.height.value");
		int iconWidth = CONFIG.getInt("ProjectView.icon.width.value");

		AbstractAction openSettingsDialog = new ActionVisualization("settings",RESOURCE.getIconSVG("settings",ICON_SIZE,ICON_SIZE), model) {
					@Override
					public void actionPerformed(ActionEvent e) {
						DialogFactory.createSettingsDialog(model).setVisible(true);
					}
				};


		AbstractAction paintArrowAction = new AbstractAction("paintArrowAction", RESOURCE.getIconSVG("walking_direction",ICON_SIZE,ICON_SIZE)) {

			private static final long serialVersionUID = 14131313L;

			@Override
			public void actionPerformed(ActionEvent e) {
				model.config.setShowWalkdirection(!model.config.isShowWalkdirection());
				model.notifyObservers();
			}
		};

		AbstractAction paintPedestriansAction = new AbstractAction("paintPedestrianAction",
				resources.getIcon("pedestrian.png", iconWidth, iconHeight)) {

			private static final long serialVersionUID = 14131313L;

			@Override
			public void actionPerformed(ActionEvent e) {
				model.config.setShowPedestrians(!model.config.isShowPedestrians());
				model.notifyObservers();
			}
		};

		AbstractAction showGroupInformationAction = new AbstractAction("showGroupInformationAction",
				RESOURCE.getIconSVG("group",ICON_SIZE,ICON_SIZE)) {
			@Override
			public void actionPerformed(ActionEvent e) {
				model.config.setShowGroups(!model.config.isShowGroups());
				model.notifyObservers();
			}
		};

		AbstractAction drawVoronoiDiagram = new ActionSwapSelectionMode("draw_voronoi_diagram", RESOURCE.getIconSVG("voronoi",ICON_SIZE,ICON_SIZE), model);

		AbstractAction drawMesh = new ActionShowMesh("draw_mesh", RESOURCE.getIconSVG("triangulation",ICON_SIZE,ICON_SIZE), model);


		AbstractAction paintGridAction = new AbstractAction("paintGridAction",
				RESOURCE.getIconSVG("grid",ICON_SIZE,ICON_SIZE)) {

			private static final long serialVersionUID = 1123132132L;

			@Override
			public void actionPerformed(ActionEvent e) {
				model.config.setShowGrid(!model.config.isShowGrid());
				model.notifyObservers();
			}
		};

		AbstractAction paintTrajectories = new AbstractAction("paintTrajectoriesAction",
				RESOURCE.getIconSVG("trajectories",ICON_SIZE,ICON_SIZE)) {

			private static final long serialVersionUID = 1123132132L;

			@Override
			public void actionPerformed(ActionEvent e) {
				model.config.setShowTrajectories(!model.config.isShowTrajectories());
				model.notifyObservers();
			}
		};

		AbstractAction paintDensity = new AbstractAction("paintDensityAction",
				RESOURCE.getIconSVG("density",ICON_SIZE,ICON_SIZE)) {

			private static final long serialVersionUID = 1123132132L;

			@Override
			public void actionPerformed(ActionEvent e) {
				model.config.setShowDensity(!model.config.isShowDensity());
				model.notifyObservers();
			}
		};

		OnlinevisualizationRenderer renderer = new OnlinevisualizationRenderer(model);
		renderer.setLogo(resources.getImage("vadere.png"));
		ActionGeneratePNG generatePNG = new ActionGeneratePNG(
				Localization.getString("ProjectView.btnPNGSnapshot.tooltip"),
				RESOURCE.getIconSVG("camera_png",ICON_SIZE,ICON_SIZE),
				renderer,
				model);

		ActionGenerateSVG generateSVG = new ActionGenerateSVG(
				Localization.getString("ProjectView.btnSVGSnapshot.tooltip"),
				RESOURCE.getIconSVG("camera_svg",ICON_SIZE,ICON_SIZE),
				renderer,
				model);

		ActionGenerateTikz generateTikz = new ActionGenerateTikz(
				Localization.getString("ProjectView.btnTikZSnapshot.tooltip"),
				RESOURCE.getIconSVG("camera_tikz",ICON_SIZE,ICON_SIZE),
				renderer,
				model);

		ActionGenerateINETenv generateINETenv = new ActionGenerateINETenv(
				Localization.getString("ProjectView.btnINETSnapshot.tooltip"),
				RESOURCE.getIconSVG("camera_tikz",ICON_SIZE,ICON_SIZE),
				renderer,
				model);

		ActionGeneratePoly generatePoly = new ActionGeneratePoly(
				model);

        ActionShowPotentialField showPotentialField = new ActionShowPotentialField(
                "showPotentialField",
				RESOURCE.getIconSVG("potential",ICON_SIZE,ICON_SIZE),
                model);

		ActionRecording recording = new ActionRecording(
				"showPotentialField",
				RESOURCE.getIconSVG("record",ICON_SIZE,ICON_SIZE),
				renderer);


		mainPanel.addRendererChangeListener(generatePNG);
		mainPanel.addRendererChangeListener(generateSVG);
		mainPanel.addRendererChangeListener(generateTikz);
		mainPanel.addRendererChangeListener(generateINETenv);
		mainPanel.addRendererChangeListener(showPotentialField);

		// Pedestrian-related options
		SwingUtils.addActionToToolbar(toolbar, paintPedestriansAction, Localization.getString("ProjectView.btnShowPedestrian.tooltip"));
		SwingUtils.addActionToToolbar(toolbar, paintTrajectories, Localization.getString("ProjectView.btnShowTrajectories.tooltip"));
		SwingUtils.addActionToToolbar(toolbar, paintArrowAction, Localization.getString("ProjectView.btnShowWalkingDirection.tooltip"));
		SwingUtils.addActionToToolbar(toolbar, showGroupInformationAction, Localization.getString("ProjectView.btnShowGroupInformation.tooltip"));
		toolbar.addSeparator();

		// "Measuring" tools
		SwingUtils.addActionToToolbar(toolbar, drawVoronoiDiagram, Localization.getString("ProjectView.btnDrawVoronoiDiagram.tooltip"));
		SwingUtils.addActionToToolbar(toolbar, drawMesh, Localization.getString("ProjectView.btnDrawMesh.tooltip"));
		SwingUtils.addActionToToolbar(toolbar, paintGridAction, Localization.getString("ProjectView.btnShowGrid.tooltip"));
		SwingUtils.addActionToToolbar(toolbar, paintDensity, Localization.getString("ProjectView.btnShowDensity.tooltip"));
		toolbar.addSeparator();

		// Snapshot options
		ArrayList<Action> imgOptions = new ArrayList<>();
		imgOptions.add(generatePNG);
		imgOptions.add(generateSVG);
		imgOptions.add(generateTikz);
		imgOptions.add(generateINETenv);
		imgOptions.add(generatePoly);

		ActionOnlineVisMenu imgDialog = new ActionOnlineVisMenu(
				"camera_menu",
				RESOURCE.getIconSVG("camera",ICON_SIZE,ICON_SIZE), imgOptions);
		JButton imgMenuBtn = SwingUtils.addActionToToolbar(toolbar, imgDialog, Localization.getString("ProjectView.btnSnapshot.tooltip"));
		imgDialog.setParent(imgMenuBtn);

		SwingUtils.addActionToToolbar(toolbar, showPotentialField, Localization.getString("OnlineVis.btnShowPotentialfield.tooltip"));
		JButton recordButton = SwingUtils.addActionToToolbar(toolbar, recording, Localization.getString("OnlineVis.btnRecord.tooltip"));
		recording.setButton(recordButton);

		toolbar.add(Box.createHorizontalGlue());

		SwingUtils.addActionToToolbar(toolbar, openSettingsDialog, Localization.getString("ProjectView.btnSettings.tooltip"));

		splitPaneForTopographyAndJsonPane = new JSplitPane();
		splitPaneForTopographyAndJsonPane.setResizeWeight(0.8);
		splitPaneForTopographyAndJsonPane.resetToPreferredSizes();
		splitPaneForTopographyAndJsonPane.setLeftComponent(scenarioScrollPane);
		splitPaneForTopographyAndJsonPane.setRightComponent(jsonPanel);

		scenarioScrollPane.setPreferredSize(new Dimension(1, windowHeight));

		add(toolbar, cc.xyw(2, 2, 3));
		add(splitPaneForTopographyAndJsonPane, cc.xywh(2, 4, 4, 1));
		add(infoPanel, cc.xyw(2, 6, 3));

		repaint();
		revalidate();
	}

	@Override
	public void update(Observable o, Object arg) {
		repaint();
	}
}
