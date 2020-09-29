package org.vadere.meshing.utils.math;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.inter.IFace;
import org.vadere.meshing.mesh.inter.IHalfEdge;
import org.vadere.meshing.mesh.inter.IMesh;
import org.vadere.meshing.mesh.inter.IVertex;
import org.vadere.util.geometry.GeometryUtils;
import org.vadere.util.math.InterpolationUtil;

import java.util.function.Function;
import java.util.function.Predicate;

public class GeometryUtilsMesh {

	public static <V extends IVertex, E extends IHalfEdge, F extends IFace> double[] curvature(
			@NotNull final IMesh<V, E, F> mesh,
			@NotNull final V v, Function<V, Double> f) {
		return GeometryUtilsMesh.curvature(mesh, v, f, u -> true);
	}

	public static <V extends IVertex, E extends IHalfEdge, F extends IFace> double getArea(
			@NotNull final F face,
			@NotNull final IMesh<V, E, F> mesh) {
		return GeometryUtils.areaOfPolygon(mesh.getVertices(face));
	}

	public static <V extends IVertex, E extends IHalfEdge, F extends IFace> double barycentricInterpolation(
			@NotNull final F face,
	        @NotNull final IMesh<V, E, F> mesh,
			@NotNull final Function<V, Double> function,
			final double x,
			final double y) {

		E edge = mesh.getEdge(face);
		V v1 = mesh.getVertex(edge);
		V v2 = mesh.getVertex(mesh.getNext(edge));
		V v3 = mesh.getVertex(mesh.getPrev(edge));

		return InterpolationUtil.barycentricInterpolation(v1, v2, v3, function, x, y);
	}

	/**
	 * Computes and returns the mean and gaussian curvature.
	 *
	 * @param mesh
	 * @param v
	 * @param f
	 * @param valid
	 * @param <V>
	 * @param <E>
	 * @param <F>
	 * @return
	 */
	public static <V extends IVertex, E extends IHalfEdge, F extends IFace> double[] curvature(
			@NotNull final IMesh<V, E, F> mesh,
			@NotNull final V v,
			Function<V, Double> f,
			@NotNull final Predicate<V> valid) {

		double vx = mesh.getX(v);
		double vy = mesh.getY(v);
		double vz = f.apply(v);
		double alpha = 0;
		double beta = 0;
		double area = 0;


		for(E edge : mesh.getEdgeIt(v)) {
			if(!mesh.isAtBoundary(edge)) {
				V vi = mesh.getTwinVertex(edge);
				V vj = mesh.getTwinVertex(mesh.getTwin(mesh.getNext(edge)));
				V vk = mesh.getTwinVertex(mesh.getPrev(mesh.getTwin(edge)));

				double vix = mesh.getX(vi);
				double viy = mesh.getY(vi);
				double viz = f.apply(vi);

				double vjx = mesh.getX(vj);
				double vjy = mesh.getY(vj);
				double vjz = f.apply(vj);

				if(valid.test(vi) && valid.test(vj) && valid.test(vk)) {
					double eix = vix - vx;
					double eiy = viy - vy;
					double eiz = viz - vz;

					double ejx = vjx - vx;
					double ejy = vjy - vy;
					double ejz = vjz - vz;

					double ekx = mesh.getX(vk) - vx;
					double eky = mesh.getY(vk) - vy;
					double ekz = f.apply(vk) - vz;

					double alphai = GeometryUtils.angle3D(eix, eiy, eiz, ejx, ejy, ejz);
					double[] ni = new double[]{ 0, 0, 0 };
					double[] nk = new double[]{ 0, 0, 0 };

					GeometryUtils.cross(new double[]{ eix, eiy, eiz}, new double[]{ ejx, ejy, ejz}, ni);
					GeometryUtils.cross(new double[]{ ekx, eky, ekz }, new double[]{ eix, eiy, eiz}, nk);
					GeometryUtils.norm3D(ni);
					GeometryUtils.norm3D(nk);

					double betai = GeometryUtils.angle3D(nk[0], nk[1], nk[2], ni[0], ni[1], ni[2]);

					alpha += alphai;
					beta += Math.sqrt(eix * eix + eiy * eiy) * betai;
				}
			}
		}

		double gaussianCurvature = 2*Math.PI - alpha;
		double curvature = 1.0/2.0 * beta;

		//double meanGaussianCurvature = gaussianCurvature / area;
		//double meanMurvature = curvature / area;

		return new double[]{curvature, gaussianCurvature};
	}

