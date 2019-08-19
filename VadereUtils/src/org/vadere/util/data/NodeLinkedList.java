package org.vadere.util.data;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * A implementation of a Linked List that supports accessible pointers (i.e. Node) to the elements of the Linked List.
 *
 * @author Benedikt Zoennchen
 *
 * @param <E> the type of the elements contained in the list
 */
public class NodeLinkedList<E> implements Iterable<Node<E>> {

    /**
     * the head of the list
     */
    Node<E> head;

    /**
     * the tail of the list
     */
    Node<E> tail;

    /**
     * Default constructor, initialize a new empty list.
     */
    public NodeLinkedList() {}

    /**
     * Construct a new Linked List, contain all elements in order.
     *
     * @param elements the elements
     */
    public NodeLinkedList(E[] elements) {
        this();
        for(E e : elements) {
            add(e);
        }
    }

    /**
     * Construct a new Linked List, contain all elements in order.
     *
     * @param elements the elements
     */
    public NodeLinkedList(Iterable<E> elements) {
        for(E e : elements) {
            add(e);
        }
    }

    /**
     * Converts the Linked List into a ArrayList.
     *
     * @return
     */
    public List<E> toList() {
        List<E> list = new ArrayList<>();
        for(Node<E> node : this) {
            list.add(node.getElement());
        }
        return list;
    }

    /**
     * Copy constructor.
     *
     * @param list the orignal Linked List.
     */
    private NodeLinkedList(final NodeLinkedList<E> list) {
        for(Node<E> node : list) {
            add(node.getElement());
        }
    }

    /**
     * Appends all elements to the end Linked List.
     *
     * @param tailList the elements
     */
    public void append(final List<E> tailList) {
        for(E element : tailList) {
            add(element);
        }
    }

    /**
     * Appends all elements to the end Linked List.
     *
     * @param tailList the elements
     */
    public void append(final NodeLinkedList<E> tailList) {
        for(Node<E> node : tailList) {
            add(node.getElement());
        }
    }

    /**
     * Suspend all elements to the beginning of the Linked List, i.e.
     * assume the headList = [A,B,C] and this = [E,F,D] the result is [A,B,C,E,F,D].
     *
     * @param headList the elements
     */
    public void suspend(final LinkedList<E> headList) {
        Iterator<E> descendingIterator = headList.descendingIterator();
        while (descendingIterator.hasNext()) {
            addFirst(descendingIterator.next());
        }
    }

    /**
     * Suspend all elements to the beginning of the Linked List, i.e.
     * assume the headList = [A,B,C] and this = [E,F,D] the result is [A,B,C,E,F,D].
     *
     * @param headList the elements
     */
    public void suspend(final NodeLinkedList<E> headList) {
        Iterator<Node<E>> descendingIterator = headList.descendingIterator();
        while (descendingIterator.hasNext()) {
            addFirst(descendingIterator.next().getElement());
        }
    }

    /**
     * Returns the pointer to the head of the Linked List.
     *
     * @return the pointer to the head of the Linked List
     */
    public Node<E> getHead() {
        return head;
    }

    /**
     * Returns the pointer to the tail of the Linked List.
     *
     * @return the pointer to the tail of the Linked List
     */
    public Node<E> getTail() {
        return tail;
    }

    /**
     * Returns true, if the Linked List is empty, otherwise false.
     *
     * @return true, if the Linked List is empty, otherwise false
     */
    public boolean isEmpty() {
        return head == null;
    }

    /**
     * Returns a pointer to the k^th element of the Linked List (from the left).
     *
     * @param k, the number of the element
     * @return a pointer to the k^th element of the Linked List (from the left)
     */
    public Node<E> get(final int k) {
        if(k < 0) {
           throw new IndexOutOfBoundsException("index < 0.");
        }
        else {
            int i = 0;
            for(Node<E> node : this) {
                if(i == k) {
                    return node;
                }
                i++;
                if(i > k) {
                    throw new IndexOutOfBoundsException("i >= size of the list.");
                }
            }
        }
        throw new RuntimeException("this should never happen!");
    }

    /**
     * Inserts the element before the head of the Linked List. The element becomes the new head.
     *
     * @param element the element which will be inserted
     */
    public Node<E> addFirst(final E element) {
        if(head == null) {
            tail = new Node<>(this, element);
            head = tail;
        }
        else {
            head = insertPrevious(element, head);
        }
	    return head;
    }

    /**
     * Inserts the element after the tail of the Linked List. The element becomes the new tail.
     *
     * @param element the element which will be inserted
     */
    public Node<E> addLast(E element) {
        if(head == null) {
            return addFirst(element);
        }
        else {
            tail = insertNext(element, tail);
            return tail;
        }
    }

