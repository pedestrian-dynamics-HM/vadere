package org.vadere.gui.topographycreator.utils;

import org.vadere.state.scenario.ScenarioElement;

import java.util.Optional;

public class TreeElementWrapper {

	public enum TreeElementType{
		ROOT,
		CATEGORY,
		LEAF
	}

	private ScenarioElement element;
	private String title;
	private TreeElementType treeElementType;

	public static TreeElementWrapper root(String title){
		return new TreeElementWrapper(title, TreeElementType.ROOT);
	}

	public static TreeElementWrapper category(String title){
		return new TreeElementWrapper(title, TreeElementType.CATEGORY);
	}

	public static TreeElementWrapper leaf(ScenarioElement element){
		return new TreeElementWrapper(element, TreeElementType.LEAF);
	}

	private TreeElementWrapper(String title, TreeElementType type) {
		this.element = null;
		this.title = title;
		this.treeElementType = type;
	}

	public boolean isType(TreeElementType t){
		return treeElementType.equals(t);
	}

	private TreeElementWrapper(ScenarioElement element,  TreeElementType type) {
		this.element = element;
		this.title = String.format("%s (%s)", element.getType().name().toLowerCase(), element.getId());
		this.treeElementType = type;
	}

	public Optional<ScenarioElement> getElement() {
		return Optional.of(element);
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public boolean isRoot(){
		return element == null;
	}


	@Override
	public String toString() {
		return  title;
	}

	public boolean compareWithScenarioElement(final ScenarioElement e){
		if (e == null || element == null) return false;
		return element.getType() == e.getType() && element.getId() == e.getId();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || element == null || getClass() != o.getClass()) return false;
		TreeElementWrapper that = (TreeElementWrapper) o;
		return element.getType() == that.element.getType() && element.getId() == that.element.getId();
	}

}
