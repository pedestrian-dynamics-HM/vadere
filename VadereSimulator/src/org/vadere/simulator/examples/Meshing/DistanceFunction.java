package org.vadere.simulator.examples.Meshing;

import org.vadere.meshing.examples.ElementSizeFunction;
import org.vadere.meshing.mesh.gen.PMesh;
import org.vadere.meshing.mesh.impl.PSLG;
import org.vadere.meshing.mesh.triangulation.DistanceFunctionApproxBF;
import org.vadere.meshing.utils.io.poly.PSLGGenerator;
import org.vadere.util.math.IDistanceFunction;

import java.io.IOException;
import java.io.InputStream;

public class DistanceFunction {
	public static void main(String ... args) throws IOException {
		fmm();
	}

	private static void bruteForce() throws IOException {
		String fileName = "/poly/kaiserslautern.poly";
		final InputStream inputStream = ElementSizeFunction.class.getResourceAsStream("/poly/kaiserslautern.poly");

		System.out.println(String.format("Meshing %s...", fileName));

		PSLG pslg = PSLGGenerator.toPSLG(inputStream);
		System.out.println("minX = " + pslg.getAllPoints().stream().mapToDouble(p -> p.getX()).min().getAsDouble());
		System.out.println("minY = " + pslg.getAllPoints().stream().mapToDouble(p -> p.getY()).min().getAsDouble());
		System.out.println("maxX = " + pslg.getAllPoints().stream().mapToDouble(p -> p.getX()).max().getAsDouble());
		System.out.println("maxY = " + pslg.getAllPoints().stream().mapToDouble(p -> p.getY()).max().getAsDouble());
		DistanceFunctionApproxBF distanceFunctionApproxBF = new DistanceFunctionApproxBF(pslg, IDistanceFunction.create(pslg.getSegmentBound(), pslg.getHoles()), () -> new PMesh());
		distanceFunctionApproxBF.printPython();
	}

	private static void fmm() throws IOException {
		String fileName = "/poly/kaiserslautern.poly";
		final InputStream inputStream = ElementSizeFunction.class.getResourceAsStream("/poly/kaiserslautern.poly");

		System.out.println(String.format("Meshing %s...", fileName));

		PSLG pslg = PSLGGenerator.toPSLG(inputStream);
		System.out.println("minX = " + pslg.getAllPoints().stream().mapToDouble(p -> p.getX()).min().getAsDouble());
		System.out.println("minY = " + pslg.getAllPoints().stream().mapToDouble(p -> p.getY()).min().getAsDouble());
		System.out.println("maxX = " + pslg.getAllPoints().stream().mapToDouble(p -> p.getX()).max().getAsDouble());
		System.out.println("maxY = " + pslg.getAllPoints().stream().mapToDouble(p -> p.getY()).max().getAsDouble());
		DistanceFunctionApproxFMM distanceFunctionApproxBF = new DistanceFunctionApproxFMM(pslg);
		distanceFunctionApproxBF.printPython();
	}
}
