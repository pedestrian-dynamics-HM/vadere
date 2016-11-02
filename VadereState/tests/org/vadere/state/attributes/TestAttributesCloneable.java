package org.vadere.state.attributes;

import static org.junit.Assert.*;

import org.junit.Test;

public class TestAttributesCloneable {
	
	private static class SimpleAttributes extends Attributes {
		int id = 1;
	}

	private static class NestedAttributes extends Attributes {
		int id = 1;
		SimpleAttributes c = new SimpleAttributes();
		SimpleCloneable d = new SimpleCloneable();
	}

	private static class SimpleCloneable implements Cloneable {
		int id = 1;
	}

	private static class NotCloneableClass {
		@Override
		public Object clone() throws CloneNotSupportedException {
			return super.clone(); // throws exception because it does not implement Cloneable
		}
	}

	@Test
	public void testSimpleCloneable() {
		try {
			SimpleAttributes a = new SimpleAttributes();
			SimpleAttributes b = (SimpleAttributes) a.cloneAttributes();
			assertEquals(a.id, b.id);
		} catch (Exception e) {
			fail("clone should not throw exception");
		}
	}

	@Test
	public void testExtendedCloneable() {
		try {
			NestedAttributes a = new NestedAttributes();
			NestedAttributes b = (NestedAttributes) a.cloneAttributes();
			assertEquals(a.id, b.id);
			assertEquals(a.c.id, b.c.id);
			assertEquals(a.d.id, b.d.id);
		} catch (Exception e) {
			fail("clone should not throw exception");
		}
	}
	
	@Test(expected=CloneNotSupportedException.class)
	public void testNotCloneableClass() throws CloneNotSupportedException {
		new NotCloneableClass().clone();
	}

}
