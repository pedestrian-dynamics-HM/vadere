package org.vadere.state.attributes.processor;

/**
 * @author Benedikt Zoennchen
 */
public class AttributesTestProcessor extends AttributesProcessor {

	public enum Result {SUCCESS, FAIL, SUCCESS_OR_FAIL}

	private Result expectedResult = Result.SUCCESS;

	public void setExpectedResult(final Result expectedResult) {
		checkSealed();
		this.expectedResult = expectedResult;
	}

	public Result getExpectedResult() {
		return expectedResult;
	}
}
