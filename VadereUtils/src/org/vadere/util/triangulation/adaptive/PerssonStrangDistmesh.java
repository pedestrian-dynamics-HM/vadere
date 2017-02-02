package org.vadere.util.triangulation.adaptive;


import org.vadere.util.triangulation.BowyerWatson3;
import org.vadere.util.geometry.shapes.VLine;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.geometry.shapes.VShape;
import org.vadere.util.geometry.shapes.VTriangle;

import java.awt.geom.PathIterator;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.lang.Math.pow;

public class PerssonStrangDistmesh {
	private List<VPoint> points = new ArrayList<>();
	private List<VPoint> oldPoints = new ArrayList<>();
	private BowyerWatson3 triangulation;
	private Function<VPoint, Double> fd;
	private Function<VPoint, Double> fh;

	// Parameters
	private double h0;
	private double geps;
	private double deps;

	/*
	Konstruktor für den Algorithmus von Persson und Strang
	 */
	public PerssonStrangDistmesh(VRectangle box,
	                             Collection<? extends VShape> obstacles,
	                             double h0,
	                             boolean uniform,
	                             Function<VPoint, Double> density,
	                             String method) {
		long now = System.currentTimeMillis();
		this.h0 = h0;
		this.geps = .001*h0;
		this.deps = 1.4901e-8*h0;
		double MAXDENSITY = calculateMaxDensity(density, box);
		fd = v -> {
			double value = box.distance(v);
			for (VShape obstacle : obstacles) {
				value = doDDiff(value, obstacle.distance(v));
			}
			return value;
		};

		if(uniform)
		{
			fh = v -> 1.0;
		} else {
			fh = v -> {
				double result;
				switch (method) {
					case "Distmesh":
						result = 0.15 - 0.2 * box.distance(v);
						double last = -box.distance(v);
						for (VShape obstacle : obstacles) {
							if (Math.max(box.getWidth(), box.getHeight()) <= 10) {
								result = doDUnion(result, 0.06 + 0.2 * obstacle.distance(v));
								last += obstacle.distance(v);
							} else {
								result = doDUnion(result, 0.06 + 0.2 * obstacle.distance(v) * 10 / Math.max(box.getWidth(), box.getHeight()));
								last += obstacle.distance(v) * 10 / Math.max(box.getWidth(), box.getHeight());
							}
						}
						last /= obstacles.size();
						result = doDUnion(result, last);
						break;
					case "Density":
						result = 1 / (Parameters.MINIMUM + (density.apply(v) / MAXDENSITY) * Parameters.DENSITYWEIGHT);
						break;
					default:
						throw new RuntimeException("Method not accepted");
				}
				return result;
			};
		}
		generatePoints(box);
		removePointsAndRejection();
		addFixPointsOnBoundary(new ArrayList<VShape>() {{
			addAll(obstacles);
			add(box);
		}});
		setOldPointsToInf();
		work();
		Date date = new Date(System.currentTimeMillis() - now);
		SimpleDateFormat sdf = new SimpleDateFormat("mm:ss:SSS");
		System.out.println(sdf.format(date) + " Minuten:Sekunden:Millisekunden");
	}

	/*
	Berechnet die maximale Dichte des Szenarios anhand von Stichproben
	 */
	private double calculateMaxDensity(Function<VPoint, Double> density, VRectangle bbox)
	{
		double maxDensity = 0;
		double[][] means = new double[Parameters.SAMPLEDIVISION][Parameters.SAMPLEDIVISION];
		Random random = new Random();
		for (int i = 0; i < Parameters.SAMPLENUMBER; i++)
		{
			for (int j = 0; j < Parameters.NPOINTS; j++) {
				double x = random.nextInt((int) (bbox.getMaxX() - bbox.getMinX()) + 1);
				double y = random.nextInt((int) (bbox.getMaxY() - bbox.getMinY()) + 1);
				int xi = (int)Math.floor(x/(bbox.getMaxX()-bbox.getMinX())*(Parameters.SAMPLEDIVISION-1));
				int yi = (int)Math.floor(y/(bbox.getMaxY()-bbox.getMinY())*(Parameters.SAMPLEDIVISION-1));
				means[yi][xi] = (means[yi][xi] + density.apply(new VPoint(x, y)))/2;
				if(maxDensity < means[yi][xi])
					maxDensity = means[yi][xi];
			}
		}
		return maxDensity;
	}

