package org.vadere.meshing.mesh.impl;

import org.jetbrains.annotations.NotNull;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VPoint;

import javax.annotation.Nullable;

public class DataPoint<D> implements IPoint {

	private IPoint point;
	private @Nullable D data;

	public DataPoint(@NotNull final IPoint point) {
		this.point = point;
	}

	public DataPoint(@NotNull final double x, final double y) {
		this.point = new VPoint(x, y);
	}


	@Nullable
	public D getData() {
		return data;
	}

	public void setData(@Nullable final D data) {
		this.data = data;
	}

	@Override
	public double getX() {
		return point.getX();
	}

	@Override
	public double getY() {
		return point.getY();
	}

	@Override
	public IPoint add(IPoint point) {
		return this.point.add(point);
	}

	@Override
	public IPoint add(double x, double y) {
		return point.add(x, y);
	}

	@Override
	public IPoint addPrecise(IPoint point) {
		return this.point.addPrecise(point);
	}

	@Override
	public IPoint subtract(IPoint point) {
		return this.point.subtract(point);
	}

	@Override
	public IPoint multiply(IPoint point) {
		return this.point.multiply(point);
	}

	@Override
	public IPoint scalarMultiply(double factor) {
		return this.point.scalarMultiply(factor);
	}

	@Override
	public IPoint rotate(double radAngle) {
		return this.point.rotate(radAngle);
	}

	@Override
	public double scalarProduct(IPoint point) {
		return this.point.scalarProduct(point);
	}

	@Override
	public IPoint norm() {
		return this.point.norm();
	}

	@Override
	public IPoint norm(double len) {
		return this.point.norm(len);
	}

	@Override
	public IPoint normZeroSafe() {
		return this.point.normZeroSafe();
	}

	@Override
	public double distance(IPoint other) {
		return this.point.distance(other);
	}

	@Override
	public double distance(double x, double y) {
		return this.point.distance(x, y);
	}

	@Override
	public double distanceSq(IPoint other) {
		return this.point.distanceSq(other);
	}

	@Override
	public double distanceSq(double x, double y) {
		return this.point.distanceSq(x, y);
	}

	@Override
	public double distanceToOrigin() {
		return this.point.distanceToOrigin();
	}

	@Override
	public IPoint clone() {
		DataPoint<D> dataPoint = new DataPoint<>(this.point);
		dataPoint.setData(this.getData());
		return dataPoint;
	}

	@Override
	public String toString() {
		return this.point.toString();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof DataPoint))
			return false;

		IPoint other = (IPoint) obj;

		if (this.getX() != other.getX())
			return false;
		if (this.getY() != other.getY())
			return false;

		return true;
	}
}
