package org.vadere.util.triangulation.adaptive;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.vadere.util.color.ColorHelper;
import org.vadere.util.geometry.mesh.gen.AFace;
import org.vadere.util.geometry.mesh.gen.AVertex;
import org.vadere.util.geometry.mesh.gen.PFace;
import org.vadere.util.geometry.mesh.inter.IFace;
import org.vadere.util.geometry.mesh.inter.IHalfEdge;
import org.vadere.util.geometry.mesh.inter.IMesh;
import org.vadere.util.geometry.mesh.inter.IVertex;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VLine;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.geometry.shapes.VTriangle;
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
 * Created by bzoennchen on 29.05.17.
 */
public class PSMeshingPanel<P extends IPoint, V extends IVertex<P>, E extends IHalfEdge<P>, F extends IFace<P>> extends Canvas {

    private static final Logger log = LogManager.getLogger(PSMeshingPanel.class);
	private IMesh<P, V, E, F> mesh;
	private double width;
	private double height;
	private Collection<F> faces;
    private final Predicate<F> alertPred;
    private Collection<VTriangle> triangles;

    public PSMeshingPanel(final IMesh<P, V, E, F> mesh, final Predicate<F> alertPred, final double width, final double height) {
        this.mesh = mesh;
        this.width = width;
        this.height = height;
        this.alertPred = alertPred;
        this.triangles = new ArrayList<>();
    }

    public void update() {
        faces = mesh.getFaces();
    }

	@Override
	public void paint(Graphics g) {

        synchronized (mesh) {
            update();
            Graphics2D graphics2D = (Graphics2D) g;
            BufferedImage image = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = (Graphics2D) image.getGraphics();


            //graphics.scale(3, 3);

            graphics.setColor(Color.WHITE);
            graphics.fill(new VRectangle(0, 0, getWidth(), getHeight()));
            Font currentFont = graphics.getFont();
            Font newFont = currentFont.deriveFont(currentFont.getSize() * 0.064f);
            graphics.setFont(newFont);
            graphics.setColor(Color.GRAY);
	       /* for(VShape obstacle : obstacles) {
		        graphics.fill(obstacle);
	        }*/

            graphics.translate(600, 200);
            graphics.scale(8, 8);
            graphics.setStroke(new BasicStroke(0.003f));
            graphics.setColor(Color.BLACK);
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            int max = 0;

            for(F face : faces) {
                if(face instanceof AFace) {
                    int sum = mesh.streamVertices(face).map(v -> (AVertex)v).mapToInt(v -> v.getId()).sum();
                    max = Math.max(sum, max);
                }
            }

            ColorHelper colorHelper = new ColorHelper(max);


            for(F face : faces) {
                VTriangle triangle = mesh.toTriangle(face);
                if(alertPred.test(face)) {
                    //log.info("red triangle");
                    graphics.setColor(Color.GRAY);
                    graphics.draw(triangle);
                    graphics.setColor(Color.RED);
                    graphics.fill(triangle);


                } else {
                    graphics.setColor(Color.GRAY);
                    graphics.draw(triangle);
                    if(face instanceof AFace) {
                        int sum = mesh.streamVertices(face).map(v -> (AVertex)v).mapToInt(v -> v.getId()).sum();
                        graphics.setColor(colorHelper.numberToColor(sum));
                        graphics.fill(triangle);
                    }

                }
            }
            /*graphics.setColor(Color.BLACK);
            Collection<V> points = mesh.getVertices();
            int id = 0;

            for(V v : points) {
                float dx = 0f;
                float delta = 0.6f;
                P p = mesh.getPoint(v);
                if(id >= 100) {
                    graphics.drawString((id / 100)+"", (float)p.getX()+dx, (float)p.getY());
                    dx += delta;
                }

                if(id >= 10) {
                    graphics.drawString(((id % 100) / 10)+"", (float)p.getX()+dx, (float)p.getY());
                    dx += delta;
                }

                graphics.drawString((id % 10)+"", (float)p.getX()+dx, (float)p.getY());


                id++;
            }*/

            //graphics.translate(5,5);
            graphics2D.drawImage(image, 0, 0, null);
        }

//            graphics.setColor(Color.RED);
//            tc.triangulation.getTriangles().parallelStream().forEach(graphics::draw);
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
