package org.vadere.gui.postvisualization.utils;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.vadere.gui.components.model.DefaultSimulationConfig;
import org.vadere.gui.components.model.SimulationModel;
import org.vadere.gui.components.view.SimulationRenderer;
import org.vadere.state.scenario.*;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;
import java.io.*;
import java.util.*;

import static java.awt.geom.PathIterator.*;

/**
 * Convert the (Java) scenario description into a TikZ representation.
 *
 * Usually, each (Java) scenario element is represented as a path from
 * @see PathIterator This PathSeparator must be converted into its TikZ
 * representation.
 *
 * Also use configured colors from GUI settings.
 */
public class TikzGenerator {

	private static Logger logger = LogManager.getLogger(TikzGenerator.class);
	private SimulationRenderer renderer;
	private final SimulationModel<? extends DefaultSimulationConfig> model;
    private String[] translationTable;

	public TikzGenerator(final SimulationRenderer renderer,
                         final SimulationModel<? extends DefaultSimulationConfig> model) {
		this.renderer = renderer;
		this.model = model;
        translationTable = new String[SEG_CLOSE + 1];

        initializeTranslationTable(translationTable);
    }

    private void initializeTranslationTable(String[] translationTable) {
	    // Map Java path commands to TikZ commands.
        translationTable[SEG_MOVETO] = "(%f,%f) ";
        translationTable[SEG_LINETO] = "to (%f,%f) ";
        translationTable[SEG_QUADTO] = "How to convert SEG_QUADTO to TikZ?";
        translationTable[SEG_CUBICTO] = "How to convert SEG_CUBICTO to TikZ?";
        translationTable[SEG_CLOSE] = "to cycle;";
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

	    String output = (generateCompleteDocument) ? String.format(texTemplate, tikzCode) : tikzCode ;

	    // TODO: maybe uses Java's resources notation.
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

	private String convertScenarioElementsToTikz() {
	    String generatedCode = "";

	    Topography topography = model.getTopography();

        generatedCode += "% Boundary Box\n";
        String boundaryBoxTextPattern = "\\fill[white] (%f,%f) rectangle (%f,%f);\n";
        generatedCode += String.format(boundaryBoxTextPattern,
                topography.getBounds().x,
                topography.getBounds().y,
                topography.getBounds().x + topography.getBounds().width,
                topography.getBounds().y + topography.getBounds().height);

	    // TODO: draw also stairs and (maybe) trajectories.
        generatedCode += "% Sources\n";
        for (Source source : topography.getSources()) {
            generatedCode += String.format("\\fill[SourceColor] %s\n", generatePathForScenarioElement(source));
        }

        generatedCode += "% Targets\n";
        for (Target target : topography.getTargets()) {
            generatedCode += String.format("\\fill[TargetColor] %s\n", generatePathForScenarioElement(target));
        }

        generatedCode += "% Obstacles\n";
        for (Obstacle obstacle : topography.getObstacles()) {
            generatedCode += String.format("\\fill[ObstacleColor] %s\n", generatePathForScenarioElement(obstacle));
        }

        // TODO: add agents as path NOT as pre-defined form (they require cubic splines).
        generatedCode += "% Agents\n";
        for (Agent agent : model.getAgents()) {
            String agentTextPattern = "\\fill[AgentColor] (%f,%f) circle [radius=%fcm];\n";
            generatedCode += String.format(agentTextPattern, agent.getPosition().x, agent.getPosition().y, agent.getRadius());
        }

	    return generatedCode;
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

        Color agentColor = model.getConfig().getPedestrianDefaultColor();
        colorDefinitions += String.format(colorTextPattern, "AgentColor", agentColor.getRed(), agentColor.getGreen(), agentColor.getBlue());

	    return colorDefinitions;
    }

	private String generatePathForScenarioElement(ScenarioElement element) {
	    String generatedPath = "";

		AffineTransform noTransformation = new AffineTransform();
		PathIterator pathIterator = element.getShape().getPathIterator(noTransformation);

		while (!pathIterator.isDone()) {
            float[] coords = new float[6];
            int type = pathIterator.currentSegment(coords);

            generatedPath += convertJavaToTikzPath(type, coords);

			// System.out.println("segment type= " + type + " with coords: " + Arrays.toString(coords));
            pathIterator.next();
		}

		// System.out.println("TikZ path: " + generatedPath);

		return generatedPath;
	}

	private String convertJavaToTikzPath(int type, float[] coords) {
	    String convertedPath = "";

	    if (type > SEG_CLOSE + 1) {
            throw new IllegalStateException(String.format("Cannot process path segment type: %d (coordinates: %s)", type, Arrays.toString(coords)));
        }

        convertedPath = translationTable[type];

	    if (type == SEG_MOVETO) {
            convertedPath = String.format(convertedPath, coords[0], coords[1]);
        } else if (type == SEG_LINETO) {
            convertedPath = String.format(convertedPath, coords[0], coords[1]);
        } else if (type == SEG_QUADTO) {
	        // TODO
        } else if (type == SEG_CUBICTO) {
            // TODO
        }

	    return convertedPath;
    }
}
