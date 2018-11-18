package com.quant.backtest.multi.strategy.cache;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

@Component(value = "mapCache")
public class SimpleMapCache<K, V> {

    private Map<K, V> cache = new ConcurrentHashMap<K, V>();
    
    public void cache(K k, V v) {
	cache.put(k, v);
    }
    
    public V retrieve(K k) {
	return cache.get(k);
    }
    
    public boolean containsKey(K k) {
	return cache.containsKey(k);
    }
    
    public Set<Entry<K, V>> entrySet() {
	return cache.entrySet();
    }
    
    public void remove(K k) {
	cache.remove(k);
    }
}
