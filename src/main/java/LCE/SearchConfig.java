package LCE;

import java.nio.file.Path;

public final class SearchConfig {

    private final CachePolicy cachePolicy;
    private final int cacheSize;
    private final int autocompleteTopK;
    private final double bm25K1;
    private final double bm25B;
    private final Path defaultIndexPath;

    private SearchConfig(Builder builder) {
        this.cachePolicy = builder.cachePolicy;
        this.cacheSize = builder.cacheSize;
        this.autocompleteTopK = builder.autocompleteTopK;
        this.bm25K1 = builder.bm25K1;
        this.bm25B = builder.bm25B;
        this.defaultIndexPath = builder.defaultIndexPath;
    }

    public static SearchConfig defaultConfig() {
        return new Builder().build();
    }

    // getters

    public Path getDefaultIndexPath() {
        return defaultIndexPath;
    }

    CachePolicy getCachePolicy() {
        return cachePolicy;
    }

    int getCacheSize() {
        return cacheSize;
    }

    int getAutocompleteTopK() {
        return autocompleteTopK;
    }

    double getBm25K1() {
        return bm25K1;
    }

    double getBm25B() {
        return bm25B;
    }

    // ---- Builder ----

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private CachePolicy cachePolicy = CachePolicy.LRU; 
        private int cacheSize = 1000;
        private int autocompleteTopK = 5;
        private double bm25K1 = 1.5;
        private double bm25B = 0.75;
        private Path defaultIndexPath = Path.of("data/lce_index.lce");

        private Builder() {}

        public Builder defaultIndexPath(Path path) {
            this.defaultIndexPath = path;
            return this;
        }

        public Builder cachePolicy(CachePolicy policy) {
            this.cachePolicy = policy;
            return this;
        }

        public Builder cacheSize(int size) {
            if (size <= 0) {
                throw new IllegalArgumentException("cacheSize must be > 0");
            }
            this.cacheSize = size;
            return this;
        }

        public Builder autocompleteTopK(int k) {
            if (k <= 0) {
                throw new IllegalArgumentException("autocompleteTopK must be > 0");
            }
            this.autocompleteTopK = k;
            return this;
        }

        public Builder bm25K1(double k1) {
            if (k1 <= 0) {
                throw new IllegalArgumentException("bm25K1 must be > 0");
            }
            this.bm25K1 = k1;
            return this;
        }

        public Builder bm25B(double b) {
            if (b < 0 || b > 1) {
                throw new IllegalArgumentException("bm25B must be between 0 and 1");
            }
            this.bm25B = b;
            return this;
        }

        public  SearchConfig build() {
            if (cachePolicy == CachePolicy.NONE) {
                this.cacheSize = 0; 
            }
            return new SearchConfig(this);
        }
    }
}
