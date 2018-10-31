package org.vadere.meshing.mesh.gen;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Directed Acyclic graph. Each node of the DAG is a DAG itself.
 *
 * @param <E> type of the elements of the DAG.
 */
public class DAG<E> {

	/**
	 * The element of this DAG-Node.
	 */
	private final E element;

	/**
	 * The children of the DAG-Node.
	 */
	private final List<DAG<E>> children;

	/**
	 * Default constructor.
	 * @param element the element of the Dag-node
	 */
	public DAG(@NotNull final E element) {
		this.element = element;
		children = new ArrayList<>();
	}

	/**
	 * Returns all child nodes of the DAG.
	 * @return all child nodes of the DAG
	 */
	public List<DAG<E>> getChildren() {
		return children;
	}

	/**
	 * Adds a new element as child node to the DAG.
	 * @param child a new element
	 */
	public void addChild(E child) {
		this.children.add(new DAG<E>(child));
	}

	/**
	 * Adds a new Node to as child node to the DAG.
	 * @param child a new child node
	 */
	public void addChild(DAG<E> child) {
		this.children.add(child);
	}

	/**
	 * Returns the element of the DAG.
	 * @return the element of the DAG
	 */
	public E getElement() {
		return element;
	}

	public Collection<E> collectLeafs() {
		Collection<E> leafs = new ArrayList<>();
		LinkedList<DAG<E>> nodesToVisit = new LinkedList<>();
		nodesToVisit.add(this);

		while (!nodesToVisit.isEmpty()) {
			DAG<E> currentNode = nodesToVisit.removeLast();
			nodesToVisit.addAll(currentNode.children);

			if(currentNode.isLeaf())
				leafs.add(currentNode.getElement());
		}

		return leafs;
	}

	/**
	 * Test whether this DAG-Node is a child or not.
	 *
	 * @return true if this node is a child node, false otherwise.
	 */
	public boolean isLeaf(){
		return children.isEmpty();
	}

	/**
	 * Finds the first DAG-node element in a dept first fashion.
	 *
	 * @param test the predicate the element of the DAG-node has to fulfill.
	 *
	 * @return (optional) the first DAG-node element in a dept first fashion
	 */
	public Optional<E> findFirstElement(final Predicate<E> test){
		Optional<DAG<E>> optDag = findFirst(test);
		if(optDag.isPresent()) {
			return Optional.of(optDag.get().getElement());
		}
		else {
			return Optional.empty();
		}
	}

	/**
	 * Finds the first DAG-node in a dept first fashion.
	 * @param test the predicate the element of the DAG-node has to fulfill.
	 *
	 * @return (optional) the first DAG-node in a dept first fashion.
	 */
	public Optional<DAG<E>> findFirst(final Predicate<E> test){
		LinkedList<DAG<E>> nodesToVisit = new LinkedList<>();
		nodesToVisit.add(this);

		while(!nodesToVisit.isEmpty()) {
			DAG<E> currentNode = nodesToVisit.removeLast();
			if(test.test(currentNode.getElement())) {
				return Optional.of(currentNode);
			}
			nodesToVisit.addAll(currentNode.children);
		}

		return Optional.empty();
	}

	/**
	 * Returns the last node of a path of elements that satisfy the test.
	 * The path will be constructed in a dept first fashion, therefore there
	 * may exist other paths.
	 *
	 * @param test the test that has to be satisfied
	 * @return the last node of a path of elements that satisfy the test
	 */
	public Optional<DAG<E>> matchFirst(final Predicate<E> test) {

		DAG<E> currentNode = this;

		if(!test.test(currentNode.getElement())){
			return Optional.empty();
		}
		else {
			while(!currentNode.isLeaf()) {
				Optional<DAG<E>> opt = currentNode.children.stream().filter(node -> test.test(node.getElement())).findFirst();

				// we stop at the last path element we found
				if(!opt.isPresent()) {
					return Optional.of(currentNode);
				}
				else {
					currentNode = opt.get();
				}
			}
		}

		return Optional.of(currentNode);
	}

	/**
	 * Returns a set of Dag elements containing all leafs such that there is a path
	 * to the leaf and for each node on the path the condition is feasible including
	 * the leaf itself.
	 *
	 * @param test a condition which has to be feasible for the path from leaf to leaf
	 * @return a set of leaf Dag elements
	 */
	public Set<DAG<E>> matchAll(final Predicate<E> test) {
		Set<DAG<E>> leafs = new HashSet<>();
		LinkedList<DAG<E>> nodesToVisit = new LinkedList<>();
		nodesToVisit.add(this);

		while(!nodesToVisit.isEmpty()) {
			DAG<E> currentNode = nodesToVisit.removeLast();

			if(test.test(currentNode.getElement())) {
				nodesToVisit.addAll(currentNode.children);

				if(currentNode.isLeaf()) {
					leafs.add(currentNode);
				}
			}
		}

		return leafs;
	}
}
