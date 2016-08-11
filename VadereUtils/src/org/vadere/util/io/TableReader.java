package org.vadere.util.io;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;

import org.vadere.util.data.Row;
import org.vadere.util.data.Table;
import org.vadere.util.data.Tripel;
import org.vadere.util.data.Tupel;

/**
 * This class convert a stream of table lines {@link java.util.stream.Stream<String>} into a
 * {@link org.vadere.util.data.Table}.
 * Note that a stream can not be reused, so if u first want to get the headline and then construct
 * the table
 * based on the headline you have to use two {@link java.util.stream.Stream<String>} instances,
 * so you have to recreate the {@link java.util.stream.Stream<String>}.
 *
 */
public class TableReader {

	private static int TYPE_PATTERN_LENGTH = 2; // such as: %d or %f and so on
	private String format;

	public TableReader(final String format) {
		this.format = format;
	}

	public TableReader() {
		this.format = null;
	}

	public Table readTable(final Stream<String> lines, final String... variables) {
		return readTable(lines, true, variables);
	}

	public Table readTable(final Stream<String> lines, boolean containsHeadLine, final String... variables) {
		Table table = new Table(variables);
		readRows(lines, containsHeadLine, variables).forEach(row -> table.addRow(row));
		return table;
	}

	public String[] readHeadLine(final Stream<String> lines) {
		Optional<String> line = lines.filter(TableReader::isNotEmptyString).findFirst();
		String[] headline = new String[] {};
		if (line.isPresent()) {
			String headerFormat;
			if (format == null) {
				headerFormat = Arrays.stream(line.get().split(" ")).reduce("", (s1, s2) -> s1.concat("%s ")).trim();
			} else {
				headerFormat = format.replaceAll("%.", "%s");
			}


			long limit = headerFormat.chars().filter(c -> c == '%').count();
			return Stream
					.iterate(Tupel.of(headerFormat, line.get()), t -> nextResidual(t))
					.limit(limit)
					.filter(t -> t.isPresent())
					.map(t -> toValue(t.v1, t.v2).toString()).toArray(String[]::new);
		}
		return headline;
	}


	private Stream<Row> readRows(final Stream<String> lines, final boolean skipHeadline, final String... variables) {
		return lines.filter(TableReader::isNotEmptyString).skip(skipHeadline ? 1 : 0)
				.map(line -> toRow(line, format, variables));
	}

	private Row toRow(final String line, String format, final String... variables) {
		if (format == null) {
			format = Arrays.stream(line.split(" ")).reduce("", (s1, s2) -> s1.concat("%s ")).trim();
		}
		Row row = Stream
				.iterate(Tripel.of(0, format, line), t -> nextResidual(t))
				.limit(variables.length)
				.map(tripel -> toColumn(variables[tripel.v1], tripel.v2, tripel.v3))
				.reduce(new Row(), (r1, r2) -> r1.combine(r2));
		return row;
	}

	private Object toValue(final String residualFormatString, final String residualLine) {
		Row row = new Row();

		int first = residualFormatString.indexOf("%");
		int second = residualFormatString.indexOf('%', first + 1);

		char typeChar = residualFormatString.charAt(first + 1);
		String value = "";

		if (second != -1) {
			String seperator = residualFormatString.substring(first + TYPE_PATTERN_LENGTH, second);
			value = residualLine.substring(0, residualLine.indexOf(seperator));
		} else {
			value = residualLine;
		}
		return TableReader.convertStringByFormat(value, typeChar);
	}

	private Row toColumn(final String columnName, final String residualFormatString, final String residualLine) {
		Row row = new Row();
		row.setEntry(columnName, toValue(residualFormatString, residualLine));
		return row;
	}

	private Tripel<Integer, String, String> nextResidual(final Tripel<Integer, String, String> tripel) {
		Integer varCount = tripel.v1;
		Tupel<String, String> tupel = nextResidual(Tupel.of(tripel.v2, tripel.v3));

		return tupel.isPresent() ? Tripel.of(varCount + 1, tupel.v1, tupel.v2) : Tripel.empty();
	}

	private Tupel<String, String> nextResidual(final Tupel<String, String> tupel) {
		if (!tupel.isPresent()) {
			return tupel;
		}

		String residualFormat = tupel.v1;
		String residualLine = tupel.v2;

		int first = residualFormat.indexOf('%');
		residualFormat = residualFormat.substring(first + TYPE_PATTERN_LENGTH);

		first = residualFormat.indexOf('%');

		Tupel<String, String> resultTupel;
		if (first != -1) {
			String seperator = residualFormat.substring(0, first);
			residualFormat = residualFormat.substring(residualFormat.indexOf('%'));
			residualLine = residualLine.substring(residualLine.indexOf(seperator) + seperator.length());
			resultTupel = Tupel.of(residualFormat, residualLine);
		} else {
			resultTupel = Tupel.empty();
		}

		return resultTupel;
	}

	/**
	 * Convert the value to an primitive DataType which is defined by format (e.g. d (for integer)
	 * or f (for double)).
	 * 
	 * @param value
	 * @param format
	 * @return
	 */
	private static Object convertStringByFormat(final String value, final char format) {
		switch (format) {
			case 'd':
				return Integer.parseInt(value);
			case 's':
				return value;
			case 'f':
				return Double.parseDouble(value);
			default:
				return "";
		}
	}

	private static boolean isNotEmptyString(final String s) {
		return !s.trim().isEmpty();
	}
}
