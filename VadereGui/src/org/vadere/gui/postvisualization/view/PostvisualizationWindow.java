package org.vadere.gui.postvisualization.view;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import org.apache.commons.configuration2.Configuration;
import org.jetbrains.annotations.NotNull;
import org.vadere.gui.components.control.IViewportChangeListener;
import org.vadere.gui.components.control.JViewportChangeListener;
import org.vadere.gui.components.control.PanelResizeListener;
import org.vadere.gui.components.control.ViewportChangeListener;
import org.vadere.gui.components.model.IDefaultModel;
import org.vadere.gui.components.utils.Messages;
import org.vadere.gui.components.utils.ResourceStrings;
import org.vadere.gui.components.utils.Resources;
import org.vadere.gui.components.view.ScenarioElementView;
import org.vadere.gui.postvisualization.model.PostvisualizationModel;
import org.vadere.simulator.projects.Scenario;
import org.vadere.simulator.projects.io.IOOutput;
import org.vadere.util.config.VadereConfig;
import org.vadere.util.io.IOUtils;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.Observable;
import java.util.Observer;
import java.util.Optional;

public class PostvisualizationWindow extends JPanel implements Observer {

    protected static final Configuration CONFIG = VadereConfig.getConfig();

    private static final long serialVersionUID = -8177132133860336295L;

    protected static Resources resources = Resources.getInstance("postvisualization");

    private final PostvisualizationRenderer renderer;
    private final JScrollPane scrollPane;

    protected PostvisualizationModel model;
    protected ScenarioElementView textView;
    protected JToolBar toolbar;
    protected ScenarioPanel scenarioPanel;
    protected AdjustPanel adjustPanel;
    protected JMenu mRecentFiles;
    protected JMenuBar menuBar;

    public PostvisualizationWindow(PostvisualizationModel model,
                                   PostvisualizationRenderer renderer
    ) {

        this.model = model;
        this.renderer = renderer;

        this.textView = null;
        this.toolbar = new JToolBar("Toolbar");
        this.scrollPane = new JScrollPane();
        this.scenarioPanel = new ScenarioPanel(this.renderer, this.scrollPane);
        this.adjustPanel = new AdjustPanel(this.model);
        this.mRecentFiles = new JMenu(Messages.getString("PostVis.menuRecentFiles.title"));
        this.menuBar = new JMenuBar();


    }

    public PostvisualizationWindow(PostvisualizationModel model) {
        this(model, new PostvisualizationRenderer(model));
    }

    public PostvisualizationWindow() {
        this(new PostvisualizationModel());
    }

    public void init(final boolean loadTopographyInformationsOnly, final String projectPath) {

        // 1. get data from the user screen
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int windowHeight = screenSize.height - 250;

        // 2. set up the model
        model.addObserver(this);
        model.config.setLoadTopographyInformationsOnly(loadTopographyInformationsOnly);

        // 3. set up renderer (he draws also the svg and the png's)
        renderer.setLogo(resources.getImage("vadere.png"));

        renderer.setImage(resources.getImage("zombie.png"));

        // 4. set up the jscrollpane
        scrollPane.getViewport()
                .addChangeListener(new JViewportChangeListener(model, scrollPane.getVerticalScrollBar()));
        scrollPane.setPreferredSize(new Dimension(1, windowHeight));
        IViewportChangeListener viewportChangeListener = new ViewportChangeListener(model, scrollPane);
        model.addViewportChangeListener(viewportChangeListener);
        model.addScrollPane(scrollPane);

        // 5. set up the scenario panel on that the renderer draw all the content.
        model.addObserver(scenarioPanel);
        scenarioPanel.addComponentListener(new PanelResizeListener(model));
        model.addScaleChangeListener(scenarioPanel);
        scrollPane.setViewportView(scenarioPanel);

        // 6. set up the toolbar
        int toolbarSize = CONFIG.getInt("Gui.toolbar.size");
        //toolbar.setPreferredSize(new Dimension(toolbarSize, toolbarSize));
        toolbar.setBorderPainted(false);
        toolbar.setFloatable(false);
        toolbar.setAlignmentX(Component.LEFT_ALIGNMENT);
        toolbar.setAlignmentY(Component.TOP_ALIGNMENT);

        // 7. set up the adjust panel
        model.addObserver(adjustPanel);

        // 8. set the view options of this frame
        FormLayout layout;
        CellConstraints cc = new CellConstraints();

        // 9. add all components to this frame
        if (CONFIG.getBoolean("PostVis.enableJsonInformationPanel")) {
            layout = new FormLayout("2dlu, default:grow(0.75), 2dlu, default:grow(0.25), 2dlu", // col
                    "2dlu, default, 2dlu, default, 2dlu, default, 2dlu"); // rows
            setLayout(layout);

            textView = new ScenarioElementView(model);
            textView.setEditable(false);
            textView.setPreferredSize(new Dimension(1, windowHeight));

            JSplitPane splitPaneForTopographyAndJsonPane = new JSplitPane();
            splitPaneForTopographyAndJsonPane.setResizeWeight(0.8);
            splitPaneForTopographyAndJsonPane.resetToPreferredSizes();
            splitPaneForTopographyAndJsonPane.setLeftComponent(scrollPane);
            splitPaneForTopographyAndJsonPane.setRightComponent(textView);

            add(toolbar, cc.xyw(2, 2, 3));
            add(splitPaneForTopographyAndJsonPane, cc.xywh(2, 4, 4, 1));
            add(adjustPanel, cc.xyw(2, 6, 4));
        } else {
            layout = new FormLayout("2dlu, default:grow, 2dlu", // col
                    "2dlu, default, 2dlu, default, 2dlu, default, 2dlu"); // rows
            setLayout(layout);

            add(toolbar, cc.xy(2, 2));
            add(scrollPane, cc.xy(2, 4));
            add(adjustPanel, cc.xy(2, 6));
        }

    }

    public IDefaultModel getDefaultModel() {
        return this.model;
    }


    protected JMenuBar getMenu() {
        return menuBar;
    }

    protected PostvisualizationRenderer getRenderer() {
        return renderer;
    }


    @Override
    public void update(Observable o, Object arg) {

    }

    public void loadOutputDir(@NotNull File vadereOutputDirectory) {

        Optional<File> trajectoryFile =
                IOUtils.getFirstFile(vadereOutputDirectory, IOUtils.TRAJECTORY_FILE_EXTENSION);
        Optional<File> scenarioFile =
                IOUtils.getFirstFile(vadereOutputDirectory, IOUtils.SCENARIO_FILE_EXTENSION);

        if (trajectoryFile.isPresent() && scenarioFile.isPresent()) {
            try {
                Scenario vadereScenario = IOOutput.readScenario(scenarioFile.get().toPath());
                model.init(IOOutput.readTrajectories(trajectoryFile.get().toPath()), vadereScenario, trajectoryFile.get().getParent());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

    }
}
