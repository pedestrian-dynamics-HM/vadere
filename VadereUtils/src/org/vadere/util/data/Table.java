package org.vadere.util.data;

import org.vadere.util.logging.Logger;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * The {@link Table} is an abstract data type with a fixed column size (fixed numbers of columns and
 * fixed column names).
 * Null entries are not allowed. If you try to get an value from an entry outside the range this may
 * result
 * in an {@link IllegalArgumentException}. Column names are unique. It is possible to merge a table
 * in an
 * existing table, if they both has the same number of rows. One can only insert values at the end
 * of the table.
 * You have to complete a whole row before you can insert another one. The only possibility to
 * delete manipulate
 * existing rows is to use the iterators {@link RowIterator} or {@link RowArrayIterator}. The reason
 * is that it
 * is inefficient to reference via index since the implementation is based on {@link LinkedList}.
 * 
 * Note: The column size difference is maximal +-1, if the row is not complete. This implementation
 * is not synchronized.
 * 
 *
 */
public class Table implements Iterable<Row> {
	private Map<String, LinkedList<Object>> table;
	private String[] columnNames;
	private Set<String> columnNamesSet;
	private int size;

	private static Logger logger = Logger.getLogger(Table.class);

	/**
	 * Construct a new fixed column sized {@link org.vadere.util.data.Table}.
	 * 
	 * @param columnNames immutable columnNames
	 */
	public Table(final String... columnNames) {
		this.columnNamesSet = new HashSet<>();
		this.table = new HashMap<>();
		clear(columnNames);

	}

	public Table(final List<String> columnNames) {
		this(columnNames.toArray(new String[] {}));
	}

	public Table(final String[] columnNames, final Object[] values, final int size) {
		this(columnNames);
		if (columnNames.length != values.length) {
			throw new IllegalArgumentException("values and columnNames array has not the same length: "
					+ columnNames.length + " != " + values.length);
		}

		for (int row = 0; row < size; row++) {
			addRow();
			for (int col = 0; col < columnNames.length; col++) {
				addColumnEntry(columnNames[col], values[col]);
			}
		}
	}

	/**
	 * Returns the a copy of all column names of this table.
	 * 
	 * @return a copy of all column names of this table
	 */
	public String[] getColumnNames() {
		return columnNames.clone();
	}

	/**
	 * Clears the table, so after this call the table will be empty.
	 */
	public void clear() {
		this.size = 0;
		table = new HashMap<>();

		for (String fieldName : getColumnNames()) {
			table.put(fieldName, new LinkedList<>());
		}

	}

	public void clear(final String... columnNames) {
		this.size = 0;
		this.table.clear();
		this.columnNamesSet.clear();

		for (String columnName : columnNames) {
			if (!this.columnNamesSet.add(columnName)) {
				throw new IllegalArgumentException("duplicated column names " + columnName + ".");
			}
			this.table.put(columnName, new LinkedList<>());
		}

		this.columnNames = columnNames.clone();
	}

	/**
	 * Adds a value to the specific column of the current row identified by a column name.
	 * Note that one has to fill a whole row before add a new row. After a row is complete
	 * one has to call addRow(). If the column does not exist an {@link IllegalArgumentException}
	 * will be thrown. If one try to add an entry to a full column an
	 * {@link IndexOutOfBoundsException}
	 * will be thrown.
	 * 
	 * @param columnName name of the column
	 * @param value the value that will be inserted
	 * @throws IllegalArgumentException if the column does not exist
	 * @throws IndexOutOfBoundsException if the column is full before the call is happened
	 */
	public void addColumnEntry(final String columnName, final Object value) {
		LinkedList<Object> column = table.get(columnName);
		if (column == null) {
			column = new LinkedList<>();
			table.put(columnName, column);
			throw new IllegalArgumentException("column " + columnName + " does not exist.");
		}

		if (column.size() != size - 1) {
			throw new IndexOutOfBoundsException(column.size() + " != " + (size - 1));
		} else {
			// this is cheap!
			column.add(value);
		}
	}

	public void addColumnEntries(final Row row) {
		for (String columnName : row.getColumnNames()) {
			addColumnEntry(columnName, row.getEntry(columnName));
		}
	}

	public boolean containsColumn(final String columnName) {
		return columnNamesSet.contains(columnName);
	}


	/**
	 * Adds a new empty row to this table. After one complete a row
	 * this method has to be called to continue insert elements.
	 */
	public void addRow() {
		size++;
	}

	public void addRow(final Row row) {
		addRow();
		for (String colNames : row.getColumnNames()) {
			addColumnEntry(colNames, row.getEntry(colNames));
		}
	}