	/*
	Stellt den Verlauf der Iterationen dar. Innerhalb der while(true) passiert eine Iteration des Algorithmus
	 */
	private void work()
	{
		while(true)
		{
			//double maxMove = largeMovement();
			if(largeMovement())
			{
				copyPoints();
				long now = System.currentTimeMillis();
				triangulation = new BowyerWatson3(points);
				triangulation.execute();
				//System.out.println("different edges:" + triangulation.getTriangles().stream().flatMap(t -> t.getLineStream()).collect(Collectors.toSet()).size());

				Set<VLine> setLine = new HashSet<>(triangulation.getTriangles().stream().flatMap(t -> t.getLineStream()).collect(Collectors.toList()));
				System.out.println("different edges:" + setLine.size());

				//triangulation.setTriangles(triangulation.getTriangles().parallelStream().filter(p ->
				//		fd.apply(p.midPoint()) < -geps).collect(Collectors.toList()));
				int size = triangulation.getTriangles().size();
				/*triangulation.removeTriangleIf(triple -> fd.apply(
						new VTriangle(triple.getLeft(), triple.getMiddle(), triple.getRight()).midPoint()) >= -geps);*/
				triangulation.setTriangles(triangulation.getTriangles().parallelStream().filter(p ->
						fd.apply(p.midPoint()) < -geps).collect(Collectors.toList()));
				System.out.println("deleted:" + (size - triangulation.getTriangles().size()));

				Date date = new Date(System.currentTimeMillis() - now);
				System.out.println(new SimpleDateFormat("mm:ss:SSS").format(date) + " Bowyer-Watson-Algo");
			}
			long now = System.currentTimeMillis();
			HashMap<VLine, Integer> reversedLines = createBars();
			Date date = new Date(System.currentTimeMillis() - now);
			System.out.println(new SimpleDateFormat("mm:ss:SSS").format(date) + " CreateBars");
			now = System.currentTimeMillis();
			List<VLine> lines = new ArrayList<>(reversedLines.keySet());
			date = new Date(System.currentTimeMillis() - now);
			System.out.println(new SimpleDateFormat("mm:ss:SSS").format(date) + " CompleteLineList");
			now = System.currentTimeMillis();
			List<VPoint> FVec = generateFVec(lines);
			date = new Date(System.currentTimeMillis() - now);
			System.out.println(new SimpleDateFormat("mm:ss:SSS").format(date) + " CalculateFVec");
			now = System.currentTimeMillis();
			HashMap<String, Double> Ftot = generateFtot(reversedLines, lines, FVec);
			date = new Date(System.currentTimeMillis() - now);
			System.out.println(new SimpleDateFormat("mm:ss:SSS").format(date) + " CalculateFTot");
			now = System.currentTimeMillis();
			double[][] array = createFTotArray(Ftot, FVec, points.size());
			date = new Date(System.currentTimeMillis() - now);
			System.out.println(new SimpleDateFormat("mm:ss:SSS").format(date) + " CreateArrayOfFTot");
			now = System.currentTimeMillis();
			doEuler(array);
			date = new Date(System.currentTimeMillis() - now);
			System.out.println(new SimpleDateFormat("mm:ss:SSS").format(date) + " Euler-Method");
			now = System.currentTimeMillis();
			projectBack();
			date = new Date(System.currentTimeMillis() - now);
			System.out.println(new SimpleDateFormat("mm:ss:SSS").format(date) + " ProjectPointsBack");
			now = System.currentTimeMillis();
			double test = qualityCheck();
			date = new Date(System.currentTimeMillis() - now);
			System.out.println(new SimpleDateFormat("mm:ss:SSS").format(date) + " QualityCheck");
			System.out.println(test);
			if(test > Parameters.qualityMeasurement)
				break;
		}
	}

