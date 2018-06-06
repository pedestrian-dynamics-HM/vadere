package org.vadere.util.triangulation.adaptive;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.vadere.util.color.ColorHelper;
import org.vadere.util.debug.gui.ColorFunctions;
import org.vadere.util.geometry.mesh.gen.AFace;
import org.vadere.util.geometry.mesh.gen.AMesh;
import org.vadere.util.geometry.mesh.gen.AVertex;
import org.vadere.util.geometry.mesh.gen.PFace;
import org.vadere.util.geometry.mesh.inter.IFace;
import org.vadere.util.geometry.mesh.inter.IHalfEdge;
import org.vadere.util.geometry.mesh.inter.IMesh;
import org.vadere.util.geometry.mesh.inter.IVertex;
import org.vadere.util.geometry.shapes.*;
import org.vadere.util.math.SpaceFillingCurve;
import org.vadere.util.triangulation.improver.IMeshImprover;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.swing.*;

/**
 * @author Benedikt Zoennchen
 *
 * @param <P>
 * @param <V>
 * @param <E>
 * @param <F>
 */
public class PSMeshingPanel<P extends IPoint, V extends IVertex<P>, E extends IHalfEdge<P>, F extends IFace<P>> extends Canvas {

    private static final Logger log = LogManager.getLogger(PSMeshingPanel.class);
	private IMesh<P, V, E, F> mesh;
	private double width;
	private double height;
	private Collection<F> faces;
    private final Predicate<F> alertPred;
    private Collection<VTriangle> triangles;
    private VRectangle bound;
    private final double scale;
    private ColorFunctions<P, V, E, F> colorFunctions;

    public PSMeshingPanel(final IMesh<P, V, E, F> mesh, final Predicate<F> alertPred, final double width, final double height, final VRectangle bound) {
        this.mesh = mesh;
        this.width = width;
        this.height = height;
        this.alertPred = alertPred;
        this.triangles = new ArrayList<>();
        this.bound = bound;
        this.scale = Math.min(width / bound.getWidth(), height / bound.getHeight());
	    this.faces = new ArrayList<>();
	    this.colorFunctions = new ColorFunctions<>();
    }

	@Override
	public void update(Graphics g) {
    	// TODO clone it!
		synchronized (mesh) {
			faces = mesh.clone().getFaces();
			super.update(g);
		}

	}

	@Override
	public void paint(Graphics g) {
    	// double buffering => draw into an image
		Graphics2D graphics2D = (Graphics2D) g;
		BufferedImage image = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);
		Graphics2D graphics = (Graphics2D) image.getGraphics();

		graphics.setColor(Color.WHITE);
		graphics.fill(new VRectangle(0, 0, getWidth(), getHeight()));
		Font currentFont = graphics.getFont();
		Font newFont = currentFont.deriveFont(currentFont.getSize() * 0.064f);
		graphics.setFont(newFont);
		graphics.setColor(Color.GRAY);
		graphics.scale(scale, scale);
		graphics.translate(-bound.getMinX()+(0.5*Math.max(0, bound.getWidth()-bound.getHeight())), -bound.getMinY() + (bound.getHeight()-height / scale));
		graphics.setStroke(new BasicStroke(0.003f));
		graphics.setColor(Color.BLACK);
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);


		int groupSize = 64;
		ColorHelper colorHelper = new ColorHelper(faces.size());

		for(F face : faces) {
			try {
				try {
					VPolygon polygon = mesh.toPolygon(face);
					graphics.setColor(colorFunctions.qualityToGrayScale(mesh, face));
					graphics.setColor(Color.GRAY);
					graphics.fill(polygon);
					graphics.draw(polygon);
					graphics.setColor(Color.RED);
					graphics.draw(polygon);
				}
				catch (NullPointerException ne) {
					VPolygon polygon = mesh.toPolygon(face);
				}

				//if(alertPred.test(face)) {
					//log.info("red triangle");


					//graphics.setColor(Color.WHITE);
					//graphics.draw(polygon);

				//} /*else if(!mesh.isBoundary(face)) {
					/*if(face instanceof AFace) {

						int bucket = ((AFace)face).getId() / groupSize;

						graphics.setColor(colorHelper.numberToColor(((AFace)face).getId()));
						graphics.fill(polygon);
					}
				}*/
				/*else {
					graphics.setColor(Color.BLACK);
					graphics.draw(polygon);
				}*/

			}
			catch (ArrayIndexOutOfBoundsException e) {
				log.error("could not paint a face + " + face);
			}

		}

		graphics2D.drawImage(image, 0, 0, null);
	}


	public double triangleToQuality(final VTriangle triangle) {

		VLine[] lines = triangle.getLines();
		double a = lines[0].length();
		double b = lines[1].length();
		double c = lines[2].length();
		double part = 0.0;
		if(a != 0.0 && b != 0.0 && c != 0.0) {
			part = ((b + c - a) * (c + a - b) * (a + b - c)) / (a * b * c);
		}
		else {
			throw new IllegalArgumentException(triangle + " is not a feasible triangle!");
		}
		return part;
	}

	public JFrame display() {
		JFrame jFrame = new JFrame();
		jFrame.setSize((int)width+10, (int)height+10);
		jFrame.add(this);
		jFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		setVisible(true);
		return jFrame;
	}
}
