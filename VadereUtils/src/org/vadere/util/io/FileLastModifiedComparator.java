package org.vadere.util.io;

import java.io.File;
import java.util.Comparator;

public class FileLastModifiedComparator implements Comparator<File> {

	@Override
	public int compare(final File o1, final File o2) {
		if (o1.lastModified() > o2.lastModified()) {
			return 1;
		} else if (o1.lastModified() < o2.lastModified()) {
			return -1;
		} else {
			return 0;
		}
	}
}