	/*
	Euler-Methode
	 */
	private void doEuler(double[][] array)
	{
		points = IntStream.range(0,points.size()).parallel().mapToObj(i -> {
			if(points.get(i).identifier != -1) {
				return new VPoint(array[i][0] * Parameters.DELTAT + points.get(i).getX(), Parameters.DELTAT * array[i][1] + points.get(i).getY(), i);

			} else {
				return points.get(i);
			}
		}).collect(Collectors.toList());
	}

	/*
	Fügt Fixpunkte hinzu. Dabei werden von allen Objekten die Ecken hinzugefügt bzw. anhand von SEGMENTDIVISION
	 */
	private void addFixPointsOnBoundary(ArrayList<? extends VShape> obs)
	{
		for(VShape obj : obs)
		{
			PathIterator path = obj.getPathIterator(null);
			double[] tempCoords = new double[6];
			double[] coordinates = new double[6];
			path.currentSegment(tempCoords);

			while (!path.isDone()) {
				path.next();

				path.currentSegment(coordinates);
				VPoint[] points = divLine(coordinates[0], coordinates[1], tempCoords[0], tempCoords[1], Parameters.SEGMENTDIVISION);
				if (coordinates[0] == tempCoords[0] && coordinates[1] == tempCoords[1]) {
					break;
				}
				this.points.addAll(Arrays.asList(points).subList(1, points.length));
				path.currentSegment(tempCoords);
			}
		}
	}

	/*
	Unterteilt eine Linie eines Objekts in Fixpunkte
	 */
	private VPoint[] divLine(double x1, double y1, double x2, double y2, int segments)
	{
		VPoint[] points = new VPoint[segments+1];
		double dX = (x1-x2)/segments;
		double dY = (y1-y2)/segments;

		for(int i = 1; i < points.length; i++)
		{
			points[i] = new VPoint(x2 + i*dX, y2 + i*dY, -1);
		}
		return points;
	}

	/*
	Berechnet die durchschnittliche Qualität aller erzeugten Dreiecke
	 */
	private double qualityCheck()
	{
		double ave = 0;
		int i = 0;
		for(VTriangle v : triangulation.getTriangles()) {
			VLine[] line = v.getLines();
			double a = Math.sqrt(Math.pow(line[0].getX1() - line[0].getX2(), 2) + Math.pow(line[0].getY1() - line[0].getY2(), 2));
			double b = Math.sqrt(Math.pow(line[1].getX1() - line[1].getX2(), 2) + Math.pow(line[1].getY1() - line[1].getY2(), 2));
			double c = Math.sqrt(Math.pow(line[2].getX1() - line[2].getX2(), 2) + Math.pow(line[2].getY1() - line[2].getY2(), 2));
			ave += ((b + c - a) * (c + a - b) * (a + b - c)) / (a * b * c);
			i++;
		}
		return ave/i;
	}

	/*
	Erzeugt das entgültige System an externen und internen Kräften
	 */
	private double[][] createFTotArray(HashMap<String, Double> Ftot, List<VPoint> FVec, int size)
	{
		double[][] FtotArray = new double[size][2];
		int i = 0;
		for(String key : Ftot.keySet())
		{
			String[] indAsString = key.split(":");
			int[] ind = new int[] {Integer.parseInt(indAsString[0]), Integer.parseInt(indAsString[1])};
			double value = Ftot.get(key);
			if(ind[0] != -1) {
				if (ind[1] == 0)
					FtotArray[ind[0]][ind[1]] = value;
				else
					FtotArray[ind[0]][ind[1]] = value;
			}
			i++;
		}
		return FtotArray;
	}

