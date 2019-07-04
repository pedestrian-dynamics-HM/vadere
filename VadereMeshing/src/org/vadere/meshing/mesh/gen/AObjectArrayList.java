package org.vadere.meshing.mesh.gen;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class AObjectArrayList<K> extends ObjectArrayList<K> {
	public void swap(int i, int j) {
		if (i >= size || j >= size) {
			throw new IndexOutOfBoundsException("Index (" + i + ") or (" + j + ") is greater than or equal to list size (" + size + ")");
		} else {
			K tmp = a[j];
			a[j] = a[i];
			a[i] = tmp;
		}
	}
}
