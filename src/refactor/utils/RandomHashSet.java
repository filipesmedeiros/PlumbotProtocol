package refactor.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class RandomHashSet<E> {

	private List<E> set;
	
	public RandomHashSet(int initialCapacity) {
		set = new ArrayList<>(initialCapacity);
	}
	
	public RandomHashSet() {
		this(10);
	}
	
	public void put(E element) {
		set.add(element);
	}
	
	public boolean remove(E element) {
		return set.remove(element);
	}
	
	public E getRandom() {
		Random r = new Random();
		return set.get(r.nextInt(set.size()));
	}
	
	public E removeRandom() {
		Random r = new Random();
		return set.remove(r.nextInt(set.size()));
	}
}
