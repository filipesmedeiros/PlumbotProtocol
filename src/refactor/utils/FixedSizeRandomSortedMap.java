package refactor.utils;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class FixedSizeRandomSortedMap<K extends Comparable<K>, V> implements FixedSizeCollection<V> {

    private final int capacity;

    private List<Entry<K, V>> list;
    private SortedMap<K, V> sortedMap;

    public FixedSizeRandomSortedMap(int capacity) {
        this.capacity = capacity;
        list = new ArrayList<>(capacity);
        sortedMap = new TreeMap<>(Collections.reverseOrder());
    }

    public FixedSizeRandomSortedMap() {
        this(10);
    }

    public V getRandom() {
        Random r = new Random();
        return list.get(r.nextInt(list.size())).getValue();
    }

    public V removeRandom() {
        Random r = new Random();
        Map.Entry<K, V> chosen = list.remove(r.nextInt(list.size()));
        if(chosen == null)
            return null;
        sortedMap.remove(chosen.getKey());
        return chosen.getValue();
    }

    public V add(K key, V value) {
        if(list.size() == capacity)
            return null;
        sortedMap.put(key, value);
        list.add(new Entry<>(key, value));
        return value;
    }

    public V remove(V element) {
        // Using a for loop because I know it's more efficient in an ArrayList specifically
        Entry<K, V> entry = null;
        for(int i = 0; i < list.size(); i++) {
            Entry<K, V> current = list.get(i);
            if(current.getValue().equals(element)) {
                list.remove(i);
                entry = current;
                break;
            }
        }
        if(entry == null)
            return null;
        sortedMap.remove(entry.getKey());
        return element;
    }

    public Iterator<Map.Entry<K, V>> sortedIterator() {
        return sortedMap.entrySet().iterator();
    }

    public Iterator<Map.Entry<K, V>> sortedIteratorFrom(int index) {
        Iterator<Map.Entry<K, V>> iterator = sortedMap.entrySet().iterator();
        for(int i = 0; i < index; i++)
            iterator.next();
        return iterator;
    }

    public int size() {
        return list.size();
    }

    public V getElement(int index) {
        return list.get(index).getValue();
    }

    public Entry<K, V> get(int index) {
        return list.get(index);
    }

    public V getLast() {
        return getElement(size() - 1);
    }

    public V getFirst() {
        return getElement(0);
    }

    public boolean isEmpty() {
        return list.size() == 0;
    }

    public boolean hasNPlusElements(int n) {
        return size() >= n;
    }

    public boolean isFull() {
        return list.size() == capacity;
    }

    public boolean contains(V value) {
        return sortedMap.containsValue(value);
    }

    public void forEach(Consumer<V> task) {
        Iterator<Map.Entry<K, V>> iterator = sortedIterator();
        while(iterator.hasNext()) {
            V value = iterator.next().getValue();
            task.accept(value);
        }
    }

    public void forEach(BiConsumer<K, V> task) {
        Iterator<Map.Entry<K, V>> iterator = sortedIterator();
        while(iterator.hasNext()) {
            Map.Entry<K, V> entry = iterator.next();
            K key = entry.getKey();
            V value = entry.getValue();
            task.accept(key, value);
        }
    }

    public static class Entry<K, V> implements Map.Entry<K, V> {

        private K key;
        private V value;

        private Entry(K key, V value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public K getKey() {
            return key;
        }

        @Override
        public V getValue() {
            return value;
        }

        @Override
        public V setValue(V value) {
            this.value = value;
            return value;
        }
    }
}