	/*
	Projeziert außerhalb liegende Punkte zurück in das Szenario
	 */
	private void projectBack()
	{
		int[] positions = IntStream.range(0,points.size()).parallel().filter(i -> fd.apply(points.get(i)) > 0).toArray();
		double[] dGradPX = Arrays.stream(positions).parallel().mapToDouble(i -> (fd.apply(points.get(i).add(new VPoint(deps,0))) - fd.apply(points.get(i)))/deps).toArray();
		double[] dGradPY = Arrays.stream(positions).parallel().mapToDouble(i -> (fd.apply(points.get(i).add(new VPoint(0,deps))) - fd.apply(points.get(i)))/deps).toArray();
		int j = 0;
		for(int i : positions)
		{
			int save = points.get(i).identifier;
			if(save != -1)
			{
				points.set(i, points.get(i).subtract(new VPoint(fd.apply(points.get(i)) * dGradPX[j], fd.apply(points.get(i)) * dGradPY[j])));
				points.get(i).identifier = save;
			}
			j++;
		}
	}

	/*
	Erzeugt alle Kräfte der Kanten (Teilkräfte von Ftot)
	 */
	private List<VPoint> generateFVec(List<VLine> lines)
	{
		List<Double> L = lines.parallelStream().map(l -> l.getP1().distance(l.getP2())).collect(Collectors.toList());
		List<Double> L0 = calculateDesiredLengths(lines, L);
		List<Double> F = IntStream.range(0, L0.size()).parallel().mapToDouble(i -> Math.max(L0.get(i) - L.get(i),0)).boxed().collect(Collectors.toList());
		return IntStream.range(0, L.size()).parallel().mapToObj(i ->
				new VPoint((F.get(i) / L.get(i)) * (lines.get(i).getX1()-lines.get(i).getX2()),
						(F.get(i) / L.get(i)) * (lines.get(i).getY1() - lines.get(i).getY2()), i)).collect(Collectors.toList());
	}

	/*
	Berechnet die gewünschten Kantenlängen
	 */
	private List<Double> calculateDesiredLengths(List<VLine> allLines, List<Double> L)
	{
		List<Double> hbars = allLines.parallelStream().map(p -> fh.apply(new VPoint((p.x1+p.x2)/2, (p.y1+p.y2)/2))).collect(Collectors.toList());
		double sumOfL = L.parallelStream().mapToDouble(s -> Math.pow(s, 2)).sum();
		double sumOfhbars = hbars.parallelStream().mapToDouble(s -> Math.pow(s, 2)).sum();
		System.out.println("scale factor" + Math.sqrt(sumOfL/sumOfhbars));
		return hbars.parallelStream().mapToDouble(h -> h*Parameters.FSCALE*Math.sqrt(sumOfL/sumOfhbars)).boxed().collect(Collectors.toList());
	}

	/*
	Erzeugt die insgesamt wirkenden Kräfte im Szenario
	 */
	private HashMap<String, Double> generateFtot(HashMap<VLine, Integer> filteredLines, List<VLine> lines, List<VPoint> Fvec)
	{
		HashMap<String, Double> fTot = new HashMap<>();
		for(VLine bar : lines)
		{
			String[] id = bar.getIdentifier().split(":");
			for(int i = 0; i < 2; i++)
			{
				VPoint f = Fvec.get(filteredLines.get(bar));
				if(!(fTot.containsKey(id[i]+":0") && fTot.containsKey(id[i]+":1")))
				{
					if(i==0) {
						fTot.put(id[i]+":0", f.getX());
						fTot.put(id[i]+":1", f.getY());
					} else {
						fTot.put(id[i]+":0", -f.getX());
						fTot.put(id[i]+":1", -f.getY());
					}
				} else {
					double f1 = fTot.get(id[i]+":0");
					double f2 = fTot.get(id[i]+":1");
					if(i==0) {
						fTot.replace(id[i]+":0", f1, f1 + f.getX());
						fTot.replace(id[i]+":1", f2, f2 + f.getY());
					} else {
						fTot.replace(id[i]+":0", f1, f1 - f.getX());
						fTot.replace(id[i]+":1", f2, f2 - f.getY());
					}
				}
			}
		}
		return fTot;
	}

