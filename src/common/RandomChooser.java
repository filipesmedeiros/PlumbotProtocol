package common;

import java.util.List;
import java.util.Random;
import java.util.Set;

public class RandomChooser<T> {

    private Random r;

    public RandomChooser() {
        r = new Random();
    }

    public T fromList(List<T> list) {
        return list.get(r.nextInt(list.size()));
    }

    @SuppressWarnings("unchecked")
    public T fromSet(Set<T> set) {
        return (T) set.toArray()[r.nextInt(set.size())];
    }

    public boolean removeFromSet(Set<T> set) {
        return set.remove(fromSet(set));
    }

    public int integer() {
        return r.nextInt();
    }

    public int integer(int bound) {
        return r.nextInt(bound);
    }

    public short shorteger() {
        return (short) r.nextInt(Integer.MAX_VALUE / 2);
    }

    public long nLong() {
        return r.nextLong();
    }
}
