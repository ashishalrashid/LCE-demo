package LCE;

import java.util.LinkedHashMap;
import java.util.Map;

class LRU<K, V> extends LinkedHashMap<K, V> implements Cache<K, V> {

    private final int capacity;

    LRU(int capacity) {
        super(capacity, 0.75f, true);
        this.capacity = capacity;
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return size() > capacity;
    }
}
