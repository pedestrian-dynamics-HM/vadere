package org.vadere.util.geometry.mesh.inter;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by bzoennchen on 04.04.17.
 */
public interface ITriangulationHierarchy<
		P extends IHierarchyPoint,
		E extends IHalfEdge<P>,
		F extends IFace<P>,
		T extends ITriangulation> {

	enum LOCATE_TYPE {}


	default P insert(final double x, final double y, LOCATE_TYPE locateType, F face, int li) {

		int vertexLevel = randomLevel();
		List<F> faces = locateInAll(x ,y);

		ITriangulation<P, E, F> triangulation = getLevel(0);
		P vertex = triangulation.getMesh().insertVertex(x, y);
		triangulation.insert(faces.get(0), vertex);

		P prev = vertex;
		P first = vertex;

		int level = 1;
		while(level <=  vertexLevel) {
			triangulation = getLevel(level);
			vertex = triangulation.getMesh().insertVertex(x, y);
			vertex.setDown(prev);// link with level above
			prev.setUp(vertex);
			prev = vertex;
			level++;
		}

		return first;
	}

	int randomLevel();

	int getMaxLevel();

	int getMinSize();

	IHierarchyPoint create(P vertex);


	ITriangulation<P, E, F> getLevel(int level);

	default List<F> locateInAll(double x, double y) {
		int level = getMaxLevel();
		F face;
		List<F> faces = new ArrayList<>(getMaxLevel());

		while(level > 0 && (getLevel(level).getMesh().getNumberOfVertices() < getMinSize() || getLevel(level).getDimension() < 2)) {
			level--;
		}

		for(int i = level+1; i < getMaxLevel(); i++) {
			//pos[i] = 0???
		}

		while(level > 0) {
			//getLevel(level).lo
			//faces.add(face);
		}
		return null;
	}
}
