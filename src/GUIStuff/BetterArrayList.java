package GUIStuff;

import java.util.ArrayList;

/**
 * This class is simply a wrapper class for ArrayList that contains some useful functions to reduce the character length
 * of each line where applicable.
 *
 * @param <E>
 */
public class BetterArrayList<E> extends ArrayList<E> {
	/**
	 * Constructor for creating an ArrayList with the specified initial capacity
	 *
	 * @param i the initial capacity
	 */
	BetterArrayList(int i) {
		super(i);
	}

	/**
	 * Default constructor
	 */
	public BetterArrayList() {
		super();
	}

	/**
	 * Returns the last element in this ArrayList
	 *
	 * @return the last element in this ArrayList
	 */
	public E getLast() {
		return this.get(this.size() - 1);
	}

	/**
	 * Returns the 2nd last element in this ArrayList
	 *
	 * @return the 2nd last element in this ArrayList
	 */
	public E get2ndLast() {
		return this.get(this.size() - 2);
	}

	/**
	 * Removes and returns the last element in this ArrayList
	 *
	 * @return the removed last element in this ArrayList
	 */
	public E removeLast() {
		return this.remove(this.size() - 1);
	}
}