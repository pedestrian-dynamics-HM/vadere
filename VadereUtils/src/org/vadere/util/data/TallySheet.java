package org.vadere.util.data;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class TallySheet<T> {
	private Map<T, Integer> map = new HashMap<>();

	public void addOneTo(T key) {
		if (map.containsKey(key)) {
			map.put(key, map.get(key) + 1);
		} else {
			map.put(key, 1);
		}
	}
	
	public int getCount(T key) {
		Integer result = map.get(key);
		if (result == null) {
			return 0;
		}
		return result;
	}
	
	public Set<T> getKeys() {
		return map.keySet();
	}

}
