package org.vadere.util.io.parser;

import com.fasterxml.jackson.databind.JsonNode;

import java.text.ParseException;
import java.util.HashSet;
import java.util.Set;

/**
 * This {@link org.vadere.util.io.parser.JsonLogicParser} is able to generate a {@linkg VPredicate}
 * based on a logical expression ({@link String})
 * that tests a Object of type {@link com.google.gson.JsonObject}.
 *
 */
public class JsonLogicParser extends LogicalParser<JsonNode> {

	public JsonLogicParser(final String expression) {
		super(expression);
	}

	@Override
	protected VPredicate<JsonNode> getPredicate(final String expression) throws ParseException {
		// subset or superset
		if (expression.equals("false")) {
			return jsonObj -> false;
		} else if (expression.equals("true")) {
			return jsonObj -> true;
		} else if (expression.contains(":")) {
			String[] split = expression.split(":");
			// superset
			if (split[0].contains("{")) {
				return (JsonNode jsonObj) -> {
					Set<Double> set = getSetFromString(split[0]);
					JsonNode element = getJsonElement(split[1], jsonObj);
					if (element.isArray()) {
						Set<Double> jsonSet = getSetFromJson(element);
						return jsonSet.containsAll(set);
					} else {
						return set.size() == 1 && set.contains(element.asDouble());
					}
				};
			} // subset
			else {
				return (JsonNode jsonObj) -> {
					Set<Double> set = getSetFromString(split[1]);
					JsonNode element = getJsonElement(split[0], jsonObj);
					if (element.isArray()) {
						Set<Double> jsonSet = getSetFromJson(element);
						return set.containsAll(jsonSet);
					} else {
						return set.contains(element.asDouble());
					}
				};
			}
		} else if (expression.contains("==")) {
			return jsonObj -> JsonLogicParser.evalEquals(expression, jsonObj);
		} else if (expression.contains("!=")) {
			return jsonObj -> JsonLogicParser.evalNotEquals(expression, jsonObj);
		} else if (expression.contains("<=")) {
			return jsonObj -> JsonLogicParser.evalSmallerThanOrEquals(expression, jsonObj);
		} else if (expression.contains(">=")) {
			return jsonObj -> JsonLogicParser.evalGreaterThanOrEquals(expression, jsonObj);
		} else if (expression.contains("<")) {
			return jsonObj -> JsonLogicParser.evalSmallerThan(expression, jsonObj);
		} else if (expression.contains(">")) {
			return jsonObj -> JsonLogicParser.evalGreaterThan(expression, jsonObj);
		} else {
			throw new ParseException("unsupported expression: " + expression, 0);
		}
	}

	// helper methods

	private Set<Double> getSetFromJson(final JsonNode jsonArray) {
		Set<Double> jsonSet = new HashSet<>();
		for (JsonNode el : jsonArray) {
			jsonSet.add(el.asDouble());
		}
		return jsonSet;
	}

	private Set<Double> getSetFromString(final String stringSet) {
		Set<Double> set = new HashSet<>();
		String cleanString = stringSet.substring(stringSet.indexOf('{') + 1, stringSet.indexOf('}'));
		for (String s : cleanString.split(",")) {
			set.add(Double.parseDouble(s.trim()));
		}
		return set;
	}

	private static JsonNode getJsonElement(final String path, final JsonNode jsonObject) {
		String[] parts = path.split("\\.");
		JsonNode obj = jsonObject;
		if (path != null) {
			for (int i = 0; i < parts.length; i++) {
				if (i < parts.length - 1) {
					obj = obj.get(parts[i]);
				} else {
					return obj.get(parts[i]);
				}
			}
		}
		return null;
	}

	private static JsonNode getJsonPrimitiv(final String path, final JsonNode jsonObj) throws ParseException {
		JsonNode jsonElement = JsonLogicParser.getJsonElement(path, jsonObj);
		if (jsonElement == null || !jsonElement.isValueNode()) {
			throw new ParseException("Json element does not exist or is not a primitive type.", 0);
		} else {
			return jsonElement;
		}
	}

	private static boolean evalGreaterThan(final String expression, final JsonNode jsonObj) throws ParseException {
		String[] split = expression.split(">");
		JsonNode obj = getJsonPrimitiv(split[0], jsonObj);

		if (obj.isNumber()) {
			return obj.asDouble() > Double.parseDouble(split[1]);
		} else {
			throw new ParseException("parse error in eq cause by " + expression, 0);
		}
	}

	private static boolean evalGreaterThanOrEquals(final String expression, final JsonNode jsonObj)
			throws ParseException {
		String[] split = expression.split(">=");
		JsonNode obj = getJsonPrimitiv(split[0], jsonObj);

		if (obj.isNumber()) {
			return obj.asDouble() >= Double.parseDouble(split[1]);
		} else {
			throw new ParseException("parse error in eq cause by " + expression, 0);
		}
	}

	private static boolean evalSmallerThan(final String expression, final JsonNode jsonObj) throws ParseException {
		String[] split = expression.split("<");
		JsonNode obj = JsonLogicParser.getJsonPrimitiv(split[0], jsonObj);

		if (obj.isNumber()) {
			return obj.asDouble() < Double.parseDouble(split[1]);
		} else {
			throw new ParseException("parse error in eq cause by " + expression, 0);
		}
	}

	private static boolean evalSmallerThanOrEquals(final String expression, final JsonNode jsonObj)
			throws ParseException {
		String[] split = expression.split("<=");
		JsonNode obj = getJsonPrimitiv(split[0], jsonObj);

		if (obj.isNumber()) {
			return obj.asDouble() <= Double.parseDouble(split[1]);
		} else {
			throw new ParseException("parse error in eq cause by " + expression, 0);
		}
	}

	private static boolean evalEquals(final String expression, final JsonNode jsonObj) throws ParseException {
		String[] split = expression.split("==");
		JsonNode obj = getJsonPrimitiv(split[0], jsonObj);

		if (obj.isBoolean()) {
			return obj.asBoolean() == Boolean.parseBoolean(split[1]);
		} else if (obj.isNumber()) {
			return obj.asDouble() == Double.parseDouble(split[1]);
		} else if (obj.isTextual()) {
			return obj.asText().equals(split[1]);
		} else {
			throw new ParseException("parse error in eq cause by " + expression, 0);
		}
	}

	private static boolean evalNotEquals(final String expression, final JsonNode jsonObj) throws ParseException {
		String[] split = expression.split("!=");
		JsonNode obj = getJsonPrimitiv(split[0], jsonObj);

		if (obj.isBoolean()) {
			return obj.asBoolean() != Boolean.parseBoolean(split[1]);
		} else if (obj.isNumber()) {
			return obj.asDouble() != Double.parseDouble(split[1]);
		} else if (obj.isTextual()) {
			return !obj.asText().equals(split[1]);
		} else {
			throw new ParseException("parse error in eq cause by " + expression, 0);
		}
	}


}
