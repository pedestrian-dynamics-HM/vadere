package org.vadere.simulator.projects.dataprocessing.processor.tests;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.vadere.simulator.projects.dataprocessing.datakey.NoDataKey;
import org.vadere.simulator.projects.dataprocessing.processor.DataProcessor;
import org.vadere.state.attributes.processor.AttributesTestProcessor;

/**
 * @author Benedikt Zoennchen
 */
public abstract class TestProcessor extends DataProcessor<NoDataKey, Boolean> {

	private static Logger logger = LogManager.getLogger(TestProcessor.class);

	public TestProcessor(@NotNull final String header){
		super(header);
	}

	@Override
	public AttributesTestProcessor getAttributes() {
		return (AttributesTestProcessor)super.getAttributes();
	}

	protected void handleAssertion(final boolean assertion) {
		putValue(NoDataKey.key(), assertion);

		switch (getAttributes().getExpectedResult()) {
			case SUCCESS:
				if(!assertion)
					logger.warn("assertion violated");
				assert assertion;
				break;
			case FAIL:
				if(assertion)
					logger.warn("assertion which should be violated is not violated.");
				assert !assertion; break;
			default: break;
		}

		assert assertion;
	}
}
