package LCE;

//this class is purly to pass any null check if the user has not chosen any cache
public class NoOpCache<K,V> implements Cache<K,V> {
    
    @Override
    public V get(K key){
        return null;
    }

    @Override
    public V put(K key,V value){
        return null; 
    }

    @Override
    public void clear(){
    }
}
