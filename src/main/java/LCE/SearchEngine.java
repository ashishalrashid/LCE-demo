package LCE;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

class SearchEngine {
    //instance objects
    private Map<String, Map<Integer,Integer>> ReverseIndex;
    private Map<Integer, Map<String,Integer>> ForwardIndex;
    private long TotalTokens;
    private double AvgDocLength;

    private final Cache<String,List<Integer>> searchCache;

    //bm25 params
    private final double K1;
    private final double B;

    SearchEngine(SearchConfig config){
        ForwardIndex= new HashMap<>();
        ReverseIndex=new HashMap<>();
        this.searchCache =CacheFactory.create(config);
        this.K1=config.getBm25K1();
        this.B=config.getBm25B();
    }
    
    //Ingester function - make a delete function as well(to delete ingestions)
    @Deprecated
    void ingest(int doc_id,String text){ 

        String NormalText=TextProcessor.normalizer(text);
        List<String> tokens=TextProcessor.tokenize(NormalText);

        TotalTokens+=tokens.size();
        
        
        ForwardIndex.putIfAbsent(doc_id,new HashMap<>());
        Map<String,Integer> SubFMap=ForwardIndex.get(doc_id);

        for (String token: tokens){
            //foward mapping
            SubFMap.put(token,SubFMap.getOrDefault(token,0)+1);

            //reverse mapping
            ReverseIndex.putIfAbsent(token,new HashMap<>());
            Map<Integer,Integer> SubRMap=ReverseIndex.get(token);
            SubRMap.put(doc_id,SubRMap.getOrDefault(doc_id,0)+1);
        }
        //update internal variables -required for bm25
        TotalTokens +=tokens.size();
        AvgDocLength = TotalTokens / (double) ForwardIndex.size();



    }

    //single token ingestion
    void beginDoc(int docId , int estimatedTokens){
        ForwardIndex.putIfAbsent(docId,new HashMap<>(estimatedTokens*2));
    }

    void TokenIngest(int docId, String token){

        //forward Index
        Map<String, Integer> docMap =ForwardIndex.get(docId);
        docMap.put(token, docMap.getOrDefault(token,0)+1);

        //reverse Index
        Map<Integer,Integer> tokenDocs= ReverseIndex.computeIfAbsent(token,t->new HashMap<>());
        tokenDocs.put(docId, tokenDocs.getOrDefault(docId,0)+1);

        TotalTokens++;
        AvgDocLength=TotalTokens/(double) ForwardIndex.size();
    }

    //deletion fucntion:
    void deleteDoc(int docId) {
    Map<String, Integer> singleDoc = ForwardIndex.get(docId);
   
    if (singleDoc == null) return;

    for (Map.Entry<String, Integer> entry : singleDoc.entrySet()) {

        String token = entry.getKey();
        int freq = entry.getValue();

        Map<Integer, Integer> posting = ReverseIndex.get(token);
        if (posting == null) continue;

        posting.remove(docId);

        if (posting.isEmpty()) {
            ReverseIndex.remove(token);
        }

        TotalTokens -= freq;
    }

    ForwardIndex.remove(docId);

    AvgDocLength = ForwardIndex.isEmpty()? 0.0: TotalTokens / (double) ForwardIndex.size();
}

    //Query Functions
    //public function needed , seach
    //seach-> get >normalize > tokenize > order > score >rank > return 
    
    List<Integer> search(String query, int k){
        String NormalQuery=TextProcessor.normalizer(query);
        
        //lookup cache
        String cacheKey=NormalQuery +"|"+k;
        List<Integer> cached =searchCache.get(cacheKey);
        if (cached !=null){
            return cached;
        }

        //excute the search

        List<String> QueryTokens=TextProcessor.tokenize(NormalQuery);

        List<String> OrderedQueryTokens= OrderTokens(QueryTokens);

        Set<Integer> Candidates=GetCandidates(OrderedQueryTokens);

        List<Integer> Results= ScoreBM25(Candidates,OrderedQueryTokens,k);


        //cache the results
        searchCache.put(cacheKey,Results);
        
        return Results;
    }

    //private functions: ordering,get candidates , scoring , ranking
    
    //filtering and ordering , ordering happens after tokenization , so i think this is the correct way
   //Ordering
    private  List<String> OrderTokens(List<String> Tokens){
        List<String> Ordered =new ArrayList<>();
        
        for (String token : Tokens){
            if (ReverseIndex.containsKey(token)){
                Ordered.add(token);
            }
        Ordered.sort(Comparator.comparing(t->ReverseIndex.get(t).size()));

        
        }
        return Ordered;
    }

    //generate candidate list
    private Set<Integer> GetCandidates(List<String> OrderedQueryTokens){
        Set<Integer> candiates=null;

        for (String Token: OrderedQueryTokens){
            Map<Integer,Integer> tokenDocs= ReverseIndex.get(Token);
            
            if (tokenDocs==null){
                return Set.of();
            }
            if (candiates==null){
                candiates=tokenDocs.keySet();
            } else {
                candiates.retainAll(tokenDocs.keySet());
            }

            if (candiates.isEmpty()){
                break;
            }
            }
            return candiates ==null? Set.of() : candiates;
        }

    //helper function to get doc length
    private int getDocLength(int docId) {
    int length = 0;
    for (int freq : ForwardIndex.get(docId).values()) {
        length += freq;
    }
    return length;
    }

    //scoring fucntion to score and create heap 
    private List<Integer> ScoreBM25(Set<Integer> Candidates,  List<String> QueryTokens, int k){

        PriorityQueue<Map.Entry<Integer,Double>> heap = new PriorityQueue<>(Comparator.comparingDouble(Map.Entry::getValue));

        int N =ForwardIndex.size();

        for (int docId: Candidates){
            double score=0.0;
            int docLen=getDocLength(docId);

            for (String token: QueryTokens){

                Map<Integer,Integer>  tokenDocs=ReverseIndex.get(token);
                if (tokenDocs == null) continue;

                Integer tfObj = tokenDocs.get(docId);
                if (tfObj==null) continue;

                int tf=tfObj;
                int df=tokenDocs.size();

                double idf = Math.log((N - df + 0.5) / (df + 0.5) + 1);

                double denom = tf + K1 * (1 - B + B * (docLen / AvgDocLength));

                score += idf * (tf * (K1 + 1)) / denom;
            }
            if (heap.size()<k){
                heap.offer(Map.entry(docId,score));
            } else if (score >heap.peek().getValue()){
                heap.poll();
                heap.offer(Map.entry(docId,score));
            }
            }
            List<Integer> result =new ArrayList<>();
            while (!heap.isEmpty()){
                result.add(heap.poll().getKey());
            }
            Collections.reverse(result);
            return result;
        }

    //getter functions:
    Map<String,Integer> getSubForwardIndex(int docId){
        return ForwardIndex.get(docId);
    }
    Map<String, Map<Integer,Integer>> getReverseIndex(){
        return ReverseIndex;
    }
    
    Map<Integer, Map<String,Integer>> getForwardIndex(){
        return ForwardIndex;
    }
    
    long getTotalTokens(){
        return TotalTokens;
    }

    double getAvgDocLength(){
        return AvgDocLength;
    }

    //setter funtions
    void putForward(int docId, Map<String,Integer> map){
        ForwardIndex.put(docId,map);
    }
    void putReverse(String token, Map<Integer,Integer> map){
        ReverseIndex.put(token,map);
    }
    void setAvgDocLength(double num){
        this.AvgDocLength=num;
    }
    void setTotalTokens(long num){
        this.TotalTokens=num;
    }

}


