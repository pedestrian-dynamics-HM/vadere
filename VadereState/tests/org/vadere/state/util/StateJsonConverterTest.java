package org.vadere.state.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.ser.FilterProvider;
import com.fasterxml.jackson.databind.ser.PropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;

import org.junit.Test;
import org.vadere.state.attributes.AttributesSimulation;


import static org.junit.Assert.*;

public class StateJsonConverterTest {


	@Test
	public void testMapper(){
		AttributesSimulation a = new AttributesSimulation();
		PropertyFilter empty = SimpleBeanPropertyFilter.serializeAll();
		PropertyFilter ignoreSeed = SimpleBeanPropertyFilter.serializeAllExcept("randomSeed");
		SimpleFilterProvider fp = new SimpleFilterProvider().addFilter("testFilter", empty);
		ObjectMapper mapper = StateJsonConverter.getMapper().setFilterProvider(fp);
		ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();

		try {
			JsonNode json = mapper.convertValue(a, JsonNode.class);
			String str = writer.writeValueAsString(json);
			System.out.println(str);
		} catch (JsonProcessingException | IllegalArgumentException e) {
			throw new RuntimeException(e);
		}

		fp.removeFilter("testFilter");
		fp.addFilter("testFilter", ignoreSeed);

		try {
			JsonNode json = mapper.convertValue(a, JsonNode.class);
			String str = writer.writeValueAsString(json);
			System.out.println(str);
		} catch (JsonProcessingException | IllegalArgumentException e) {
			throw new RuntimeException(e);
		}


	}



}