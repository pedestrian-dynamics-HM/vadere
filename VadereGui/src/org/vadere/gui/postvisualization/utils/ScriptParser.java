package org.vadere.gui.postvisualization.utils;

import java.text.ParseException;
import java.util.LinkedList;

public class ScriptParser {
	private String code;
	private LinkedList<String> parsedTokens;
	private LinkedList<String> tokens;
	private int index;

	public static void main(String[] args) {
		ScriptParser parser = new ScriptParser();
		try {
			LinkedList<String> list = parser.parse("pedestrian.id==1 && (asd==6 || b)");
			LinkedList<String> tokens = parser.tokenize("pedestrian.id==1 && (asd==6 || b)");
			System.out.println(tokens);
			System.out.println(list);
		} catch (ParseException e) {
			e.printStackTrace();
		}
	}

	public ScriptParser() {
		this.parsedTokens = new LinkedList<>();
		this.index = 0;
	}

	/*
	 * public boolean logicalEval(final String expression) throws ParseException {
	 * tokens = tokenize(expression);
	 * return logicalEval(tokens);
	 * }
	 */

	/*
	 * private boolean logicalEval(final LinkedList<String> tokens) {
	 * String token = nextToken();
	 * boolean result = true;
	 * if(token.equals("!")) {
	 * return !logicalEval(tokens);
	 * }
	 * else if(token.equals("&&")) {
	 * result = logicalEval(tokens);
	 * }
	 * else {
	 * return evalExpression(token);
	 * }
	 * }
	 * 
	 * private boolean evalExpression(final String expression) {
	 * 
	 * }
	 */

	private String nextToken() {
		return tokens.removeFirst();
	}

	public LinkedList<String> parse(final String code) throws ParseException {
		return parse(tokenize(code));
	}

	private LinkedList<String> parse(final LinkedList<String> tokens) {

		if (!tokens.isEmpty()) {
			String token = tokens.removeFirst();
			if (token.equals("&&") || token.equals("||")) {
				LinkedList tmp = parse(tokens);
				tmp.addLast(token);
				return tmp;
			} else {
				LinkedList tmp = parse(tokens);
				tmp.addLast(token);
				return tmp;
			}
		} else {
			return new LinkedList<>();
		}
	}

	private LinkedList<String> tokenize(final String code) throws ParseException {
		LinkedList<String> tokens = new LinkedList<>();
		String token = "";
		for (int i = 0; i < code.length(); i++) {
			char c = code.charAt(i);

			switch (c) {
				case '!':
				case ')':
				case '(':
					if (!token.isEmpty()) {
						tokens.add(token);
						token = "";
					}
					tokens.add(c + "");
					break;
				case '&': {
					if (token.equals("&")) {
						tokens.add("&&");
						token = "";
					} else if (!token.isEmpty()) {
						tokens.add(token);
						token = "&";
					}
				}
					break;
				case '|': {
					if (token.equals("|")) {
						tokens.add("||");
						token = "";
					} else if (!token.isEmpty()) {
						tokens.add(token);
						token = "|";
					}
				}
					break;
				case ' ': {
				}
					break;
				default: {
					if (token.equals("&") || token.equals("|")) {
						throw new ParseException("missing " + token + " after " + token, i);
					}
					token += c;
				}
			}
		}

		if (!token.isEmpty()) {
			tokens.add(token);
		}
		return tokens;
	}

}
