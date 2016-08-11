package org.vadere.state.deserializers;

import com.google.gson.*;

import java.lang.reflect.Type;

import org.vadere.state.attributes.models.AttributesTimeCost;
import org.vadere.util.io.IOUtils;

public class AttributesTimeCostDeserializer implements JsonDeserializer<AttributesTimeCost> {

	@Override
	public AttributesTimeCost deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
			throws JsonParseException {
		JsonObject jsonObj = json.getAsJsonObject();

		JsonElement element = jsonObj.get("laodingType");
		if (element != null) {
			jsonObj.addProperty("loadingType", element.getAsString());
		}

		element = jsonObj.get("queueWidthLaoding");
		if (element != null) {
			jsonObj.addProperty("queueWidthLoading", element.getAsDouble());
		}

		element = jsonObj.get("type");
		if (element.getAsString().equals("QUEING")) {
			jsonObj.addProperty("type", AttributesTimeCost.TimeCostFunctionType.QUEUEING.toString());
		}

		return new Gson().fromJson(jsonObj, AttributesTimeCost.class);
	}
}
