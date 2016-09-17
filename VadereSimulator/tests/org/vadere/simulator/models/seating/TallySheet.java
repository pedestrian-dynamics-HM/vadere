package org.vadere.simulator.models.seating;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class TallySheet {
	private Map<Integer, Integer> map = new HashMap<>();

	public void addOneTo(int key) {
		if (map.containsKey(key)) {
			map.put(key, map.get(key) + 1);
		} else {
			map.put(key, 1);
		}
	}
	
	public int getCount(int key) {
		Integer result = map.get(key);
		if (result == null) {
			return 0;
		}
		return result;
	}
	
	public Set<Integer> getKeys() {
		return map.keySet();
	}

}
