package GUIStuff;

import java.util.ArrayList;
import java.util.Collection;

/**
 * This class is simply a wrapper class for ArrayList that contains some useful functions to reduce the character length
 * of each line where applicable.
 *
 * @param <E>
 */
public class BetterArrayList<E> extends ArrayList<E> {
	BetterArrayList(int i) {
		super(i);
	}

	public BetterArrayList() {
		super();
	}

	BetterArrayList(Collection<? extends E> c) {
		super(c);
	}

	public E getLast() {
		return this.get(this.size() - 1);
	}

	public E get2ndLast() {
		return this.get(this.size() - 2);
	}

	public E removeLast() {
		return this.remove(this.size() - 1);
	}
}