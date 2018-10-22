package org.vadere.util.io.parser;

import java.text.ParseException;
import java.util.Scanner;
import java.util.regex.Pattern;

/**
 * This {@link org.vadere.util.io.parser.LogicalParser} is able to generate a {@linkg VPredicate}
 * based on a logical expression ({@link String})
 * that tests a Object of type T. The following operations are allowed:
 *
 * == (String and Number,
 * != (String and Number),
 * <= (Number),
 * >= (Number),
 * < (Number),
 * > (Number)
 *
 * expression := true
 * expression := false
 * expression := javaIdentifier (==|<|>|>=|<=|!=) javaIdentifier
 * (all operators are only defined for Numbers, expect == and != these are also defined with Strings
 * and Booleans)
 * expression := javaIdentifier:{(Double)[,(Double)]*)}
 * (such as: attributes.id:{1,3,4.0}) which measn that this expression is true if attributes.id is a
 * subset of {1,3,4.0}
 * expression := {(Double)[,(Double)]*)}:javaIdentifier
 * (such as: {1,3,4.0}:attributes.id) which measn that this expression is true if attributes.id is a
 * superset of {1,3,4.0}
 * expression := !(expression)
 * expression := expression && expression
 * expression := expression || expression
 * expression := (expression)
 *
 *
 *         Gramma:
 *         E -> T || E;
 *         E -> T;
 *         T -> F && T;
 *         T -> F;
 *         F -> (E);
 *         F -> !E
 *         F -> atom;
 *
 *         ( adas && ddd || aaa )
 *         F -> (E) -> (T || E) -> (F && T || E) -> (atom && F || E) -> (atom && atom || E) -> ...
 *         -> (atom && atom || atom)
 *
 *
 */
public abstract class LogicalParser<T> {
	protected final Scanner scanner; // ((\w)+(\.\w)*)(={2})((\w)+(\.\w)*)|((\w)+(\.\w)*)
	protected String lookahead;
	private final String regInteger = "(\\+|-)?(0|[1-9][0-9]*)";
	private final String regDouble = "[+-]?(0|[1-9][0-9]*)(\\.[0-9]*)";
	private final String regIntOrDouble = "(" + regInteger + "|" + regDouble + ")";
	private final String regJavaIdentifier = "(\\w)+(\\.\\w+)*";
	private final String regCompare = regJavaIdentifier + "((==)|<|>|(>=)|(<=)|(!=))" + regJavaIdentifier;
	private final String regSubSet = regJavaIdentifier + ":\\{" + regIntOrDouble + "(," + regIntOrDouble + ")*\\}";
	private final String regSuperSet = "\\{" + regIntOrDouble + "(," + regIntOrDouble + ")*\\}:" + regJavaIdentifier;

	private final String regExpression =
			"((" + regJavaIdentifier + ")|(" + regCompare + ")|(" + regSubSet + ")|(" + regSuperSet + "))";// + "==" + regJavaIdentifier;
	// private final String regExpression = regSubSet;
	private final String regAnd = "&&";
	private final String regOr = "\\|\\|";
	private final String regOpen = "\\(";
	private final String regClose = "\\)";
	private final String regNot = "!";
	private final String regDelimiter = "\\s";
	private int counter;

	protected LogicalParser(final String text) {
		scanner = new Scanner(reformatText(text));
		scanner.useDelimiter(Pattern.compile("\\s"));
		counter = 0;
		lookahead = "";
	}

	private String reformatText(final String text) {
		// remove all spaces
		String reformatText = text.replaceAll("\\s+", "");

		// space for brackets and logic relations
		reformatText = reformatText.replaceAll("\\(", " ( ");
		reformatText = reformatText.replaceAll("\\)", " ) ");
		reformatText = reformatText.replaceAll("&&", " && ");
		reformatText = reformatText.replaceAll("\\|\\|", " \\|\\| ");

		/*reformatText = reformatText.replaceAll("\\s==\\s", "==");
		reformatText = reformatText.replaceAll("\\s<\\s", "<");
		reformatText = reformatText.replaceAll("\\s<=\\s", "<=");
		reformatText = reformatText.replaceAll("\\s>=\\s", ">=");
		reformatText = reformatText.replaceAll("\\s>\\s", ">");
		reformatText = reformatText.replaceAll("\\s<\\s", "<");
		reformatText = reformatText.replaceAll("\\s!=\\s", "!=");
		reformatText = reformatText.replaceAll("\\s:\\s", ":");*/

		if (!reformatText.startsWith("(") || !reformatText.endsWith(")")) {
			reformatText = "( " + reformatText + " )";
		}
		// System.out.println(reformatText);
		return reformatText;
	}

	protected void parseError(final String msg, final int index) throws ParseException {
		throw new ParseException(msg, index);
	}

	protected void next() {
		// delete empty strings
		while (scanner.hasNext() && (lookahead = scanner.next()).trim().equals("")) {
		}
		counter++;
	}

	private VPredicate<T> parseE() throws ParseException {
		if (lookahead.matches(regExpression) || lookahead.matches(regOpen) || lookahead.matches(regNot)) {
			VPredicate<T> t = parseT();
			if (lookahead.matches(regOr)) {
				// System.out.print("|| ");
				next();
				VPredicate<T> e = parseE();
				return s -> t.test(s) || e.test(s);
			}
			return t;
		} else {
			throw new ParseException("parseE(): require logic-expression or '(', found " + lookahead, counter);
		}
	}

	private VPredicate<T> parseT() throws ParseException {
		if (lookahead.matches(regExpression) || lookahead.matches(regOpen) || lookahead.matches(regNot)) {
			VPredicate<T> f = parseF();
			if (lookahead.matches(regAnd)) {
				// System.out.print("&& ");
				next();
				VPredicate<T> t = parseT();
				return s -> f.test(s) && t.test(s);
			}
			return f;
		} else {
			throw new ParseException("parseT(): require logic-expression or '(', found " + lookahead, counter);
		}
	}

	private VPredicate<T> parseF() throws ParseException {
		VPredicate<T> result = null;

		if (lookahead.matches(regExpression)) {
			// System.out.print("id:" + lookahead + " ");
			result = getPredicate(lookahead);
			next();
		} else if (lookahead.matches(regNot)) {
			// System.out.print("!");
			next();
			VPredicate<T> e = parseE();
			result = s -> !e.test(s);
		} else if (lookahead.matches(regOpen)) {
			// System.out.print("(");
			next();
			result = parseE();
			if (lookahead.matches(regClose)) {
				// System.out.print(")");
				next();
			} else {
				throw new ParseException("parseF(): require ')', found " + lookahead, counter);
			}
		} else {
			throw new ParseException("parseF(): require logic-expression, '(' or '!', found " + lookahead, counter);
		}
		return result;
	}

	protected abstract VPredicate<T> getPredicate(final String expression) throws ParseException;

	public VPredicate<T> parse() throws ParseException {
		try {
			next();
			VPredicate<T> result = parseF();
			counter = 0;
			return result;
		} catch (Exception e) {
			e.printStackTrace();
			throw new ParseException(e.getMessage(), 0);
		}
	}
}