	/**
	 * Merges a table into this table by add each column to this table. Both
	 * tables has to be of the same size. If there a duplicated column names
	 * this table will stay with its column. Futhermore the columns of the other table
	 * will be added behind the this table.
	 * 
	 * @param table the table that will be merged into this table
	 */
	public void merge(final Table table) {
		if (size() != table.size) {
			throw new IndexOutOfBoundsException(table.size() + " != " + (this.size()));
		}
		// merge content
		for (String columnName : table.columnNames) {
			if (!this.columnNamesSet.contains(columnName) || this.table.get(columnName) == null
					|| this.table.get(columnName).isEmpty()) {
				this.table.put(columnName, table.getColumn(columnName));
				this.columnNamesSet.add(columnName);
			}
		}

		Set<String> columnNameSetCopy = new HashSet<>(columnNamesSet);

		// merge column names
		String[] columnNames = new String[columnNamesSet.size()];
		int columnCounter = 0;
		for (int i = 0; i < this.columnNames.length; i++) {
			if (columnNameSetCopy.contains(this.columnNames[i])) {
				columnNameSetCopy.remove(this.columnNames[i]);
				columnNames[columnCounter] = this.columnNames[i];
				columnCounter++;
			}
		}
		for (int i = 0; i < table.columnNames.length; i++) {
			if (columnNameSetCopy.contains(table.columnNames[i])) {
				columnNameSetCopy.remove(table.columnNames[i]);
				columnNames[columnCounter] = table.columnNames[i];
				columnCounter++;
			}
		}
		this.columnNames = columnNames;
	}

	/**
	 * Returns a single value of this table. This is a expensive call
	 * since the data structure use LinkedLists. You should use the iterator.
	 * 
	 * @param columnName the column of the entry
	 * @param row the row of the entry
	 * @return a single value of this table
	 */
	public Object getEntry(final String columnName, final int row) {
		if (getColumn(columnName).size() == 0) {
			return null;
		}
		return getColumn(columnName).get(row);
	}

	/**
	 * Returns a whole column if it exist, otherwise it will return null.
	 * 
	 * @param columnName the name of the column
	 * @return a whole column if it exist, null otherwise
	 */
	@SuppressWarnings("unchecked")
	public LinkedList<Object> getColumn(final String columnName) {
		return (LinkedList<Object>) table.get(columnName).clone();
	}

	/**
	 * Returns the number of filled rows in this table.
	 * 
	 * @return the number of filled rows in this table
	 */
	public int size() {
		return size;
	}

	/**
	 * Returns the column capacity of this table.
	 * 
	 * @return the column capacity of this table
	 */
	public int columns() {
		return columnNamesSet.size();
	}

	/**
	 * Returns true if this table triangleContains no elements.
	 * 
	 * @return true if this table triangleContains no elements
	 */
	public boolean isEmpty() {
		return columns() == 0 || size() == 0 || getEntry(getColumnNames()[0], 0) == null;
	}

	@Override
	public Iterator<Row> iterator() {
		return new RowIterator(getColumnNames());
	}

	public ListIterator<Row> listMapIterator() {
		return new RowIterator(getColumnNames());
	}

	public ListIterator<Object[]> listArrayIterator() {
		return new RowArrayIterator(getColumnNames());
	}

	public ListIterator<Object[]> listArrayIterator(final String... columnNames) {
		return new RowArrayIterator(columnNames);
	}

	/*
	 * Too expensive!
	 * 
	 * @Override
	 * public boolean equals(final Object obj) {
	 * if(obj == null || !getClass().equals(obj.getClass())) {
	 * return false;
	 * }
	 * 
	 * Table other = (Table)obj;
	 * 
	 * if(size() != other.size() || columnNames.length != other.columnNames.length ||
	 * !columnNamesSet.equals(other.columnNamesSet)) {
	 * return false;
	 * }
	 * 
	 * for(int i = 0; i < columnNames.length; i++) {
	 * if(!columnNames[i].equals(other.columnNames[i])) {
	 * return false;
	 * }
	 * }
	 * 
	 * for(Map.Entry<String, LinkedList<Object>> entry : table.entrySet()) {
	 * LinkedList<Object> otherList = other.table.get(entry.getKey());
	 * if(otherList == null) {
	 * return false;
	 * }
	 * else {
	 * 
	 * }
	 * }
	 * 
	 * 
	 * return super.equals(obj);
	 * }
	 */

	/**
	 * A {@link ListIterator} for manipulate the Table (remove, and update rows).
	 * 
	 *
	 */
	private class RowIterator implements ListIterator<Row> {

		private int row;
		private Map<String, ListIterator<Object>> iteratorList;
		private String[] columnNames;

		private RowIterator(final String... columnNames) {
			this.columnNames = columnNames;
			this.iteratorList = new HashMap<>();
			for (String columnName : columnNames) {
				iteratorList.put(columnName, table.get(columnName).listIterator());
			}
		}

