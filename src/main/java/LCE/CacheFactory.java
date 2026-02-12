package LCE;

class CacheFactory {

    static <K, V> Cache<K, V> create(SearchConfig config) {

        switch (config.getCachePolicy()) {
            case LRU:
                return new LRU<>(config.getCacheSize());

            // case ARC:
            //     return new ARC<>(config.getCacheSize());

            case NONE:
            default:
                return new NoOpCache<>();
        }
    }
}