    /**
     * Inserts the element after the tail of the Linked List. The element becomes the new tail.
     *
     * @param element the element which will be inserted
     */
    public Node<E> add(E element) {
        return addLast(element);
    }

    /**
     * Inserts an element next to the anchor. Note the anchor has to be part of this list,
     * otherwise the resulting Linked List becomes invalid.
     *
     * @param element   the element to that will be inserted
     * @param anchor    the pointer that points to the position at which the element will inserted (to the right)
     * @return the pointer that points to the element
     */
    public Node<E> insertNext(final E element, final Node<E> anchor) {
        Node<E> node = new Node<>(this, element);
        if(anchor.hasNext()) {
            anchor.next.prev = node;
        }

        node.next = anchor.next;
        node.prev = anchor;
        anchor.next = node;

        if(anchor.identical(tail)) {
            tail = node;
        }

        return node;
    }

    /**
     * Inserts an element before the anchor. Note the anchor has to be part of this list,
     * otherwise the resulting Linked List becomes invalid.
     *
     * @param element   the element to that will be inserted
     * @param anchor    the pointer that points to the position at which the element will inserted (to the left)
     * @return the pointer that points to the element
     */
    public Node<E> insertPrevious(final E element, final Node<E> anchor) {
        Node<E> node = new Node<>(this, element);
        if(anchor.hasPrev()) {
            anchor.prev.next = node;
        }

        node.prev = anchor.prev;
        node.next = anchor;
        anchor.prev = node;

        if(anchor.identical(head)) {
            head = node;
        }
        return node;
    }

    /**
     * Splits the Linked List into two lists. The split takes part at the first element
     * that satisfy the predicate. This element will be part of the first list.
     * The first list will be returned and this list will be the remaining part.
     *
     * @param predicate the predicate the split element has to satisfy
     * @return the first part of the split (a new Linked List)
     */
    public NodeLinkedList<E> split(final Predicate<E> predicate) {
        NodeLinkedList<E> firstPart = new NodeLinkedList<E>();
        Optional<Node<E>> optFind = findFirst(predicate);
        if(optFind.isPresent()) {
            firstPart.head = head;
            firstPart.tail = optFind.get();
            if(firstPart.tail.hasNext()) {
                head = firstPart.tail.getNext();
                firstPart.tail.next = null;
                head.prev = null;
            }
            else {
                head = null;
                tail = null;
            }
        }
        return firstPart;
    }

    /**
     * Returns the first element (from the left) that satisfy the predicate condition.
     * @param predicate the predicate condition
     * @return the first element (from the left) that satisfy the predicate condition
     */
    public Optional<Node<E>> findFirst(final Predicate<E> predicate) {
        for(Node<E> node : this) {
            if(predicate.test(node.getElement())) {
                return Optional.of(node);
            }
        }
        return Optional.empty();
    }

    /**
     * Returns the last element (from the left) that satisfy the predicate condition.
     *
     * @param predicate the predicate condition
     * @return the first element (from the left) that satisfy the predicate condition
     */
    public Optional<Node<E>> findLast(final Predicate<E> predicate) {
        Node<E> node = tail;
        while(node != null) {
            if(predicate.test(node.getElement())) {
                return Optional.of(node);
            }
            else {
                node = node.prev;
            }
        }
        return Optional.empty();
    }

    /**
     * Returns a set containing all elements satisfying the predicate.
     *
     * @param predicate the predicate condition
     * @return a set containing all elements satisfying the predicate
     */
    public Set<E> findAllDistinct(final Predicate<E> predicate) {
        Set<E> set = new HashSet<>();
        for(Node<E> node : this) {
            if(predicate.test(node.getElement())) {
                set.add(node.getElement());
            }
        }
        return set;
    }

    /**
     * Returns a list containing all elements satisfying the predicate.
     * The list may contain duplicated entries.
     *
     * @param predicate the predicate condition
     * @return a list containing all elements satisfying the predicate
     */
    public List<E> findAll(final Predicate<E> predicate) {
        List<E> list = new ArrayList<>();
        for(Node<E> node : this) {
            if(predicate.test(node.getElement())) {
                list.add(node.getElement());
            }
        }
        return list;
    }

    /**
     * Returns a list containing all pointers to elements satisfying the predicate.
     *
     * @param predicate the predicate condition
     * @return a list containing all pointers to elements satisfying the predicate
     */
    public List<Node<E>> findAllPointers(final Predicate<E> predicate) {
        List<Node<E>> list = new ArrayList<>();
        for(Node<E> node : this) {
            if(predicate.test(node.getElement())) {
                list.add(node);
            }
        }
        return list;
    }

