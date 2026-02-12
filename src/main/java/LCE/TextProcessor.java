package LCE;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

class TextProcessor {

    private static final Set<String> STOP_WORDS =Set.of("i", "me", "my", "myself", "we", "our", "ours", "ourselves", "you", "your", "yours", "yourself", "yourselves", "he", "him", "his", "himself", "she", "her", "hers", "herself", "it", "its", "itself", "they", "them", "their", "theirs", "themselves", "what", "which", "who", "whom", "this", "that", "these", "those", "am", "is", "are", "was", "were", "be", "been", "being", "have", "has", "had", "having", "do", "does", "did", "doing", "a", "an", "the", "and", "but", "if", "or", "because", "as", "until", "while", "of", "at", "by", "for", "with", "about", "against", "between", "into", "through", "during", "before", "after", "above", "below", "to", "from", "up", "down", "in", "out", "on", "off", "over", "under", "again", "further", "then", "once", "here", "there", "when", "where", "why", "how", "all", "any", "both", "each", "few", "more", "most", "other", "some", "such", "no", "nor", "not", "only", "own", "same", "so", "than", "too", "very", "s", "t", "can", "will", "just", "don", "should", "now");
    //normalizer
    static  String normalizer(String text){
        text=text.toLowerCase();
        text = text.replaceAll("[^a-z0-9 ]", " ");
        text=text.replaceAll("\\s+"," ").trim();
        return text;
    } 


    //tokenizer
        static List<String> tokenize(String text) {

        String[] rawTokens = text.split(" ");
        List<String> tokens = new ArrayList<>(rawTokens.length);

        for (String token : rawTokens) {
            if (token.isEmpty()) continue;
            if (STOP_WORDS.contains(token)) continue;
            tokens.add(token);
        }

        return tokens;
    }
} 