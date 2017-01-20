package org.vadere.util.data;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public class DAG<E> {
	private final E root;
	private final List<DAG<E>> children;

	public DAG(@NotNull final E element) {
		root = element;
		children = new ArrayList<>();
	}

	public List<DAG<E>> getChildren() {
		return children;
	}

	public void addChild(E child) {
		this.children.add(new DAG<E>(child));
	}

	public void addChild(DAG<E> child) {
		this.children.add(child);
	}

	public E getRoot() {
		return root;
	}

	public Collection<E> collectLeafs() {
		Collection<E> leafs = new ArrayList<E>();
		LinkedList<DAG<E>> nodesToVisit = new LinkedList<>();
		nodesToVisit.add(this);

		while (!nodesToVisit.isEmpty()) {
			DAG<E> currentNode = nodesToVisit.removeFirst();
			nodesToVisit.addAll(currentNode.children);

			if(currentNode.isLeaf())
				leafs.add(currentNode.getRoot());
		}

		return leafs;
	}

	public boolean isLeaf(){
		return children.isEmpty();
	}

	/**
	 * Finds the first DAG-node element in a dept first fashion.
	 * @param test the predicate the element of the DAG-node has to fullfill.
	 * @return
	 */
	public Optional<E> findFirstElement(final Predicate<E> test){
		Optional<DAG<E>> optDag = findFirst(test);
		if(optDag.isPresent()) {
			return Optional.of(optDag.get().getRoot());
		}
		else {
			return Optional.empty();
		}
	}

	/**
	 * Finds the first DAG-node in a dept first fashion.
	 * @param test the predicate the element of the DAG-node has to fullfill.
	 * @return
	 */
	public Optional<DAG<E>> findFirst(final Predicate<E> test){
		if(test.test(root)) {
			return Optional.of(this);
		}
		else {
			return children.stream().map(child -> child.findFirst(test)).filter(opt -> opt.isPresent()).map(opt -> opt.get()).findFirst();
		}
	}

	public Optional<DAG<E>> matchAll(final Predicate<E> test) {
		if(test.test(root)) {
			if(isLeaf()) {
				return Optional.of(this);
			}
			else {
				return children.stream().map(child -> child.matchAll(test)).filter(opt -> opt.isPresent()).map(opt -> opt.get()).findFirst();
			}
		}
		return Optional.empty();
	}
}
