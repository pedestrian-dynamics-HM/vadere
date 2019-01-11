package org.vadere.s2ucre;

import com.google.protobuf.Timestamp;

import org.jetbrains.annotations.NotNull;
import org.vadere.s2ucre.generated.Common;
import org.vadere.s2ucre.generated.IosbOutput;
import org.vadere.s2ucre.generated.Pedestrian;
import org.vadere.util.geometry.Vector2D;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VPolygon;

import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;


public class Utils {

	public static Timestamp addSeconds(@NotNull final Timestamp timestamp, final double seconds) {
		return Timestamp.newBuilder()
				.setSeconds(timestamp.getSeconds() + (long)seconds)
				.setNanos((int)((seconds-(long)seconds) * 1_000_000.0))
				.build();
	}

	/*
	Timestamp timestamp = Timestamp.newBuilder().setSeconds(millis / 1000)
//         .setNanos((int) ((millis % 1000) * 1000 000)).build();
	 */

	public static double toSeconds(@NotNull final Timestamp timestamp) {
		return timestamp.getSeconds() +  timestamp.getNanos() / 1_000_000.0;
	}

	public static Vector2D toVelocity(@NotNull final IosbOutput.AreaInfoAtTime msg) {
		return new Vector2D(msg.getVector().getDirection().getX(), msg.getVector().getDirection().getY());
	}

	public static Vector2D toVelocity(@NotNull final Common.Point3D point3D) {
		return new Vector2D(point3D.getX(), point3D.getY());
	}

	public static VPoint toVPoint(@NotNull final Common.UTMCoordinate coordinate) {
		return new VPoint(coordinate.getEasting(), coordinate.getNorthing());
	}

	public static Common.UTMCoordinate toUTMCoordinate(final VPoint point, final Common.UTMCoordinate reference) {
		Common.UTMCoordinate coordinate = Common.UTMCoordinate.newBuilder()
				.setDate(reference.getDate())
				.setDateValue(reference.getDateValue())
				.setElevation(reference.getElevation())
				.setHemisphere(reference.getHemisphere())
				.setZone(reference.getZone())
				.setEasting(point.getX())
				.setNorthing(point.getY()).build();

		return coordinate;
	}

	public static VPolygon toPolygon(final IosbOutput.AreaInfoAtTime msg) {
		Common.RegionOfInterest regionOfInterest = msg.getPolygon();

		Path2D.Double path = new Path2D.Double();
		boolean first = true;
		for(Common.UTMCoordinate coord : regionOfInterest.getCoordinateList()) {
			if(first) {
				first = false;
				path.moveTo(coord.getEasting(), coord.getNorthing());
			}
			else {
				path.lineTo(coord.getEasting(), coord.getNorthing());
			}
		}
		path.closePath();
		return new VPolygon(path);
	}

	public static List<Pedestrian.PedMsg> toPedMsg(@NotNull final IosbOutput.AreaInfoAtTime msg, @NotNull final Random random) {

		List<Pedestrian.PedMsg> pedMsgs = new ArrayList<>();
		int n =  (int)Math.round(msg.getPedestrians());
		List<VPoint> randomPositions = Utils.generateRandomPositions(Utils.toPolygon(msg), random, n);

		for(int id = 1; id < n + 1; id++) {
			VPoint pos = randomPositions.get(id-1);
			Pedestrian.PedMsg pedMsg = Pedestrian.PedMsg.newBuilder()
					.setPedId(id)
					.setPosition(Utils.toUTMCoordinate(pos, msg.getPolygon().getCoordinate(0)))
					.setVelocity(msg.getVector().getDirection())
					.setTime(msg.getTime()).build();
			pedMsgs.add(pedMsg);
		}

		return pedMsgs;
	}

	public static List<VPoint> generateRandomPositions(@NotNull final VPolygon polygon, @NotNull final Random random, int n) {
		List<VPoint> randomPoints = new ArrayList<>();
		for(int i = 0; i < n; i++) {
			VPoint randomPoint;
			do {
				Rectangle2D bound = polygon.getBounds2D();
				randomPoint = new VPoint(bound.getMinX() + random.nextDouble() * bound.getWidth(), bound.getMinY() + random.nextDouble() * bound.getHeight());
			}
			while (!polygon.contains(randomPoint));

			// TODO: remove hack if the iosb coordinates are correct!
			randomPoints.add(randomPoint);
			//randomPoints.add(randomPoint.add(new VPoint(11,0)));
		}

		return randomPoints;
	}
}
