package util;

import java.util.ArrayList;

/**
 * This class is simply a wrapper class for ArrayList that contains some useful functions to reduce the character length of each line where
 * applicable.
 *
 * @param <E>
 */
public final class BetterArrayList<E> extends ArrayList<E> {
    /**
     * Constructor for creating an ArrayList with the specified initial capacity
     *
     * @param i the initial capacity
     */
    public BetterArrayList(int i) {
        super(i);
    }

    /**
     * Default constructor
     */
    public BetterArrayList() {
    }

    public BetterArrayList(BetterArrayList<E> other) {
        super(other);
    }

    @Override
    public String toString() {
        return super.toString();
    }

    /**
     * Removes and returns the last element in this ArrayList
     *
     * @return the removed last element in this ArrayList
     */
    public E removeLast() {
        return this.remove(this.size() - 1);
    }

    /**
     * Returns the last element in this ArrayList
     *
     * @return the last element in this ArrayList
     */
    public E getLast() {
        return super.get(this.size() - 1);
    }

    /**
     * Sets the last element in this ArrayList to the specified value
     *
     * @param element the value to replace the existing one with
     */
    public void setLast(E element) {
        super.set(this.size() - 1, element);
    }

    /**
     * Returns the 2nd last element in this ArrayList
     *
     * @return the 2nd last element in this ArrayList
     */
    public E get2ndLast() {
        return super.get(this.size() - 2);
    }
}
