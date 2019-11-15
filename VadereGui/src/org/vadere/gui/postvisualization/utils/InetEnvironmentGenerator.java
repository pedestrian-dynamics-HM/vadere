package org.vadere.gui.postvisualization.utils;

import org.vadere.gui.components.model.DefaultSimulationConfig;
import org.vadere.gui.components.model.SimulationModel;
import org.vadere.gui.components.view.SimulationRenderer;
import org.vadere.state.scenario.Obstacle;
import org.vadere.state.scenario.Topography;
import org.vadere.util.geometry.Vector3D;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VPolygon;
import org.vadere.util.geometry.shapes.VShape;
import org.vadere.util.logging.Logger;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

public class InetEnvironmentGenerator {

	private final static Logger logger = Logger.getLogger(InetEnvironmentGenerator.class);
	private final SimulationRenderer renderer;
	private final SimulationModel<? extends DefaultSimulationConfig> model;

	public InetEnvironmentGenerator(final SimulationRenderer renderer,
									final SimulationModel<? extends  DefaultSimulationConfig> model){
		this.renderer = renderer;
		this.model = model;
	}

	public void generateInetEnvironment(final File file){
		AffineTransform affTransformation = new AffineTransform();
		affTransformation.scale(1.0, -1.0);
		affTransformation.translate(0.0, -1*model.getTopography().getBounds().getHeight());


		StringBuilder sb = new StringBuilder();
		sb.append("<environment>").append("\n");
		createObjectsWithShape(affTransformation, sb);
		sb.append("</environment>").append("\n");
		// TODO: maybe uses Java's resources notation (in general, writing the file should be done by the caller not here).
		try {
			file.createNewFile();
			Writer out = new OutputStreamWriter(new FileOutputStream(file), "UTF-8");
			out.write(sb.toString());
			out.flush();
			logger.info("generate new INET environment file: " + file.getAbsolutePath());
		} catch (IOException e1) {
			logger.error(e1.getMessage());
			e1.printStackTrace();
		}
	}

	private void createObjectsWithShape(AffineTransform affTransformation , StringBuilder sb){

		List<Obstacle> obstacles = new ArrayList<>();
		if (!model.getTopography().hasBoundary()){
			obstacles.addAll(Topography.createObstacleBoundary(model.getTopography()));
		}
		obstacles.addAll(model.getTopography().getObstacles());

		int maxId = obstacles.stream().map(Obstacle::getId).max(Integer::compareTo).get() + 1;

		for(Obstacle o : obstacles){
			// set id for obstacles without any id.
			if (o.getId() == -1){
				o.setId(maxId);
				maxId++;
			}
			// flip y-axis of shapes
			VShape transformedShape = new VPolygon(new Path2D.Double(o.getShape(), affTransformation));
			EnvObject envObject = EnvObject.createPrism(o.getId(), transformedShape, 5.0, new Color(200,0,0));
			sb.append(envObject.xml()).append("\n");
		}
	}

	enum PosType {
		MIN("min"),
		MAX("max"),
		CENTER("center");

		private String tag;
		PosType(String tag){
			this.tag = tag;
		}

	}

	enum ShapeType{
		SPHERE("sphere"),
		CUBIOD("cuboid"),
		PRISM("prism"),
		POLYH("polyhedron"),
		;

		private String tag;
		ShapeType(String tag){
			this.tag = tag;
		}
	}

	private static class EnvObject{

		private int id;
		private Vector3D orientation;
		private Vector3D position;
		private PosType positionType;
		private double[] shape;
		private ShapeType shapeType;
		private String material;
		private Color fillColor;
		private double opacity;
		private String texture;

		static EnvObject createPrism(int id, VShape shape, double height, Color fillColor){
			EnvObject envObject = new EnvObject();
			envObject.id = id;
			envObject.shapeType = ShapeType.PRISM;
			envObject.positionType = PosType.MIN;
			envObject.orientation = new Vector3D(0.0, 0.0, 0.0);
			envObject.material = "brick";
			envObject.fillColor = fillColor;
			envObject.opacity = 0.75;
			envObject.texture = "brick.jpg";
			envObject.buildPrismShape(shape, height);
			return envObject;
		}

		private VPoint pos2D(){
			return new VPoint(position.x, position.y);
		}

		private void buildPrismShape(VShape transformedShape, double height){

			List<VPoint> points = transformedShape.getPath();
			VPoint base = new VPoint(transformedShape.getBounds2D().getMinX(), transformedShape.getBounds2D().getMinY());
			position = new Vector3D(base.x, base.y, 0.0);

			// get 2D representation of position point. Negative to use as base.
			VPoint negPos2d = pos2D().scalarMultiply(-1.0);
			this.shape = new double[points.size() *2 + 1];
			this.shape[0] = height;
			int idx = 1;
			for (VPoint p : points){
				VPoint relPoint = negPos2d.addPrecise(p);
				this.shape[idx] = relPoint.x;
				this.shape[idx+1] = relPoint.y;
				idx+=2;
			}
		}
		public String xml(){
			String sb = "<object " +
					"id=\"" + id + "\" " +
					"orientation=\"" + str(orientation) + "\" " +
					"position=\"" + positionType.tag + " " + str(position) + "\" " +
					"shape=\"" + shapeType.tag + " " + str(shape) + "\" " +
					"material=\"" + material + "\" " +
					"fill-color=\"" + str(fillColor) + "\" " +
					"opacity=\"" + str(opacity) + "\" " +
					"texture=\"" + texture + "\" " +
					"/>";
			return sb;
		}

		private String str(Vector3D v){
			return String.format("%f %f %f", v.x, v.y, v.z);
		}

		private String str(double d){
			return String.format("%f", d);
		}

		private String str(double[] data){
			StringBuilder sb = new StringBuilder();
			for (double datum : data) {
				sb.append(String.format("%f", datum)).append(" ");
			}
			sb.delete(sb.length()-1, sb.length());
			return sb.toString();
		}

		private String str(Color col){
			return col.getRed() + " " + col.getBlue() + " " + col.getGreen();
		}

		private EnvObject(){

		}
	}
}
