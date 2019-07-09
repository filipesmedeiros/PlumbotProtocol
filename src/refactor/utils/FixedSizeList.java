package refactor.utils;

import java.util.ArrayList;

public class FixedSizeList<E> extends ArrayList<E> implements FixedSizeCollection<E> {

    private final int capacity;

    public FixedSizeList(int capacity) {
        this.capacity = capacity;
    }

    @Override
    public boolean isFull() {
        return size() == capacity;
    }
}
