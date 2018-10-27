package com.quant.backtest.multi.strategy.cache;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

@Component(value = "listCache")
public class SimpleListBasedCache<T> {

    private List<T> cache = new ArrayList<>();
    
    public void cache(T t) {
	cache.add(t);
    }
    
    public boolean contains(T t) {
	return cache.contains(t);
    }
    
    public List<T> fetchCache() {
	return cache;
    }
}
