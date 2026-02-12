package LCE;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LCEngine {

    private final SearchEngine searchEngine;
    private final AutocompleteEngine autocompleteEngine ;
    private final SearchConfig config;
    private static final int MAGIC = 0x4C4345; // "LCE"
    private static final int VERSION = 1;


    //constructor
    public LCEngine(SearchConfig config){
        this.config =config;
        this.searchEngine =new SearchEngine(config);
        this.autocompleteEngine  =new AutocompleteEngine(config);

    }
     
    //default config
    public LCEngine(){
        this(SearchConfig.defaultConfig());
    }
    //public APIs
    public void ingest(int docId, String text) {

        String normalized = TextProcessor.normalizer(text);
        List<String> tokens = TextProcessor.tokenize(normalized);

        //init doc Micro optimization
        searchEngine.beginDoc(docId, tokens.size());

        for (String token : tokens) {
            searchEngine.TokenIngest(docId, token);
            autocompleteEngine.insert(token);
        }
    }

    public List<Integer> search(String Query, int k){
        //k is the number of results to return , overload with 10 for default value later
        return searchEngine.search(Query,k);
    }

    public List<Integer> search(String Query){
        return search(Query,20);
    }

    public List<String> autocomplete(String prefix) {
        return autocompleteEngine.suggest(prefix);
    }

    public void DeleteDocument(int DocId){
        Map<String, Integer> DocTokens = searchEngine.getSubForwardIndex(DocId);

        if (DocTokens==null) return;

        searchEngine.deleteDoc(DocId);
        autocompleteEngine.decrementFreq(DocTokens);
        
    }

    public void rebuildAutocompleteTrie(){
        autocompleteEngine.rebuild();
    }

        public void save(Path path) throws IOException {
        // ensure parent dirs exist (UX-friendly)
        Path parent = path.getParent();
        if (parent != null) Files.createDirectories(parent);

        try (DataOutputStream out =
                 new DataOutputStream(new BufferedOutputStream(
                     Files.newOutputStream(path,
                         StandardOpenOption.CREATE,
                         StandardOpenOption.TRUNCATE_EXISTING)))) {

            // header
            out.writeInt(MAGIC);
            out.writeInt(VERSION);

            // metadata
            out.writeLong(searchEngine.getTotalTokens());
            out.writeDouble(searchEngine.getAvgDocLength());

            // forward index
            Map<Integer, Map<String, Integer>> fwd = searchEngine.getForwardIndex();
            out.writeInt(fwd.size());
            for (var docEntry : fwd.entrySet()) {
                out.writeInt(docEntry.getKey());
                Map<String, Integer> tokMap = docEntry.getValue();
                out.writeInt(tokMap.size());
                for (var e : tokMap.entrySet()) {
                    out.writeUTF(e.getKey());
                    out.writeInt(e.getValue());
                }
            }

            // reverse index
            Map<String, Map<Integer, Integer>> rev = searchEngine.getReverseIndex();
            out.writeInt(rev.size());
            for (var tokEntry : rev.entrySet()) {
                out.writeUTF(tokEntry.getKey());
                Map<Integer, Integer> postings = tokEntry.getValue();
                out.writeInt(postings.size());
                for (var p : postings.entrySet()) {
                    out.writeInt(p.getKey());
                    out.writeInt(p.getValue());
                }
            }
        }
    }

        public static LCEngine load(Path path) throws IOException {
        try (DataInputStream in =
                 new DataInputStream(new BufferedInputStream(
                     Files.newInputStream(path)))) {

            // header
            int magic = in.readInt();
            if (magic != MAGIC) throw new IOException("Invalid LCE file");

            int version = in.readInt();
            if (version != VERSION)
                throw new IOException("Unsupported LCE version: " + version);

            // construct engine with defaults
            LCEngine engine = new LCEngine();
            SearchEngine se = engine.searchEngine;

            // metadata
            se.setTotalTokens(in.readLong());
            se.setAvgDocLength(in.readDouble());

            // forward index
            int docCount = in.readInt();
            for (int i = 0; i < docCount; i++) {
                int docId = in.readInt();
                int tokenCount = in.readInt();
                Map<String, Integer> tokMap = new HashMap<>(tokenCount * 2);
                for (int j = 0; j < tokenCount; j++) {
                    tokMap.put(in.readUTF(), in.readInt());
                }
                se.putForward(docId, tokMap);
            }

            // reverse index
            int vocabSize = in.readInt();
            for (int i = 0; i < vocabSize; i++) {
                String token = in.readUTF();
                int postingCount = in.readInt();
                Map<Integer, Integer> postings = new HashMap<>(postingCount * 2);
                for (int j = 0; j < postingCount; j++) {
                    postings.put(in.readInt(), in.readInt());
                }
                se.putReverse(token, postings);
            }

            // rebuild derived state
            // engine.autocompleteEngine.rebuildFrom(se.getReverseIndex());

            return engine;
        }
    }
}