package org.vadere.state.psychology;

import org.junit.Before;
import org.junit.Test;
import org.vadere.state.psychology.information.KnowledgeBase;
import org.vadere.state.psychology.perception.types.KnowledgeItem;

import java.util.ArrayList;
import java.util.regex.Pattern;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.*;

public class KnowledgeBaseTest {


	KnowledgeItem i1;
	KnowledgeItem i2;
	KnowledgeItem i3;

	@Before
	public void init(){
		i1 = new KnowledgeItem(1.0, 2.0, "i001");
		i2 = new KnowledgeItem(1.0, 3.0, "i002");
		i3 = new KnowledgeItem(1.0, 4.0, "i003");
	}

	@Test
	public void update_obsolete() {
		KnowledgeBase base = new KnowledgeBase();
		base.addInformation(i1);
		base.addInformation(i2);
		base.addInformation(i3);
		base.updateObsolete(2.1);
		assertThat(base.getKnowledge().size(), equalTo(2));
		assertFalse(base.knowsAbout(i1.getInformationId()));
		assertTrue(base.knowsAbout(i2.getInformationId()));
		assertTrue(base.knowsAbout(i3.getInformationId()));
	}

	@Test
	public void knowsAbout() {
		KnowledgeBase base = new KnowledgeBase();
		base.addInformation(i1);
		assertTrue(base.knowsAbout(i1.getInformationId()));
		assertFalse(base.knowsAbout("XXX"));
	}

	@Test
	public void knowsAboutPattern() {
		KnowledgeBase base = new KnowledgeBase();
		base.addInformation(i1);
		assertTrue(base.knowsAbout(Pattern.compile("i\\d\\d1")));
		assertFalse(base.knowsAbout(Pattern.compile("i\\d\\d5")));
	}

	@Test
	public void addInformation() {
		KnowledgeBase base = new KnowledgeBase();
		base.addInformation(i1);
		assertEquals(base.getKnowledge().get(0), i1);
	}

	@Test
	public void removeInformation() {
		ArrayList<KnowledgeItem> items = new ArrayList<>();
		items.add(i1);
		items.add(i3);
		KnowledgeBase base = new KnowledgeBase();
		base.addInformation(i1);
		base.addInformation(i2);
		base.addInformation(i3);
		base.removeInformation(i2.getInformationId());
		assertEquals(items, base.getKnowledge());
	}

	@Test
	public void removeInformation2() {
		ArrayList<KnowledgeItem> items = new ArrayList<>();
		items.add(i1);
		items.add(i3);
		KnowledgeBase base = new KnowledgeBase();
		base.addInformation(i1);
		base.addInformation(i2);
		base.addInformation(i3);
		base.removeInformation(Pattern.compile("i\\d\\d2"));
		assertEquals(items, base.getKnowledge());
	}

	@Test
	public void getKnowledge() {
		ArrayList<KnowledgeItem> items = new ArrayList<>();
		items.add(i1);
		items.add(i2);
		items.add(i3);
		KnowledgeBase base = new KnowledgeBase();
		base.addInformation(i1);
		base.addInformation(i2);
		base.addInformation(i3);
		assertEquals(items, base.getKnowledge());

	}
}