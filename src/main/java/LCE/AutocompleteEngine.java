package LCE;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

class AutocompleteEngine {

    private final int K;

    private TrieNode root = new TrieNode();
    private final Map<String, Integer> wordFreq = new HashMap<>();
    
    //deletions and rebuild parameter
    private int TokensDeletedSinceRebuild=0;
    private static final int Rebuild_Threshold=50_000; 

    AutocompleteEngine(SearchConfig config){
        this.K =config.getAutocompleteTopK();

    }

    void insert(String word) {

        // update global frequency
        wordFreq.put(word, wordFreq.getOrDefault(word, 0) + 1);

        TrieNode node = root;

        for (char c : word.toCharArray()) {
            node = node.children.computeIfAbsent(c, k -> new TrieNode());
            updateTopK(node, word);
        }

        node.isWord = true;
    }

    List<String> suggest(String prefix) {

        TrieNode node = root;

        for (char c : prefix.toCharArray()) {
            node = node.children.get(c);
            if (node == null) return List.of();
        }

        return node.topK;
    }

    void decrementFreq(Map<String, Integer> docTokens) {

    if (docTokens == null) return;

    for (Map.Entry<String, Integer> entry : docTokens.entrySet()) {

        String token = entry.getKey();
        int freq = entry.getValue();

        Integer current = wordFreq.get(token);
        if (current == null) continue;

        if (current <= freq) {
            wordFreq.remove(token);
        } else {
            wordFreq.put(token, current - freq);
        }

        TokensDeletedSinceRebuild++;
    }
}

    void CheckAndRebuild(){
        if (TokensDeletedSinceRebuild>=Rebuild_Threshold){
            rebuild();
        }
    }

    void rebuild(){
        TokensDeletedSinceRebuild=0;
        Resetvariables();
        TrieNode newRoot =new  TrieNode(); 

        for (Map.Entry<String,Integer> entry : wordFreq.entrySet()){

            String token =entry.getKey();
            int freq =entry.getValue();

            if (freq<=0) continue;
            
            insertIntoTrie(newRoot,token,freq);
        }
        this.root=newRoot;
    }
    
    void rebuildFrom(Map<String, Map<Integer, Integer>> reverseIndex) {

        // ---- tuning knobs (can later move to config) ----
        final int MAX_AUTOCOMPLETE_TOKENS = 10_000;   // hard cap
        final int MAX_TOKEN_LENGTH = 20;              // ignore long junk tokens
        Resetvariables();
        // Build (token -> global frequency) list
        List<Map.Entry<String, Integer>> tokenFreqs = new ArrayList<>(reverseIndex.size());

        for (var e : reverseIndex.entrySet()) {
            String token = e.getKey();

            if (token.length() > MAX_TOKEN_LENGTH) {
                continue;
            }

            int freq = 0;
            for (int f : e.getValue().values()) {
                freq += f;
            }

            tokenFreqs.add(Map.entry(token, freq));
        }

        // Sort by descending frequency
        tokenFreqs.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));

        // Insert only top-N tokens
        int inserted = 0;
        for (var entry : tokenFreqs) {
            insertWithFreq(entry.getKey(), entry.getValue());
            inserted++;

            if (inserted >= MAX_AUTOCOMPLETE_TOKENS) {
                break;
            }
        }
    }


    /* ---------- helpers ---------- */
    
    //used at rebuildfrom
    void insertWithFreq(String word,int freq){
        wordFreq.put(word,freq);

        TrieNode node =root;

        for (char c:word.toCharArray()){
            node =node.children.computeIfAbsent(c,K->new TrieNode());
            updateTopK(node, word);
        }
        node.isWord=true;
    }

    //used at rebuildfrom
    private void Resetvariables(){
        this.root =new TrieNode();
        this.wordFreq.clear();
        this.TokensDeletedSinceRebuild=0;
    }

    //used at insert
    private void updateTopK(TrieNode node, String word) { 

        List<String> topK = node.topK;
        Set<String> topKSet=node.topKSet;

        if (topKSet.contains(word)) {
            sortTopK(topK);
            return;
        }

        if (topK.size() < K) {
            topK.add(word);
            topKSet.add(word);
            sortTopK(topK);
            return;
        }

        String weakest = topK.get(topK.size() - 1);

        if (wordFreq.get(word) > wordFreq.get(weakest)) {
            topK.remove(topK.size() - 1);
            topKSet.remove(weakest);
            topK.add(word);
            topKSet.add(word);
            sortTopK(topK);
        }
    }
    // overide the sort function be in line with freq map || used at helper : updatetopK
    private void sortTopK(List<String> list) {
        list.sort((a, b) -> wordFreq.get(b) - wordFreq.get(a));
    }

    private void insertIntoTrie(TrieNode root, String word, int freq) {
    
    wordFreq.put(word, freq);
    TrieNode node = root;

    for (char c : word.toCharArray()) {
        node = node.children.computeIfAbsent(c, k -> new TrieNode());
        updateTopK(node, word);
    }

    node.isWord = true;
}

}
