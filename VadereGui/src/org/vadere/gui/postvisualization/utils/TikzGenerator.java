package org.vadere.gui.postvisualization.utils;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.vadere.gui.components.model.DefaultSimulationConfig;
import org.vadere.gui.components.model.SimulationModel;
import org.vadere.gui.components.view.SimulationRenderer;
import org.vadere.gui.postvisualization.model.PostvisualizationModel;
import org.vadere.simulator.projects.dataprocessing.processor.PedestrianOSMStrideLengthProcessor;
import org.vadere.state.scenario.*;
import org.vadere.state.simulation.Step;
import org.vadere.state.simulation.Trajectory;
import org.vadere.util.geometry.shapes.VPoint;

import javax.swing.text.html.Option;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.awt.geom.PathIterator.*;

/**
 * Convert the (Java) scenario description into a TikZ representation.
 *
 * Usually, each (Java) scenario element is represented as a path via
 * @see PathIterator This PathSeparator must be converted into its TikZ
 * representation.
 *
 * For example, traversing a Java path with PathIterator returns two segments:
 *
 *   segment type = 0 (SEG_MOVETO) with coords: [1.0, 1.0, 0.0, 0.0, 0.0, 0.0]
 *   segment type = 3 (SEG_LINETO) with coords: [2.0, 2.0, 0.0, 0.0, 0.0, 0.0]
 *
 * This must be transformed to TikZ:
 *
 * (1.0,1.0) to (2.0,2.0)
 *
 * The TikZGenerator should also respect the GUI settings (e.g., enabled
 * elements, colors etc.).
 */
public class TikzGenerator {

	private final static Logger logger = LogManager.getLogger(TikzGenerator.class);
	private final SimulationRenderer renderer;
	private final SimulationModel<? extends DefaultSimulationConfig> model;
	private final String[] translationTable;

	public TikzGenerator(final SimulationRenderer renderer,
						 final SimulationModel<? extends DefaultSimulationConfig> model) {
		this.renderer = renderer;
		this.model = model;
		this.translationTable = new String[SEG_CLOSE + 1];

		initializeTranslationTable(translationTable);
	}

	private void initializeTranslationTable(String[] translationTable) {
		// Map Java path commands to TikZ commands.
		translationTable[SEG_MOVETO] = "(%f,%f) ";
		translationTable[SEG_LINETO] = "to (%f,%f) ";
		translationTable[SEG_QUADTO] = ".. controls (%f,%f) .. (%f,%f) ";
		translationTable[SEG_CUBICTO] = ".. controls (%f,%f) and (%f,%f) .. (%f,%f) ";
		translationTable[SEG_CLOSE] = "";
	}

	public void generateTikz(final File file, final boolean generateCompleteDocument) {
		String texTemplate = "" +
				"\\documentclass{standalone}\n" +
				"\\usepackage{tikz}\n\n" +
				"\\begin{document}\n" +
				"\\begin{tikzpicture}\n" +
				"%s" +
				"\\end{tikzpicture}\n" +
				"\\end{document}\n";

		String tikzCode = "";
		tikzCode += generateTikzColorDefinitions(model);
		tikzCode += convertScenarioElementsToTikz();

		String output = (generateCompleteDocument) ? String.format(texTemplate, tikzCode) : tikzCode;

		// TODO: maybe uses Java's resources notation (in general, writing the file should be done by the caller not here).
		try {
			file.createNewFile();
			Writer out = new OutputStreamWriter(new FileOutputStream(file), "UTF-8");
			out.write(output);
			out.flush();
			logger.info("generate new TikZ: " + file.getAbsolutePath());
		} catch (IOException e1) {
			logger.error(e1.getMessage());
			e1.printStackTrace();
		}
	}

	private String generateTikzColorDefinitions(SimulationModel<? extends DefaultSimulationConfig> model) {
		String colorDefinitions = "% Color Definitions\n";

		String colorTextPattern = "\\definecolor{%s}{RGB}{%d,%d,%d}\n";

		Color sourceColor = model.getConfig().getSourceColor();
		colorDefinitions += String.format(colorTextPattern, "SourceColor", sourceColor.getRed(), sourceColor.getGreen(), sourceColor.getBlue());

		Color targetColor = model.getConfig().getTargetColor();
		colorDefinitions += String.format(colorTextPattern, "TargetColor", targetColor.getRed(), targetColor.getGreen(), targetColor.getBlue());

		Color obstacleColor = model.getConfig().getObstacleColor();
		colorDefinitions += String.format(colorTextPattern, "ObstacleColor", obstacleColor.getRed(), obstacleColor.getGreen(), obstacleColor.getBlue());

		Color stairColor = model.getConfig().getStairColor();
		colorDefinitions += String.format(colorTextPattern, "StairColor", stairColor.getRed(), stairColor.getGreen(), stairColor.getBlue());

		Color agentColor = model.getConfig().getPedestrianDefaultColor();
		colorDefinitions += String.format(colorTextPattern, "AgentColor", agentColor.getRed(), agentColor.getGreen(), agentColor.getBlue());

		return colorDefinitions;
	}

