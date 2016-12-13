package org.vadere.state.attributes;

import org.junit.Test;

public class TestAttributesCloneable {
	
	private static class NotCloneableClass {
		@Override
		public Object clone() throws CloneNotSupportedException {
			return super.clone(); // throws exception because it does not implement Cloneable
		}
	}

	@Test(expected=CloneNotSupportedException.class)
	public void testNotCloneableClass() throws CloneNotSupportedException {
		new NotCloneableClass().clone();
	}

}
