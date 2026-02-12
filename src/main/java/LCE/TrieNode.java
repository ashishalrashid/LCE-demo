package LCE;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

class TrieNode {

    Map<Character, TrieNode> children = new HashMap<>();

    boolean isWord = false;

    List<String> topK = new ArrayList<>();

    Set<String> topKSet = new HashSet<>();
}