	private String convertScenarioElementsToTikz() {
		String generatedCode = "";

		DefaultSimulationConfig config = model.getConfig();
		Topography topography = model.getTopography();

		// Clip everything outside of topography bound.
		generatedCode += "% Clipping\n";
		String clipTextPattern = "\\clip (%f,%f) rectangle (%f,%f);\n";
		generatedCode += String.format(clipTextPattern,
				topography.getBounds().x,
				topography.getBounds().y,
				topography.getBounds().x + topography.getBounds().width,
				topography.getBounds().y + topography.getBounds().height);

		// Draw background elements first, then other scenario elements.
		generatedCode += "% Ground\n";
		String groundTextPattern = (config.isShowGrid()) ? "\\draw[help lines] (%f,%f) grid (%f,%f);\n" : "\\fill[white] (%f,%f) rectangle (%f,%f);\n";
		generatedCode += String.format(groundTextPattern,
				topography.getBounds().x,
				topography.getBounds().y,
				topography.getBounds().x + topography.getBounds().width,
				topography.getBounds().y + topography.getBounds().height);

		if (config.isShowSources()) {
			generatedCode += "% Sources\n";
			for (Source source : topography.getSources()) {
				generatedCode += String.format("\\fill[SourceColor] %s;\n", generatePathForScenarioElement(source));
			}
		} else {
			generatedCode += "% Sources (not enabled in config)\n";
		}

		if (config.isShowTargets()) {
			generatedCode += "% Targets\n";
			for (Target target : topography.getTargets()) {
				generatedCode += String.format("\\fill[TargetColor] %s;\n", generatePathForScenarioElement(target));
			}
		} else {
			generatedCode += "% Targets (not enabled in config)\n";
		}

		if (config.isShowObstacles()) {
			generatedCode += "% Obstacles\n";
			for (Obstacle obstacle : topography.getObstacles()) {
				generatedCode += String.format("\\fill[ObstacleColor] %s;\n", generatePathForScenarioElement(obstacle));
			}
		} else {
			generatedCode += "% Obstacles (not enabled in config)\n";
		}

		if (config.isShowStairs()) {
			generatedCode += "% Stairs\n";
			for (Stairs stair : topography.getStairs()) {
				generatedCode += String.format("\\fill[StairColor] %s;\n", generatePathForScenarioElement(stair));
			}
		} else {
			generatedCode += "% Stairs (not enabled in config)\n";
		}

        if (config.isShowTrajectories()) {
            generatedCode += "% Trajectories\n";

            if (model instanceof PostvisualizationModel) {
                generatedCode += drawTrajectories((PostvisualizationModel)model);
            } else {
                generatedCode += String.format("%% Passed model %s does not contain trajectories\n", model.getClass().getSimpleName());
            }
        } else {
            generatedCode += "% Trajectories (not enabled in config)\n";
        }

        if (config.isShowPedestrians()) {
            generatedCode += "% Agents\n";
            generatedCode += drawAgents(config);
        } else {
            generatedCode = "% Agents (not enabled in config)\n";
        }

        return generatedCode;
	}

    private String drawTrajectories(PostvisualizationModel model) {
	    // Use a thread-safe string implementation because streams are used here.
        final StringBuffer generatedCode = new StringBuffer("");

	    Stream<Trajectory> trajectoryStream = model.getAlivePedestrians();
        Step currentTimeStep = model.getStep().orElseGet(null);

        if (currentTimeStep != null) {
            trajectoryStream.forEach(trajectory -> {
                Stream<VPoint> trajectoryPoints = trajectory.getPositionsReverse(currentTimeStep);

                // Use a newline ("to\n") for joining because TeX could possibly choke up on long lines.
                String trajectoryAsTikzString = trajectoryPoints
                        .map(point -> String.format("(%f,%f)", point.x, point.y))
                        .collect(Collectors.joining(" to\n"));

                String coloredTrajectory = applyAgentColorToTrajectory(trajectoryAsTikzString, trajectory.getAgent(currentTimeStep));

                int pedestrianId = trajectory.getPedestrianId();
                Optional<Step> trajectoryEndStep = trajectory.getEndStep();
                String trajectoryEndStepAsString = (trajectoryEndStep.isPresent()) ? "" + trajectoryEndStep.get().toString() : "unknown end step" ;
                String currentTimeStepAsString = currentTimeStep.toString();

                generatedCode.append(String.format("%% Trajectory Agent %d @ step %s of %s\n", pedestrianId, currentTimeStepAsString, trajectoryEndStepAsString));
                generatedCode.append(coloredTrajectory);
            });
        }


	    return generatedCode.toString();
    }

