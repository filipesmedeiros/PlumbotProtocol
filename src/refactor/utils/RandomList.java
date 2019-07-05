package refactor.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class RandomList<E> extends ArrayList<E> {

	public RandomList(int initialCapacity) {
		super(initialCapacity);
	}
	
	public RandomList() {
		this(10);
	}
	
	public E getRandom() {
		Random r = new Random();
		return get(r.nextInt(size()));
	}
	
	public E removeRandom() {
		Random r = new Random();
		return remove(r.nextInt(size()));
	}
}
