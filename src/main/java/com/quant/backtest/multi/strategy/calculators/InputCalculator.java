package com.quant.backtest.multi.strategy.calculators;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

@Component
public class InputCalculator {

    public Map<String, BigDecimal> calculateWeights(Map<String, BigDecimal> sortinos, Map<String, BigDecimal> flags) {
	Map<String, BigDecimal> weights = new HashMap<String, BigDecimal>();
	for (String strategy : sortinos.keySet()) {
	    weights.put(strategy, flags.get(strategy).multiply(sortinos.get(strategy)));
	}
	return weights;
    }
}