    /**
     * Returns true if the Linked List contains an element satisfying the predicate, otherwise false.
     *
     * @param predicate the predicate condition
     * @return true if the Linked List contains an element satisfying the predicate, otherwise false
     */
    public boolean contains(final Predicate<E> predicate) {
        return findFirst(predicate).isPresent();
    }

    /**
     * Returns true if the Linked List contains the element.
     *
     * @param element the element we test for containment
     * @return true if the Linked List contains the element, otherwise false
     */
    public boolean contains(final E element) {
        return contains(e -> e.equals(element));
    }

    /**
     * Removes all elements in the Linked List that satisfy the predicate condition.
     *
     * @param predicate the predicate condition
     * @return the number of deleted elements
     */
    public int removeAll(final Predicate<E> predicate) {
        Iterator<Node<E>> iterator = iterator();
        int count = 0;
        while(iterator.hasNext()) {
            Node<E> node = iterator.next();
            if(predicate.test(node.getElement())) {
                iterator.remove();
                count++;
            }
        }
        return count;
    }

    /**
     * Replaces all elements in the Linked List that satisfy the predicate condition by an element.
     *
     * @param predicate     the predicate condition
     * @param replacement   the element that will replace the elements
     * @return a List of pointers pointing to the replaced (new) elements
     */
    public List<Node<E>> replaceAll(final Predicate<E> predicate, final E replacement) {
        List<Node<E>> pointers = new LinkedList<>();
        if(!isEmpty()) {
            Node<E> current = null;
            while((!isEmpty() && current == null) || current.hasNext()) {
                if(current == null) {
                    current = head;
                }
                else {
                    current = current.getNext();
                }

                if(predicate.test(current.getElement())) {
                    current.setElement(replacement);
                    pointers.add(current);
                }
            }
        }
        return pointers;
    }

    /**
     * Computes the prefix length of elements satisfying the predicate condition.
     *
     * @param predicate the predicate condition
     * @param counter   the function that defines the counting of elements
     * @return the length of the prefix defined by the Function counter
     */
    public long prefixLen(final Predicate<E> predicate, final Function<E, Long> counter) {
        return prefixLen(predicate, iterator(), counter);
    }

    /**
     * Computes the suffix length of elements satisfying the predicate condition.
     *
     * @param predicate the predicate condition
     * @param counter   the function that defines the counting of elements
     * @return the length of the suffix defined by the Function counter
     */
    public long suffixLen(final Predicate<E> predicate, final Function<E, Long> counter) {
        return prefixLen(predicate, descendingIterator(), counter);
    }

    /**
     * Computes the prefix length of elements satisfying the predicate condition and removes
     * this prefix from the Linked List.
     *
     * @param predicate the predicate condition
     * @param counter   the function that defines the counting of elements
     * @return the length of the prefix defined by the Function counter
     */
    public long removePrefix(final Predicate<E> predicate, final Function<E, Long> counter) {
        return removePrefix(predicate, iterator(), counter);
    }

    /**
     * Computes the suffix length of elements satisfying the predicate condition and removes
     * this suffix from the Linked List.
     *
     * @param predicate the predicate condition
     * @param counter   the function that defines the counting of elements
     * @return the length of the suffix defined by the Function counter
     */
    public long removeSuffix(final Predicate<E> predicate, final Function<E, Long> counter) {
        return removePrefix(predicate, descendingIterator(), counter);
    }

    /**
     * Removes the head of the list and return its value.
     * @return the value/element of the head.
     */
    public E removeHead() {
        E element = head.getElement();
        head.remove();
        return element;
    }

    /**
     * Removes the tail of the list and return its value.
     * @return the value/element of the tail.
     */
    public E removeTail() {
        E element = tail.getElement();
        tail.remove();
        return element;
    }

    /**
     * Computes and returns the size of the linked list.
     * Complexity: O(|n|), where n is the numbers of elements inside the list.
     *
     * @return the size of the list, i.e. the numbers of elements contained in this list
     */
    public int size() {
        int size = 0;
        for(Node<E> node : this) {
            size++;
        }
        return size;
    }

