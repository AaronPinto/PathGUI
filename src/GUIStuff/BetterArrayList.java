package GUIStuff;

import java.util.ArrayList;

public class BetterArrayList<E> extends ArrayList<E> {
	BetterArrayList(int i) {
		super(i);
	}

	public BetterArrayList() {
		super();
	}

	public E getLast() {
		return this.get(this.size() - 1);
	}
}