	/*
	Erzeugt eine Map mit einem Bezug zwischen VLines und Indexen aus der Point-Liste
	 */
	private HashMap<VLine, Integer> createBars()
	{
		List<VLine> allLines = new ArrayList<>();
		List<VLine[]> allTriangleLines = triangulation.getTriangles().parallelStream().map(VTriangle::getLines).collect(Collectors.toList());
		for(VLine[] line: allTriangleLines)
		{
			allLines.addAll(Arrays.asList(line).subList(0, 3));
		}
		Set<VLine> set = new HashSet<>();
		set.addAll(allLines);
		allLines.clear();
		allLines.addAll(set);
		HashMap<VLine, Integer> mappedLines = new HashMap<>();
		for(int i = 0; i < allLines.size(); i++)
		{
			mappedLines.put(allLines.get(i), i);
		}
		return mappedLines;
	}

	/*
	Erzeugt eine Kopie der Punkte aus der Point-Liste
	 */
	private void copyPoints()
	{
		oldPoints.clear();
		oldPoints = new ArrayList<>(points);
	}

	private void setOldPointsToInf()
	{
		oldPoints.clear();
		for (int i = 0; i < points.size(); i++) {
			oldPoints.add(new VPoint(Integer.MAX_VALUE, Integer.MAX_VALUE));
		}
	}

	/*
	Rejection-Method nach Persson und Strang
	 */
	private void removePointsAndRejection()
	{
		pointsNotInDomain();
		double[] r0 = points.parallelStream().mapToDouble(p -> 1/ pow(fh.apply(p), 2)).toArray();
		double max = Arrays.stream(r0).max().getAsDouble();
		points = points.parallelStream().filter(p -> Math.random() < 1/ pow(fh.apply(p), 2)/max).collect(Collectors.toList());
		IntStream.range(0, points.size()).forEach(i -> points.get(i).identifier = i);
	}

	private boolean largeMovement()
	{
		double maxMove = IntStream.range(0,points.size()).parallel().mapToDouble(i ->
				Math.sqrt(pow(points.get(i).subtract(oldPoints.get(i)).getX(), 2) +
						pow(points.get(i).subtract(oldPoints.get(i)).getY(), 2))).max().getAsDouble();
		System.out.println("maxMove:" + maxMove);
		return maxMove/h0 > Parameters.TOL;
	}

	/*
	Berechnet die Bewegung von Punkten im Vergleich mit einer Toleranz GEPS
	 */
	private void pointsNotInDomain()
	{
		points = points.parallelStream().filter(p -> fd.apply(p) < geps).collect(Collectors.toList());
	}

	/*
	Erzeugt die Punktmenge für den Algorithmus
	 */
	private void generatePoints(VRectangle box)
	{
		double elementsInCol = Math.ceil((box.getX()+box.getWidth())/h0 + 1);
		double elementsInRow = Math.ceil((box.getY()+box.getHeight())/ (h0 * Math.sqrt(3)/2));
		double startX = box.getX();
		double startY = box.getY();

		for(int j = 0; j < elementsInRow; j++)
		{
			for(int i = 0; i < elementsInCol; i++)
			{
				if( j != 0 && j%2 != 0)
				{
					points.add(new VPoint(startX+i*h0+h0/2, startY+j*h0*Math.sqrt(3)/2));
				} else {
					points.add(new VPoint(startX+i*h0, startY+j*h0*Math.sqrt(3)/2));
				}
			}
		}
	}

	public List<VPoint> getPoints() {
		return points;
	}

	private double doDDiff(double d1, double d2)
	{
		return Math.max(d1, -d2);
	}

	private double doDUnion(double d1, double d2) {
		return Math.min(d1, d2);
	}

	public BowyerWatson3 getTriangulation() {
		return triangulation;
	}
}
