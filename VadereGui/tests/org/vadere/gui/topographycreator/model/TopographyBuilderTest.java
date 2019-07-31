package org.vadere.gui.topographycreator.model;

import org.junit.Test;
import org.vadere.state.attributes.scenario.AttributesObstacle;
import org.vadere.state.scenario.Obstacle;
import org.vadere.state.scenario.ReferenceCoordinateSystem;
import org.vadere.state.scenario.Topography;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VRectangle;

import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class TopographyBuilderTest {


	@Test
	public void translateElementsWithReferenceCoordinateSystemPresent(){
		ReferenceCoordinateSystem ref = new ReferenceCoordinateSystem();
		ref.setTranslation(new VPoint(3.3, 5.9));

		// move rectangle based on ReferenceCoordinateSystem
		VRectangle rec = new VRectangle(1.0, 1.0, 10, 30);
		rec = rec.translatePrecise(ref.getTranslation().scalarMultiply(-1.0));

		assertThat(rec.x, closeTo(1-3.3, 1e-4));
		assertThat(rec.y, closeTo(1-5.9, 1e-4));


		Topography t = new Topography();
		t.getAttributes().setReferenceCoordinateSystem(ref);
		t.addObstacle(new Obstacle(new AttributesObstacle(3, rec)));

		// translate topography. This should update the ReferenceCoordinateSystem
		TopographyBuilder builder = new TopographyBuilder(t);
		builder.translateElements(5.0, 5.0);

		Topography tNew = builder.build();
		VPoint trans = tNew.getAttributes().getReferenceCoordinateSystem().getTranslation();
		assertThat(trans.x, closeTo(3.3 - 5.0, 1e-4));
		assertThat(trans.y, closeTo(5.9 - 5.0, 1e-4));

		VRectangle recNew = (VRectangle) tNew.getObstacles().get(0).getShape();
		assertThat(recNew.x, closeTo(1 - 3.3 + 5.0, 1e-4));
		assertThat(recNew.y, closeTo(1 - 5.9 + 5.0, 1e-4));


		// if using the ReferenceCoordinateSystem translation the base value should be returned
		recNew = recNew.translatePrecise(trans);
		assertThat(recNew.x, closeTo(1.0, 1e-4));
		assertThat(recNew.y, closeTo(1.0, 1e-4));

	}


	@Test
	public void translateElementsWithoutReferenceCoordinateSystem(){

		// move rectangle based on ReferenceCoordinateSystem
		VRectangle rec = new VRectangle(1.0, 1.0, 10, 30);

		Topography t = new Topography();
		t.addObstacle(new Obstacle(new AttributesObstacle(3, rec)));

		// translate topography. This should update the ReferenceCoordinateSystem
		TopographyBuilder builder = new TopographyBuilder(t);
		builder.translateElements(5.0, 5.0);

		Topography tNew = builder.build();
		assertThat(tNew.getAttributes().getReferenceCoordinateSystem(), is(nullValue()));

		VRectangle recNew = (VRectangle) tNew.getObstacles().get(0).getShape();
		assertThat(recNew.x, closeTo(1 + 5.0, 1e-4));
		assertThat(recNew.y, closeTo(1 + 5.0, 1e-4));
			}

}