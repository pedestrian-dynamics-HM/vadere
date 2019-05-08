package org.vadere.meshing.utils.jts;

import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.triangulate.VoronoiDiagramBuilder;

import java.util.Collection;

public class VoronoiFactory {

	private GeometryFactory geometryFactory;

	public VoronoiFactory() {
		geometryFactory = new GeometryFactory();
	}

	public Geometry createVoronoiDiagram(final Collection<Coordinate> coordinates, final Collection<Polygon> polygons,
			final Envelope envolve) {
		VoronoiDiagramBuilder builder = new VoronoiDiagramBuilder();

		builder.setSites(coordinates);
		builder.setClipEnvelope(envolve);
		Geometry voronoiGeometry = builder.getDiagram(geometryFactory);

		for (Polygon polygon : polygons) {

			Geometry[] geoArray = new Geometry[voronoiGeometry.getNumGeometries()];
			for (int i = 0; i < voronoiGeometry.getNumGeometries(); i++) {
				geoArray[i] = voronoiGeometry.getGeometryN(i).difference(polygon);
			}

			voronoiGeometry = geometryFactory.createGeometryCollection(geoArray);
		}

		return voronoiGeometry;
	}

	public Geometry createVoronoiDiagram(final Collection<Coordinate> coordinates, final Envelope envolve) {
		VoronoiDiagramBuilder builder = new VoronoiDiagramBuilder();
		builder.setSites(coordinates);
		builder.setClipEnvelope(envolve);
		return builder.getDiagram(geometryFactory);
	}
}