		@Override
		public boolean hasNext() {
			for (ListIterator<Object> iterator : iteratorList.values()) {
				if (iterator.hasNext()) {
					return true;
				}
			}
			return false;
		}

		@Override
		public Row next() {
			Row resultMap = new Row();

			for (String columnName : columnNames) {
				if (iteratorList.get(columnName).hasNext()) {
					resultMap.setEntry(columnName, iteratorList.get(columnName).next());
				}
			}

			row++;
			return resultMap;
		}

		@Override
		public boolean hasPrevious() {
			for (ListIterator<Object> iterator : iteratorList.values()) {
				if (!iterator.hasPrevious()) {
					return false;
				}
			}
			return true;
		}

		@Override
		public Row previous() {
			Row resultMap = new Row();

			for (String columnName : columnNames) {
				if (iteratorList.get(columnName).hasPrevious()) {
					resultMap.setEntry(columnName, iteratorList.get(columnName).previous());
				}
			}

			row--;
			return resultMap;
		}

		@Override
		public int nextIndex() {
			return (row + 1);
		}

		@Override
		public int previousIndex() {
			return (row - 1);
		}

		@Override
		public void remove() {
			for (String columnName : columnNames) {
				iteratorList.get(columnName).remove();
			}
			size--;
		}

		@Override
		public void set(Row e) {
			for (String columnName : columnNames) {
				iteratorList.get(columnName).set(e);
			}
		}

		@Override
		public void add(Row e) {
			for (String columnName : columnNames) {
				iteratorList.get(columnName).add(e);
			}
			size++;
		}

	}

	/**
	 * A {@link ListIterator} for manipulate the Table (remove, and update rows).
	 * 
	 *
	 */
	private class RowArrayIterator implements ListIterator<Object[]> {

		private int row;
		private List<ListIterator<Object>> iteratorList;

		private RowArrayIterator(final String... columnNames) {
			this.iteratorList = new LinkedList<>();
			for (String columnName : columnNames) {
				try {
					iteratorList.add(table.get(columnName).listIterator());
				} catch (NullPointerException e) {
					// throw new NullPointerException(columnName + " is not in the table.");
					logger.error(columnName + " is not in the table. Creating NaN-column.");
					final int fakeIteratorLength = table.size();
					iteratorList.add(new ListIterator<Object>() {
						int index = 0;

						@Override
						public void set(Object e) {}

						@Override
						public void remove() {}

						@Override
						public int previousIndex() {
							return index - 1;
						}

						@Override
						public Object previous() {
							if (!hasPrevious())
								throw new NoSuchElementException();
							index--;
							return "<NaN>";
						}

						@Override
						public int nextIndex() {
							return index + 1;
						}

						@Override
						public Object next() {
							if (!hasNext())
								throw new NoSuchElementException();
							index++;
							return "<NaN>";
						}

						@Override
						public boolean hasPrevious() {
							return index >= 0;
						}

						@Override
						public boolean hasNext() {
							return index < fakeIteratorLength;
						}

						@Override
						public void add(Object e) {}
					});
				}

			}
		}

		@Override
		public boolean hasNext() {
			for (ListIterator<Object> iterator : iteratorList) {
				if (iterator.hasNext()) {
					return true;
				}
			}
			return false;
		}

		@Override
		public Object[] next() {
			Object[] result = new Object[iteratorList.size()];
			int col = 0;
			for (ListIterator<Object> iterator : iteratorList) {
				if (iterator.hasNext()) {
					result[col] = iterator.next();
				} else {
					result[col] = null;
				}
				col++;
			}
			row++;
			return result;
		}

		@Override
		public boolean hasPrevious() {
			for (ListIterator<Object> iterator : iteratorList) {
				if (iterator.hasPrevious()) {
					return true;
				}
			}
			return false;
		}

		@Override
		public Object[] previous() {
			Object[] result = new Object[iteratorList.size()];
			int col = 0;
			for (ListIterator<Object> iterator : iteratorList) {

				if (iterator.hasPrevious()) {
					result[col] = iterator.previous();
				} else {
					result[col] = null;
				}
				col++;

			}
			row--;
			return result;
		}

		@Override
		public int nextIndex() {
			return (row + 1);
		}

		@Override
		public int previousIndex() {
			return (row - 1);
		}

		@Override
		public void remove() {
			for (ListIterator<Object> iterator : iteratorList) {
				iterator.remove();;
			}
			size--;
		}

		@Override
		public void set(final Object[] e) {
			for (ListIterator<Object> iterator : iteratorList) {
				iterator.set(e);
			}
		}

		@Override
		public void add(final Object[] e) {
			for (ListIterator<Object> iterator : iteratorList) {
				iterator.add(e);
			}
			size++;
		}
	}
}
