package org.vadere.gui.postvisualization.utils;

import org.apache.batik.dom.GenericDOMImplementation;
import org.apache.batik.svggen.SVGGraphics2D;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.vadere.gui.components.model.DefaultSimulationConfig;
import org.vadere.gui.components.model.SimulationModel;
import org.vadere.gui.components.view.SimulationRenderer;
import org.vadere.state.scenario.*;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;

import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;
import java.awt.geom.Rectangle2D;
import java.io.*;
import java.util.Arrays;

public class TikZGenerator {

	private static Logger logger = LogManager.getLogger(TikZGenerator.class);
	private SimulationRenderer renderer;
	private final SimulationModel<? extends DefaultSimulationConfig> model;

	public TikZGenerator(final SimulationRenderer renderer,
                         final SimulationModel<? extends DefaultSimulationConfig> model) {
		this.renderer = renderer;
		this.model = model;
	}

	public void generateTikZ(final File file, final boolean generateCompleteDocument) {
	    String template = "" +
                "\\documentclass{standalone}\n" +
                "\\usepackage{tikz}\n\n" +
                "\\begin{document}\n" +
                "\\begin{tikzpicture}\n" +
                "%s" +
                "\\end{tikzpicture}\n" +
                "\\end{document}\n";

	    String tikZCode = generateTikZCode();

	    String output = (generateCompleteDocument) ? String.format(template, tikZCode) : tikZCode ;

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

    // TODO: draw path(!) of specified object and not a pre-defined form like a circle by extending method "generatePathForScenarioElement()".
	private String generateTikZCode() {
		String generatedCode = "";

		// TODO: maybe, use StringBuilder to be thread-safe.
		// Generate agents.
		String agentTextPattern = "\\fill[blue] (%f,%f) circle [radius=%fcm];\n";
		String agents = "% Agents\n";

		for (Agent agent : model.getAgents()) {
			agents += String.format(agentTextPattern, agent.getPosition().x, agent.getPosition().y, agent.getRadius());
		}

		String sourceTextPattern = "\\fill[green] (%f,%f) rectangle (%f,%f);\n";
		String sources = "% Sources\n";

		for (Source source : model.getTopography().getSources()) {
			Rectangle2D sourceBounds = source.getShape().getBounds2D();
			sources += String.format(sourceTextPattern, sourceBounds.getX(), sourceBounds.getY(), sourceBounds.getX() + sourceBounds.getWidth(), sourceBounds.getY() + sourceBounds.getHeight());
		}

		String targetTextPattern = "\\fill[orange] (%f,%f) rectangle (%f,%f);\n";
		String targets = "% Targets\n";

		for (Target target : model.getTopography().getTargets()) {
			Rectangle2D targetBounds = target.getShape().getBounds2D();
			targets += String.format(targetTextPattern, targetBounds.getX(), targetBounds.getY(), targetBounds.getX() + targetBounds.getWidth(), targetBounds.getY() + targetBounds.getHeight());
		}

		String obstacleTextPattern = "\\fill[black] (%f,%f) rectangle (%f,%f);\n";
		String obstacles = "% Obstacles\n";

		for (Obstacle obstacle : model.getTopography().getObstacles()) {
			Rectangle2D obstacleBounds = obstacle.getShape().getBounds2D();
			obstacles += String.format(obstacleTextPattern, obstacleBounds.getX(), obstacleBounds.getY(), obstacleBounds.getX() + obstacleBounds.getWidth(), obstacleBounds.getY() + obstacleBounds.getHeight());
		}

		generatedCode = sources + targets + obstacles + agents;

		return generatedCode;
	}

	private void generatePathForScenarioElement(DynamicElement element) {
		float[] coords = new float[6];

		AffineTransform noTransformation = new AffineTransform();
		PathIterator it = element.getShape().getPathIterator(noTransformation);

		int n = 0;
		while (!it.isDone()) {
			int type = it.currentSegment(coords);
			System.out.println("segment type= " + type + " with coords: " + Arrays.toString(coords));

			n++;
			it.next();
		}

		System.out.println("path elements = " + n);
	}
}
