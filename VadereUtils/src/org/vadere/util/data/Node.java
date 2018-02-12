package org.vadere.util.data;

import java.util.Iterator;
import java.util.function.Predicate;

/**
 * A pointer of MyLinkedList.
 *
 * @author Benedikt Zoennchen
 *
 * @param <E> the type of the element this pointer pointing at
 */
public class Node<E> {

    /**
     * The element the pointer point to.
     */
    private E element;

    private boolean alive;

    /**
     * The pointer right to this pointer (possibly null).
     */
    Node<E> next;

    /**
     * The pointer left to this pointer (possibly null).
     */
    Node<E> prev;

    /**
     * The Linked List, this pointer is part of.
     */
    private final NodeLinkedList list;

    /**
     * Initialize a new pointer that will be part of the list and point to element
     *
     * @param list      the Linked List this pointer is part of
     * @param element   the element the pointer pointing at
     */
    Node(final NodeLinkedList list, final E element) {
        this.element = element;
        this.list = list;
        this.alive = true;
    }

    /**
     * Returns the element the pointer pointing at.
     *
     * @return the element the pointer pointing at
     */
    public E getElement() {
        return element;
    }

    /**
     * Replaces the current element the pointer pointing at by element.
     *
     * @param element the element that replaces the element the pointer pointing at
     */
    public void setElement(final E element) {
        this.element = element;
    }

    /**
     * Removes the pointer from its list (and the element).
     */
    public void remove() {
        if(prev != null) {
            prev.next = next;
        }
        else {
            list.head = next;
        }

        if(next != null) {
            next.prev = prev;
        }
        else {
            list.tail = prev;
        }
        disconnect();
        alive = false;
    }

    /**
     * Removes the first next element satisfying the predicate condition.
     *
     * @param predicate the predicate condition
     */
    public void removeNext(final Predicate<E> predicate) {
        Node<E> current = this;
        while(current != null && predicate.test(current.getElement())) {
            Node<E> next = current.getNext();
            current.remove();
            current = next;
        }
    }

	public boolean isAlive() {
		return alive;
	}

	/**
     * Inserts an element to the left of the pointer.
     *
     * @param element the element that will be inserted
     */
    public void insertPrevious(final E element) {
        list.insertPrevious(element, this);
    }

    /**
     * Inserts a whole MyLinkedList of elements before the node i.e.
     * assume elements = [A,B,C] and the Linked list of the node = [D,E,F]
     * and the node = E the result will be [D,A,B,C,E,F].
     *
     * @param elements the list that will be suspend to this list.
     */
    public void insertPrevious(final NodeLinkedList<E> elements) {
        Iterator<Node<E>> iterator = elements.iterator();
        while (iterator.hasNext()) {
            insertPrevious(iterator.next().getElement());
        }
    }

    /**
     * Insert an element next to the pointer.
     *
     * @param element the element that will be inserted
     */
    public void insertNext(final E element) {
        list.insertNext(element, this);
    }

    /**
     * Inserts a whole MyLinkedList of elements after the node i.e.
     * assume elements = [A,B,C] and the Linked list of the node = [D,E,F]
     * and the node = E the result will be [D,E,A,B,C,F].
     *
     * @param elements the list that will be suspend to this list.
     */
    public void insertNext(final NodeLinkedList<E> elements) {
        Iterator<Node<E>> descendingIterator = elements.descendingIterator();
        while (descendingIterator.hasNext()) {
            insertNext(descendingIterator.next().getElement());
        }
    }

    /**
     * Returns the pointer next to this pointer.
     *
     * @return the pointer next to this pointer
     */
    public Node<E> getNext() {
        return next;
    }

    /**
     * Returns the pointer left to this pointer.
     *
     * @return the pointer left to this pointer
     */
    public Node<E> getPrev() {
        return prev;
    }

    /**
     * Returns true if there is a pointer next to this pointer.
     *
     * @return true if there is a pointer next to this pointer, otherwise false
     */
    public boolean hasNext() {
        return next != null;
    }

    /**
     * Returns true if there is a pointer left to this pointer.
     *
     * @return true if there is a pointer left to this pointer, otherwise false
     */
    public boolean hasPrev() {
        return prev != null;
    }

    /**
     * Removes the pointer from its Linked List.
     */
    private void disconnect() {
        this.next = null;
        this.prev = null;
    }

    @Override
    public String toString() {
        return (hasPrev() ? "<->" : "") + getElement().toString() + (hasNext() ? "<->" : "");
    }

    /**
     * Returns true if the reference of the pointer is equal to the reference of the object.
     *
     * @param object the object we compare this pointer with.
     * @return true if the reference of the pointer is equal to the reference of the object
     */
    public boolean identical(final Object object) {
        return object == this;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == null) {
            return false;
        }
        else if(obj.getClass() != getClass()) {
            return false;
        }
        else {
            Node node = (Node)obj;
            return this.getElement() == null && node.getElement() == null || (this.getElement() != null && this.getElement().equals(node.getElement()));
        }
    }
}
