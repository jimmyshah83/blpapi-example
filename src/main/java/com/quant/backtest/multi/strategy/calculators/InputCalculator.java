package com.quant.backtest.multi.strategy.calculators;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

@Component
public class InputCalculator {

    public Map<String, Double> calculateWeights(Map<String, Double> sortinos, Map<String, Integer> flags) {
	Map<String, Double> weights = new HashMap<String, Double>();
	for (String strategy : sortinos.keySet()) {
	    weights.put(strategy, flags.get(strategy) * sortinos.get(strategy));
	}
	return weights;
    }
}
