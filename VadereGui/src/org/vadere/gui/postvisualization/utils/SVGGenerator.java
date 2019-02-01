package org.vadere.gui.postvisualization.utils;

import org.apache.batik.dom.GenericDOMImplementation;
import org.apache.batik.svggen.SVGGraphics2D;
import org.vadere.gui.components.model.DefaultSimulationConfig;
import org.vadere.gui.components.model.SimulationModel;
import org.vadere.gui.components.view.SimulationRenderer;
import org.vadere.util.logging.Logger;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;

public class SVGGenerator {

	private static Logger logger = Logger.getLogger(SVGGenerator.class);
	private SimulationRenderer renderer;
	private final SimulationModel<? extends DefaultSimulationConfig> model;

	public SVGGenerator(final SimulationRenderer renderer,
			final SimulationModel<? extends DefaultSimulationConfig> model) {
		this.renderer = renderer;
		this.model = model;
	}

	public void generateSVG(final File file) {

		// Get a DOMImplementation.
		DOMImplementation domImpl = GenericDOMImplementation.getDOMImplementation();

		// Create an instance of org.w3c.dom.Document.
		String svgNS = "http://www.w3.org/2000/svg";
		Document document = domImpl.createDocument(svgNS, "svg", null);

		// Create an instance of the SVG Generator.
		SVGGraphics2D svgGenerator = new SVGGraphics2D(document);

		// Ask the scenario to renderPostTransformation into the SVG Graphics2D implementation.
		// The logo can not be converted into a svg:
		// TODO [priority=low] [task=feature] convert the svg version of the logo. This might be complicated!
		boolean showLogo = model.config.isShowLogo();
		model.config.setShowLogo(false);
		int width = ImageGenerator.calculateOptimalWidth(model);
		int height = ImageGenerator.calculateOptimalHeight(model);
		svgGenerator.setClip(0, 0, width, height);
		renderer.render(svgGenerator, width, height);
		model.config.setShowLogo(showLogo);

		// Finally, stream out SVG to the standard output using
		// UTF-8 encoding.
		Writer out;
		try {
			file.createNewFile();
			out = new OutputStreamWriter(new FileOutputStream(file), "UTF-8");
			svgGenerator.stream(out, false);
			logger.info("generate new svg: " + file.getAbsolutePath());
		} catch (IOException e1) {
			logger.error(e1.getMessage());
			e1.printStackTrace();
		}

	}
}
