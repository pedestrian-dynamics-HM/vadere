package org.vadere.simulator.projects.migration.incident.helper;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Iterator;
import java.util.function.Predicate;

public class JsonFilterIterator implements Iterator<JsonNode> {

	JsonNode root;
	Predicate<JsonNode> filter;
	Iterator<JsonNode> iter;
	JsonNode next;

	public JsonFilterIterator(JsonNode root, Predicate<JsonNode> filter) {
		this.root = root;
		this.filter = filter;
		this.iter = root.iterator();
		this.iter.hasNext();
	}

	/**
	 * Search for the next element in the wrapped iterator which evaluates the filter to true. Save
	 * this element as the next to serve and return true. Otherwise return false
	 */
	@Override
	public boolean hasNext() {
		while (iter.hasNext()) {
			JsonNode n = iter.next();
			if (filter.test(n)) {
				this.next = n;
				return true;
			}
		}
		this.next = null;
		return false;
	}

	/**
	 * use the next element and clean the element so it is not returned a second time.
	 */
	@Override
	public JsonNode next() {
		if (next == null) {
			return null;
		} else {
			JsonNode tmp = next;
			next = null;
			return tmp;
		}
	}
}