    /**
     * Returns a stream of pointers that point to elements of the Linked List.
     *
     * @return a stream of pointers that point to elements of the Linked List
     */
    public Stream<Node<E>> stream() {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator(), Spliterator.ORDERED), false);
    }

    /**
     * Returns a clone of the Linked List. Only the pointers will be cloned not th elements.
     *
     * @return a (soft) clone of the Linked List
     */
    public NodeLinkedList<E> clone() {
        return new NodeLinkedList<>(this);
    }

    /**
     * Returns an Iterator of pointers that iterates pointers in descending order, i.e. from right to left.
     * @return an Iterator of pointers that iterates pointers in descending order
     */
    public Iterator<Node<E>> descendingIterator () {
        return new NodeDescendingIterator();
    }

    /**
     * Returns an Iterator of pointers that iterates pointers in ascending order, i.e. from left to right.
     * @return an Iterator of pointers that iterates pointers in ascending order
     */
    public Iterator<E> elementIterator() {
        return new ElementIterator();
    }

    /**
     * Returns an Iterable of pointers that iterates pointers in ascending order, i.e. from left to right.
     * @return an Iterable of pointers that iterates pointers in ascending order
     */
    public Iterable<E> elementIterable() {
        return () -> new ElementIterator();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        for(Node<E> node : this) {
            builder.append(node.getElement());
        }
        return builder.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == null) {
            return false;
        }
        else if(obj.getClass() != this.getClass()) {
            return false;
        }
        else {
            NodeLinkedList other = (NodeLinkedList)obj;
            Iterator<Node<E>> it = this.iterator();
            Iterator<Node> otherIt = other.iterator();

            while (it.hasNext() && otherIt.hasNext()) {
                Node<E> node = it.next();
                Node otherNode = otherIt.next();
                if(!node.equals(otherNode)) {
                    return false;
                }
            }

            return !it.hasNext() && !otherIt.hasNext();
        }
    }

    @Override
    public Iterator<Node<E>> iterator() {
        return new NodeIterator();
    }

    private long removePrefix(final Predicate<E> predicate, final Iterator<Node<E>> iterator) {
        return removePrefix(predicate, iterator, e -> 1L);
    }

    private long removePrefix(final Predicate<E> predicate, final Iterator<Node<E>> iterator, final Function<E, Long> counter) {
        return prefix(predicate, iterator, counter, it -> it.remove());
    }

    private long prefixLen(final Predicate<E> predicate, final Iterator<Node<E>> iterator, final Function<E, Long> counter) {
        return prefix(predicate, iterator, counter, it -> {
        });
    }

    private long prefix(final Predicate<E> predicate, final Iterator<Node<E>> iterator, final Function<E, Long> counter, final Consumer<Iterator> consumer) {
        long count = 0;
        while(iterator.hasNext()) {
            Node<E> node = iterator.next();
            if(predicate.test(node.getElement())) {
                consumer.accept(iterator);
                //iterator.remove();
                count += counter.apply(node.getElement());
            }
            else {
                break;
            }
        }
        return count;
    }

    /**
     * Iterator iterating over all pointers of the Linked List from right to left.
     */
    private class NodeDescendingIterator implements Iterator<Node<E>> {
        private Node<E> current;
        private boolean started = false;
        private Node<E> prev = null;

        private NodeDescendingIterator() {
            current = null;
        }

        @Override
        public void remove() {
            prev = current.getPrev();;
            current.remove();
            current = null;
        }

        @Override
        public boolean hasNext() {
            if(!started) {
                return tail != null;
            }
            else {
                return prev != null || (current != null && current.hasPrev());
            }
        }

        @Override
        public Node<E> next() {
            if(!started) {
                started = true;
                current = tail;
            }
            else if(prev != null) {
                current = prev;
                prev = null;
            }
            else {
                current = current.getPrev();
            }

            return current;
        }
    }

    /**
     * Iterator iterating over all elements of the Linked List from left to right.
     */
    private class ElementIterator implements Iterator<E> {

        private Iterator<Node<E>> nodeIterator;

        private ElementIterator() {
            nodeIterator = iterator();
        }

        @Override
        public boolean hasNext() {
            return nodeIterator.hasNext();
        }

        @Override
        public E next() {
            return nodeIterator.next().getElement();
        }
    }

    /**
     * Iterator iterating over all pointers of the Linked List from left to right.
     */
    private class NodeIterator implements Iterator<Node<E>> {

        private Node<E> current;
        private boolean started = false;
        private Node<E> next = null;

        private NodeIterator() {
            current = null;
        }

        @Override
        public void remove() {
            next = current.getNext();;
            current.remove();
            current = null;
        }

        @Override
        public boolean hasNext() {
            if(!started) {
                return head != null;
            }
            else {
                return next != null || (current != null && current.hasNext());
            }
        }

        @Override
        public Node<E> next() {
            if(!started) {
                started = true;
                current = head;
            }
            else if(next != null) {
                current = next;
                next = null;
            }
            else {
                current = current.getNext();
            }

            return current;
        }
    }
}