    private String applyAgentColorToTrajectory(String trajectory, Optional<Agent> agent) {
	    String colorString = "AgentColor";

	    if (agent.get() instanceof Pedestrian) {
	        Pedestrian pedestrian = (Pedestrian)agent.get();
	        Color pedestrianColor = renderer.getAgentRender().getColor(pedestrian);

            colorString = String.format("{rgb,255: red,%d; green,%d; blue,%d}", pedestrianColor.getRed(), pedestrianColor.getGreen(), pedestrianColor.getBlue());
        }

        return String.format("\\draw[draw=%s]\n%s;\n", colorString, trajectory);
    }

    @NotNull
    private String drawAgents(DefaultSimulationConfig config) {
	    String generatedCode = "";

        for (Agent agent : model.getAgents()) {
            if (model.getConfig().isShowGroups()) {
                try {
                    Pedestrian pedestrian = (Pedestrian) agent;
                    Color pedestrianColor = renderer.getAgentRender().getColor(pedestrian);
                    Shape pedestrianShape = renderer.getAgentRender().getShape(pedestrian);

                    String colorString = String.format("{rgb,255: red,%d; green,%d; blue,%d}", pedestrianColor.getRed(), pedestrianColor.getGreen(), pedestrianColor.getBlue());
                    generatedCode += String.format("\\fill[fill=%s] %s;\n", colorString, generatePathForShape(pedestrianShape));
                } catch (ClassCastException cce) {
                    logger.error("Error casting to Pedestrian");
                    cce.printStackTrace();

                    // Fall back to default rendering of agents.
					String agentTextPattern = "\\fill[AgentColor] (%f,%f) circle [radius=%fcm];\n";
					generatedCode += String.format(agentTextPattern, agent.getPosition().x, agent.getPosition().y, agent.getRadius());
                }
            } else {
				// Do not draw agents as path for performance reasons. Usually, agents have a circular shape.
				// generatedCode += String.format("\\fill[AgentColor] %s\n", generatePathForScenarioElement(agent));
                String agentTextPattern = "\\fill[AgentColor] (%f,%f) circle [radius=%fcm];\n";
                generatedCode += String.format(agentTextPattern, agent.getPosition().x, agent.getPosition().y, agent.getRadius());
            }

            if (model.isElementSelected() && model.getSelectedElement().equals(agent)) {
                String agentTextPattern = "\\draw[magenta] (%f,%f) circle [radius=%fcm];\n";
                generatedCode += String.format(agentTextPattern, agent.getPosition().x, agent.getPosition().y, agent.getRadius());
            }
        }

        return generatedCode;
    }

    private String generatePathForScenarioElement(ScenarioElement element) {
		String generatedPath = "";

		AffineTransform noTransformation = new AffineTransform();
		PathIterator pathIterator = element.getShape().getPathIterator(noTransformation);

		while (!pathIterator.isDone()) {
			float[] coords = new float[6];
			int type = pathIterator.currentSegment(coords);

			generatedPath += convertJavaToTikzPath(type, coords);
			pathIterator.next();
		}

		return generatedPath.trim();
	}

    private String generatePathForShape(Shape shape) {
        String generatedPath = "";

        AffineTransform noTransformation = new AffineTransform();
        PathIterator pathIterator = shape.getPathIterator(noTransformation);

        while (!pathIterator.isDone()) {
            float[] coords = new float[6];
            int type = pathIterator.currentSegment(coords);

            generatedPath += convertJavaToTikzPath(type, coords);
            pathIterator.next();
        }

        return generatedPath.trim();
    }

	private String convertJavaToTikzPath(int type, float[] coords) {
		if (type < SEG_MOVETO || type > SEG_CLOSE) {
			throw new IllegalStateException(String.format("Cannot process path segment type: %d (coordinates: %s)", type, Arrays.toString(coords)));
		}

		String convertedPath = translationTable[type];

		if (type == SEG_MOVETO) {
			convertedPath = String.format(convertedPath, coords[0], coords[1]);
		} else if (type == SEG_LINETO) {
			convertedPath = String.format(convertedPath, coords[0], coords[1]);
		} else if (type == SEG_QUADTO) {
			convertedPath = String.format(convertedPath, coords[0], coords[1], coords[2], coords[3]);
		} else if (type == SEG_CUBICTO) {
			convertedPath = String.format(convertedPath, coords[0], coords[1], coords[2], coords[3], coords[4], coords[5]);
		}

		return convertedPath;
	}
}
