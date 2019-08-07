package org.vadere.state.util;

import org.vadere.state.attributes.models.AttributesFloorField;
import org.vadere.state.scenario.Topography;

public class Views {

	// Marker Class for Jackson serialization. Will not be used anywhere but is needed for the
	// subclass CacheViewExclude
	public static class CacheView{

	}

	/**
	 * all fields marked with this view in @JsonView(Views.CacheViewExclude.class) will
	 * if a serialization is started using  'withView(Views.CacheView.class)'l be ignored
	 * If the MapperFeature.DEFAULT_VIEW_INCLUSION is NOT disabled all none annotated fields
	 * will be included in the serialization.
	 *
	 * Thus if the serialization is executed with
	 * String str = mapper
	 *   				.writerWithDefaultPrettyPrinter()
	 *   				.withView(Views.CacheView.class)
	 *   				.writeValueAsString(attr);
	 *
	 * All field not annotated with @JsonView will be INCLUDED as well as all fields
	 * with @JsonView(Views.CacheView.class). Fields annotated with @JsonView(Views.CacheViewExclude.class)
	 * however will be EXCLUDED because CacheViewExclude extends CacheView.
	 * see https://www.baeldung.com/jackson-json-view-annotation for json view tutorial and
	 * see {@link StateJsonConverter#getFloorFieldHash(Topography, AttributesFloorField)} for usage.
	 */
	public static class CacheViewExclude extends CacheView {

	}
}
