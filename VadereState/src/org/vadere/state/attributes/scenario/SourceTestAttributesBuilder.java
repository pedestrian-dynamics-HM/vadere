package org.vadere.state.attributes.scenario;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.vadere.state.attributes.distributions.AttributesConstantDistribution;
import org.vadere.state.attributes.spawner.AttributesSpawner;
import org.vadere.state.util.StateJsonConverter;
import org.vadere.util.geometry.shapes.VPolygon;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.geometry.shapes.VShape;

import java.awt.geom.Path2D;
import java.io.IOException;

public class SourceTestAttributesBuilder {

	private int id = 1;
	private double startTime = 1;
	private double endTime = 2;
	private int spawnNumber = 1;
	private boolean useFreeSpaceOnly = false;
	private boolean spawnAtRandomPositions = false;
	private final String distributionName= "constant";
	private final AttributesConstantDistribution parameter = new AttributesConstantDistribution();
	private double[] groupSizeDistribution = new double[]{0.0, 0.0, 1.0};
	private Integer[] groupSizeDistributionMock = new Integer[]{};
	private int maxSpawnNumberTotal = AttributesSpawner.NO_MAX_SPAWN_NUMBER_TOTAL;
	private double x0 = 0.0;
	private double y0 = 0.0;
	private double x1 = 5.0;
	private double y1 = 0.0;
	private double x2 = 5.0;
	private double y2 = 5.0;
	private double x3 = 0.0;
	private double y3 = 5.0;
	private long randomSeed = 0;

	public SourceTestAttributesBuilder(){
		parameter.setUpdateFrequency(1);
	}

	public AttributesSource getResult() throws IOException {
		String json = generateSourceAttributesJson();
		return StateJsonConverter.deserializeObjectFromJson(json, AttributesSource.class);
	}

	public SourceTestAttributesBuilder setOneTimeSpawn(double time) {
		this.startTime = time;
		this.endTime = time;
		return this;
	}

	public SourceTestAttributesBuilder setStartTime(double startTime) {
		this.startTime = startTime;
		return this;
	}

	public SourceTestAttributesBuilder setEndTime(double endTime) {
		this.endTime = endTime;
		return this;
	}

	public SourceTestAttributesBuilder setSpawnNumber(int spawnNumber) {
		this.spawnNumber = spawnNumber;
		return this;
	}

	public SourceTestAttributesBuilder setDistributionParams(double updateFruequency) {
		this.parameter.setUpdateFrequency(updateFruequency);
		return this;
	}

	public SourceTestAttributesBuilder setUseFreeSpaceOnly(boolean useFreeSpaceOnly) {
		this.useFreeSpaceOnly = useFreeSpaceOnly;
		return this;
	}

	public SourceTestAttributesBuilder setSpawnAtRandomPositions(boolean spawnAtRandomPositions) {
		this.spawnAtRandomPositions = spawnAtRandomPositions;
		return this;
	}

//	public SourceTestAttributesBuilder setDistributionClass(Class<? extends VadereDistribution> distributionClass) {
//		this.distributionClass = distributionClass;
//		return this;
//	}

	public SourceTestAttributesBuilder setMaxSpawnNumberTotal(int maxSpawnNumberTotal) {
		this.maxSpawnNumberTotal = maxSpawnNumberTotal;
		return this;
	}

//	public SourceTestAttributesBuilder setDistributionParameters(double[] params) {
//		distributionParams = params;
//		return this;
//	}

	public SourceTestAttributesBuilder setId(int id) {
		this.id = id;
		return this;
	}

	public SourceTestAttributesBuilder setRandomSeed(long seed) {
		this.randomSeed = seed;
		return this;
	}

	public long getRandomSeed() {
		return randomSeed;
	}

	public SourceTestAttributesBuilder setSourceDim(VRectangle rect) {
		x0 = rect.x;
		y0 = rect.y;

		x1 = x0 + rect.width;
		y1 = y0;

		x2 = x0 + rect.width;
		y2 = x0 + rect.height;

		x3 = x0;
		y3 = y0 + rect.height;
		return this;
	}

	public SourceTestAttributesBuilder setSourceDim(double width, double height) {
		x1 = width;
		y1 = 0;
		x2 = width;
		y2 = height;
		x3 = 0;
		y3 = height;
		return this;
	}

	public SourceTestAttributesBuilder setGroupSizeDistribution(double... dist) {
		this.groupSizeDistribution = dist;
		return this;
	}

	public SourceTestAttributesBuilder setGroupSizeDistributionMock(Integer... mock) {
		this.groupSizeDistributionMock = mock;
		return this;
	}

	public Integer[] getGroupSizeDistributionMock() {
		return groupSizeDistributionMock;
	}

	private String groupSizeDistribution() {
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		for (int i = 0; i < groupSizeDistribution.length - 1; i++) {
			sb.append(groupSizeDistribution[i]).append(", ");
		}
		sb.append(groupSizeDistribution[groupSizeDistribution.length - 1]).append(" ]");

		return sb.toString();
	}

	public SourceTestAttributesBuilder setDiamondShapeSource() {
		x1 = 3.0;
		y1 = 3.0;
		x2 = 0.0;
		y2 = 6.0;
		x3 = -3.0;
		y3 = 3.0;
		return this;
	}

	public VShape getSourceShape() {
		Path2D.Double path = new Path2D.Double();
		path.moveTo(0.0, 0.0);
		path.lineTo(x1, y1);
		path.lineTo(x2, y2);
		path.lineTo(x3, y3);
		path.closePath();

		return new VPolygon(path);
	}

	private String generateSourceAttributesJson() {
		String paramString = "";
		try {
			 paramString = new ObjectMapper().writeValueAsString(parameter);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}


		return "{  \"id\" : " + id + ","
				+ "\"shape\": {"
				+ 	"\"type\": \"POLYGON\""
				+	",\"points\": ["
				+		"{\"x\": 0.0,\"y\": 0.0}"
				+ 		",{\"x\": " + x1 + ",\"y\": " + y1 + "}"
				+ 		",{\"x\": " + x2 + ",\"y\": " + y2 + "}"
				+ 		",{\"x\": " + x3 + ",\"y\": " + y3 + "}"
				+	"]}"
				+ ",\"targetIds\": [1]"
				+ ",\"spawner\" : {"
				+ 	"\"type\" : \"org.vadere.state.attributes.spawner.AttributesRegularSpawner\""
				+ 	",\"constraintsElementsMax\" : " + maxSpawnNumberTotal
				+ 	",\"constraintsTimeStart\" : " + startTime
				+ 	",\"constraintsTimeEnd\" : " +endTime
				+ 	",\"eventPositionRandom\" : " +spawnAtRandomPositions
				+ 	",\"eventPositionGridCA\" : false"
				+ 	",\"eventPositionFreeSpace\" : " +useFreeSpaceOnly
				+ 	",\"eventElementCount\" : "+spawnNumber
				+ 	",\"eventElement\" : null"
				+ 	",\"distribution\" : "+ paramString
				+ 	"},"
				+ "\"groupSizeDistribution\" : " +groupSizeDistribution()
				+ "}";
	}

}
