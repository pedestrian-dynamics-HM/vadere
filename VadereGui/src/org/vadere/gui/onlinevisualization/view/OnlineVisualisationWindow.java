package org.vadere.gui.onlinevisualization.view;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import org.apache.commons.configuration2.Configuration;
import org.vadere.gui.components.control.*;
import org.vadere.gui.components.control.simulation.*;
import org.vadere.gui.components.utils.Messages;
import org.vadere.gui.components.utils.ResourceStrings;
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

		AbstractAction openSettingsDialog = new ActionVisualization("settings", resources.getIcon("settings.png", iconWidth, iconHeight), model) {
					@Override
					public void actionPerformed(ActionEvent e) {
						DialogFactory.createSettingsDialog(model).setVisible(true);
					}
				};


		AbstractAction paintArrowAction = new AbstractAction("paintArrowAction",
				resources.getIcon("walking_direction.png", iconWidth, iconHeight)) {

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
				resources.getIcon("group.png", iconWidth, iconHeight)) {
			@Override
			public void actionPerformed(ActionEvent e) {
				model.config.setShowGroups(!model.config.isShowGroups());
				model.notifyObservers();
			}
		};

		AbstractAction drawVoronoiDiagram = new ActionSwapSelectionMode("draw_voronoi_diagram", resources.getIcon("voronoi.png", iconWidth, iconHeight), model);

		AbstractAction drawMesh = new ActionShowMesh("draw_mesh", resources.getIcon("triangulation.png", iconWidth, iconHeight), model);


		AbstractAction paintGridAction = new AbstractAction("paintGridAction",
				resources.getIcon("grid.png", iconWidth, iconHeight)) {

			private static final long serialVersionUID = 1123132132L;

			@Override
			public void actionPerformed(ActionEvent e) {
				model.config.setShowGrid(!model.config.isShowGrid());
				model.notifyObservers();
			}
		};

		AbstractAction paintTrajectories = new AbstractAction("paintTrajectoriesAction",
				resources.getIcon("trajectories.png", iconWidth, iconHeight)) {

			private static final long serialVersionUID = 1123132132L;

			@Override
			public void actionPerformed(ActionEvent e) {
				model.config.setShowTrajectories(!model.config.isShowTrajectories());
				model.notifyObservers();
			}
		};

		AbstractAction paintDensity = new AbstractAction("paintDensityAction",
				resources.getIcon("density.png", iconWidth, iconHeight)) {

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
				Messages.getString("ProjectView.btnPNGSnapshot.tooltip"),
				resources.getIcon("camera_png.png", iconWidth, iconHeight),
				renderer,
				model);

		ActionGenerateSVG generateSVG = new ActionGenerateSVG(
				Messages.getString("ProjectView.btnSVGSnapshot.tooltip"),
				resources.getIcon("camera_svg.png", iconWidth, iconHeight),
				renderer,
				model);

		ActionGenerateTikz generateTikz = new ActionGenerateTikz(
				Messages.getString("ProjectView.btnTikZSnapshot.tooltip"),
				resources.getIcon("camera_tikz.png", iconWidth, iconHeight),
				renderer,
				model);

		ActionGenerateINETenv generateINETenv = new ActionGenerateINETenv(
				Messages.getString("ProjectView.btnINETSnapshot.tooltip"),
				resources.getIcon("camera_tikz.png", iconWidth, iconHeight),
				renderer,
				model);

		ActionGeneratePoly generatePoly = new ActionGeneratePoly(
				Messages.getString("ProjectView.btnPolySnapshot.tooltip"),
				resources.getIcon("camera_poly.png", iconWidth, iconHeight),
				ResourceStrings.TOPOGRAPHY_CREATOR_BTN_GENERATE_POLY_TOOLTIP,
				model);

        ActionShowPotentialField showPotentialField = new ActionShowPotentialField(
                "showPotentialField",
				resources.getIcon("potentialField.png", iconWidth, iconHeight),
                model);

		ActionRecording recording = new ActionRecording(
				"showPotentialField",
				resources.getIcon("record.png", iconWidth, iconHeight),
				renderer);


		mainPanel.addRendererChangeListener(generatePNG);
		mainPanel.addRendererChangeListener(generateSVG);
		mainPanel.addRendererChangeListener(generateTikz);
		mainPanel.addRendererChangeListener(generateINETenv);
		mainPanel.addRendererChangeListener(showPotentialField);

		// Pedestrian-related options
		SwingUtils.addActionToToolbar(toolbar, paintPedestriansAction, Messages.getString("ProjectView.btnShowPedestrian.tooltip"));
		SwingUtils.addActionToToolbar(toolbar, paintTrajectories, Messages.getString("ProjectView.btnShowTrajectories.tooltip"));
		SwingUtils.addActionToToolbar(toolbar, paintArrowAction, Messages.getString("ProjectView.btnShowWalkingDirection.tooltip"));
		SwingUtils.addActionToToolbar(toolbar, showGroupInformationAction, Messages.getString("ProjectView.btnShowGroupInformation.tooltip"));
		toolbar.addSeparator();

		// "Measuring" tools
		SwingUtils.addActionToToolbar(toolbar, drawVoronoiDiagram, Messages.getString("ProjectView.btnDrawVoronoiDiagram.tooltip"));
		SwingUtils.addActionToToolbar(toolbar, drawMesh, Messages.getString("ProjectView.btnDrawMesh.tooltip"));
		SwingUtils.addActionToToolbar(toolbar, paintGridAction, Messages.getString("ProjectView.btnShowGrid.tooltip"));
		SwingUtils.addActionToToolbar(toolbar, paintDensity, Messages.getString("ProjectView.btnShowDensity.tooltip"));
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
				resources.getIcon("camera.png", iconWidth, iconHeight), imgOptions);
		JButton imgMenuBtn = SwingUtils.addActionToToolbar(toolbar, imgDialog, Messages.getString("ProjectView.btnSnapshot.tooltip"));
		imgDialog.setParent(imgMenuBtn);

		SwingUtils.addActionToToolbar(toolbar, showPotentialField, Messages.getString("OnlineVis.btnShowPotentialfield.tooltip"));
		JButton recordButton = SwingUtils.addActionToToolbar(toolbar, recording, Messages.getString("OnlineVis.btnRecord.tooltip"));
		recording.setButton(recordButton);

		toolbar.add(Box.createHorizontalGlue());

		SwingUtils.addActionToToolbar(toolbar, openSettingsDialog, Messages.getString("ProjectView.btnSettings.tooltip"));

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