	public static <V extends IVertex, E extends IHalfEdge, F extends IFace> double[] curvature(@NotNull final IMesh<V, E, F> mesh,
	                                                                                           @NotNull final Function<V, Double> f,
	                                                                                           @NotNull final Predicate<V> valid,
	                                                                                           @NotNull final E edge) {
		double alpha = 0;
		double beta = 0;
		V v = mesh.getVertex(edge);
		if(valid.test(v)) {
			double vx = mesh.getX(v);
			double vy = mesh.getY(v);
			double vz = f.apply(v);

			if(!mesh.isAtBoundary(edge)) {
				V vi = mesh.getTwinVertex(edge);
				V vj = mesh.getTwinVertex(mesh.getTwin(mesh.getNext(edge)));
				V vk = mesh.getTwinVertex(mesh.getPrev(mesh.getTwin(edge)));

				double vix = mesh.getX(vi);
				double viy = mesh.getY(vi);
				double viz = f.apply(vi);

				double vjx = mesh.getX(vj);
				double vjy = mesh.getY(vj);
				double vjz = f.apply(vj);

				if(valid.test(vi) && valid.test(vj) && valid.test(vk)) {
					double eix = vix - vx;
					double eiy = viy - vy;
					double eiz = viz - vz;

					double ejx = vjx - vx;
					double ejy = vjy - vy;
					double ejz = vjz - vz;

					double ekx = mesh.getX(vk) - vx;
					double eky = mesh.getY(vk) - vy;
					double ekz = f.apply(vk) - vz;

					alpha = GeometryUtils.angle3D(eix, eiy, eiz, ejx, ejy, ejz);
					double[] ni = new double[]{ 0, 0, 0 };
					double[] nk = new double[]{ 0, 0, 0 };

					GeometryUtils.cross(new double[]{ eix, eiy, eiz}, new double[]{ ejx, ejy, ejz}, ni);
					GeometryUtils.cross(new double[]{ ekx, eky, ekz }, new double[]{ eix, eiy, eiz}, nk);
					GeometryUtils.norm3D(ni);
					GeometryUtils.norm3D(nk);

					double betai = GeometryUtils.angle3D(nk[0], nk[1], nk[2], ni[0], ni[1], ni[2]);
					beta = 1/2 * Math.sqrt(eix * eix + eiy * eiy) * betai;
				}
			}
		}

		return new double[]{alpha, beta};
	}

	/*
	public static <V extends IVertex, E extends IHalfEdge, F extends IFace> double[] curvature(
			@NotNull final IMesh<V, E, F> mesh,
			@NotNull final V v, Function<V, Double> f,
			@NotNull final Predicate<V> valid) {

		double vx = mesh.getX(v);
		double vy = mesh.getY(v);
		double vz = f.apply(v);
		double alpha = 0;
		double beta = 0;
		double area = 0;


		for(E edge : mesh.getEdgeIt(v)) {
			if(!mesh.isAtBoundary(edge)) {
				V vi = mesh.getTwinVertex(edge);
				V vj = mesh.getTwinVertex(mesh.getTwin(mesh.getNext(edge)));
				V vk = mesh.getTwinVertex(mesh.getPrev(mesh.getTwin(edge)));
				V vn = mesh.getVertex(mesh.getNext(edge));

				double vix = mesh.getX(vi);
				double viy = mesh.getY(vi);
				double viz = f.apply(vi);

				double vjx = mesh.getX(vj);
				double vjy = mesh.getY(vj);
				double vjz = f.apply(vj);

				double vnx = mesh.getX(vn);
				double vny = mesh.getY(vn);
				double vnz = f.apply(vn);

				if(valid.test(vi) && valid.test(vj) && valid.test()) {
					double eix = vix - vx;
					double eiy = viy - vy;
					double eiz = viz - vz;

					double ejx = vjx - vx;
					double ejy = vjy - vy;
					double ejz = vjz - vz;

					double ekx = mesh.getX(vk) - vx;
					double eky = mesh.getY(vk) - vy;
					double ekz = f.apply(vk) - vz;

					double enx = vnx - vx;
					double eny = vny - vy;
					double enz = vnz - vz;

					double bx = enx / 3.0 + eix / 3.0 + vx / 3.0;
					double by = eny / 3.0 + eiy / 3.0 + vy / 3.0;
					double bz = enz / 3.0 + eiz / 3.0 + vz / 3.0;

					double areai = GeometryUtils.areaOfPolygon(new double[]{vnx, vix, bx, vjx}, new double[]{vny, viy, by, vjy}, new double[]{vnz, viz, bz, vjz});

					double alphai = GeometryUtils.angle3D(eix, eiy, eiz, ejx, ejy, ejz);
					double[] ni = new double[]{ 0, 0, 0 };
					double[] nk = new double[]{ 0, 0, 0 };

					GeometryUtils.cross(new double[]{ eix, eiy, eiz}, new double[]{ ejx, ejy, ejz}, ni);
					GeometryUtils.cross(new double[]{ ekx, eky, ekz }, new double[]{ eix, eiy, eiz}, nk);
					GeometryUtils.norm3D(ni);
					GeometryUtils.norm3D(nk);

					double betai = GeometryUtils.angle3D(nk[0], nk[1], nk[2], ni[0], ni[1], ni[2]);

					alpha += alphai;
					beta += Math.sqrt(eix * eix + eiy * eiy) * betai;
					area += areai;
				}
			}
		}

		double gaussianCurvature = 2*Math.PI - alpha;
		double curvature = 1.0/4.0 * beta;

		//double meanGaussianCurvature = gaussianCurvature / area;
		//double meanMurvature = curvature / area;

		return new double[]{curvature, gaussianCurvature};
	}
	 */

}
