package org.vadere.util.data;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class FloorFieldGridRow {
    private List<Double> values;

    public FloorFieldGridRow(int size) {
        this.values = IntStream.range(0, size).boxed().map(i -> i*1.0).collect(Collectors.toList());
    }

    public String[] toStrings() {
        return this.values.stream().map(v -> v.toString()).toArray(size -> new String[size]);
    }

    public void setValue(int index, final Double value) {
	    this.values.set(index, value);
    }

    public int size(){
        return values.size();
    }
}
