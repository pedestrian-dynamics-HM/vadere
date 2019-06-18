package org.vadere.meshing.utils.debug;


import org.vadere.meshing.mesh.inter.IFace;
import org.vadere.meshing.mesh.inter.IHalfEdge;
import org.vadere.meshing.mesh.inter.IMesh;
import org.vadere.meshing.mesh.inter.IVertex;
import org.vadere.meshing.utils.color.ColorFunctions;
import org.vadere.meshing.utils.io.tex.TexGraphBuilder;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.logging.Logger;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.LinkedList;
import java.util.function.Consumer;

/**
 * A {@link TriCanvas} is responsible to build jawa.swing and tex tizk visualization for a specific
 * purpose at some point within an triangulation algorithm.
 *
 * With the {@link ColorFunctions} field colors for visualization can be set depending on different functions.
 * The paintDecorator Consumer list and texGraphBuilder are used to manipulate what is drawn.
 *
 * With the stateLog consumer information for the specifc state within the algorithm can be printed
 * from the {@link DebugGui}.
 *
 * @param <P> the type of the points (containers)
 * @param <CE> the type of container of the half-edges
 * @param <CF> the type of the container of the faces
 * @param <V> the type of the vertices
 * @param <E> the type of the half-edges
 * @param <F> the type of the faces
 */
public abstract class TriCanvas
		<P extends IPoint, CE, CF, V extends IVertex, E extends IHalfEdge, F extends IFace>
		extends Canvas {

	static final Logger log = Logger.getLogger(TriCanvas.class);
	static final VRectangle defaultBound = new VRectangle(-12, -12, 24, 24);
	static final int defaultWidth = 1000;
	static final int defaultHeight = 1000;

	protected final IMesh<V, E, F> mesh;
	public double width;
	public double height;
	protected VRectangle bound;
	protected double scale;
	ColorFunctions<P, CE, CF, V, E, F> colorFunctions;
	LinkedList<Consumer<Graphics2D>> paintDecorator;
	private TexGraphBuilder<P, CE, CF, V, E, F> texGraphBuilder;
	private Consumer<StringBuilder> stateLog;


	public TriCanvas(final IMesh<V, E, F> mesh) {
		this(mesh, defaultWidth, defaultHeight, defaultBound);
	}

	public TriCanvas(final IMesh<V, E, F> mesh, final double width, final double height) {
		this(mesh, width, height, defaultBound);
	}

	public TriCanvas(final IMesh<V, E, F> mesh, final double width, final double height, final VRectangle bound) {
		this.width = width;
		this.height = height;
		this.bound = bound;
		this.scale = Math.min(width / bound.getWidth(), height / bound.getHeight());
		this.paintDecorator = new LinkedList<Consumer<Graphics2D>>();
		this.colorFunctions = new ColorFunctions<>();
		this.mesh = mesh;
		this.texGraphBuilder = new TexGraphBuilder<>(mesh, 0.5f, colorFunctions);
		this.stateLog = sb -> sb.append(mesh.toString());
	}

	/**
	 * Default paint method for a java.swing canvas. Do not change this function add additional
	 * elements to the paintDecorator List which will be called from this method.
	 */
	@Override
	public void paint(Graphics g) {
		Graphics2D graphics2D = (Graphics2D) g;
		BufferedImage image = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);
		Graphics2D graphics = image.createGraphics();

		graphics.setColor(Color.WHITE);
		graphics.fill(new VRectangle(0, 0, getWidth(), getHeight()));
		Font currentFont = graphics.getFont();
		Font newFont = currentFont.deriveFont(currentFont.getSize() * 0.010f);
		graphics.setFont(newFont);
		graphics.setColor(Color.GRAY);
		graphics.scale(scale, scale);
		graphics.translate(-bound.getMinX() + (0.5 * Math.max(0, bound.getWidth() - bound.getHeight())), -bound.getMinY() + (bound.getHeight() - height / scale));
		graphics.setStroke(new BasicStroke(0.001f));
		graphics.setColor(Color.BLACK);
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		//Add specif elements to image
		paintDecorator.forEach(c -> c.accept(graphics));

		graphics2D.drawImage(image, 10, 10, null);
	}

	/**
	 * State Information which can be printed from the {@link DebugGui}
	 *
	 * @return StringBuilder which can be printed to the console.
	 */
	public StringBuilder logOnGuiUpdate() {
		return null;
	}

	/**
	 * Add additional graphics primitive which should be drawn to the canvas.
	 * @param c a Consumer {@link Consumer} of {@link Graphics2D}
	 * @return this canvas
	 */
	public TriCanvas<P, CE, CF, V, E, F> addGuiDecorator(Consumer<Graphics2D> c) {
		paintDecorator.add(c);
		return this;
	}

	/**
	 * Add a complete line which will be included in the tikzdrawing. Note: add a newline character!
	 * @param c a Consumer {@link Consumer} of {@link StringBuilder}
	 * @return this canvas
	 */
	public TriCanvas<P, CE, CF, V, E, F> addTexDecorator(Consumer<StringBuilder> c) {
		texGraphBuilder.addElement(c);
		return this;
	}

	/**
	 * Get current {@link ColorFunctions} object used for java.swing and tex tikz drawing.
	 * @return the current used color function {@link ColorFunctions}
	 */
	public ColorFunctions getColorFunctions() {
		return colorFunctions;
	}

	/**
	 * Add new {@link ColorFunctions} object to the {@link TriCanvas} implementation.
	 * @param colorFunctions the new color function
	 */
	public void setColorFunctions(ColorFunctions<P, CE, CF, V, E, F> colorFunctions) {
		this.colorFunctions = colorFunctions;
	}

	public Consumer<StringBuilder> getStateLog() {
		return stateLog;
	}

	public void setStateLog(Consumer<StringBuilder> c) {
		stateLog = c;
	}

	public IMesh<V, E, F> getMesh() {
		return mesh;
	}

	public TexGraphBuilder<P, CE, CF, V, E, F> getTexGraphBuilder() {
		return texGraphBuilder;
	}
}
