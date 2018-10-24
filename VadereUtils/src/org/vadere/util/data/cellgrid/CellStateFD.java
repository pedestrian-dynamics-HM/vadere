package org.vadere.util.data.cellgrid;

/**
 * The state of one cell. Can be EMPTY(' '), PERSON('p'), OBSTACLE('#'),
 * SOURCE('s') and TARGET('t').
 * 
 */
public enum CellStateFD {
	EMPTY(' '), PERSON('p'), OBSTACLE('#'), SOURCE('s'), TARGET('t');
	final private char shortForm;

	CellStateFD(char c) {
		shortForm = c;
	}

	public static CellStateFD valueOf(char c) {
		switch (c) {
			case ' ':
				return CellStateFD.EMPTY;
			case 'p':
				return CellStateFD.PERSON;
			case 't':
				return CellStateFD.TARGET;
			case '#':
				return CellStateFD.OBSTACLE;
		}
		return CellStateFD.EMPTY;
	}

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();
		result.append(shortForm);

		return result.toString();
	}
}
