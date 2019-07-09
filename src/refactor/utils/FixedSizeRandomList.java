package refactor.utils;

import java.util.Random;

public class FixedSizeRandomList<E> extends FixedSizeList<E> {

    public FixedSizeRandomList(int capacity) {
        super(capacity);
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
