package org.vadere.simulator.projects.dataprocessing.processor.tests;

import org.jetbrains.annotations.NotNull;
import org.vadere.simulator.projects.dataprocessing.datakey.NoDataKey;
import org.vadere.simulator.projects.dataprocessing.processor.DataProcessor;
import org.vadere.state.attributes.processor.AttributesTestProcessor;
import org.vadere.util.logging.Logger;

/**
 * @author Benedikt Zoennchen
 */
public abstract class TestProcessor extends DataProcessor<NoDataKey, Boolean> {

	private static Logger logger = Logger.getLogger(TestProcessor.class);

	public TestProcessor(@NotNull final String header){
		super(header);
	}

	@Override
	public AttributesTestProcessor getAttributes() {
		return (AttributesTestProcessor)super.getAttributes();
	}

	protected void handleAssertion(final boolean assertion, final String condition) {
		putValue(NoDataKey.key(), assertion);

		switch (getAttributes().getExpectedResult()) {
			case SUCCESS:
				if(!assertion)
					logger.warn("assertion violated for " + getClass().getSimpleName() + ":" + condition);
				assert assertion : condition;
				break;
			case FAIL:
				if(assertion)
					logger.warn("assertion which should be violated is not violated for " + getClass().getSimpleName() + ":" + condition);
				assert !assertion : "!(" + condition + ")";
				break;
			default: break;
		}
	}

	protected void handleAssertion(final boolean assertion) {
		handleAssertion(assertion, "");
	}
}
