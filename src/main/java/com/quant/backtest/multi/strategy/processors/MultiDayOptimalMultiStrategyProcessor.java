package com.quant.backtest.multi.strategy.processors;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.quant.backtest.multi.strategy.calculators.InputCalculator;
import com.quant.backtest.multi.strategy.properties.InputPropertiesLoader;
import com.quant.backtest.multi.strategy.utils.CsvUtils;

@Component
public class MultiDayOptimalMultiStrategyProcessor {
    
    private static final Logger logger = LoggerFactory.getLogger(MultiDayOptimalMultiStrategyProcessor.class);
    
    private final Double DEFAULT_DOUBLE = 0.0d;
    
    @Autowired
    private InputCalculator inputCalculator;
    @Autowired
    private InputPropertiesLoader inputPropertiesLoader;
    @Autowired
    private OptimalMultiStrategyProcessor optimalMultiStrategyProcessor;
    @Autowired
    private CsvUtils csvUtils;
    
    public Map<String, Double> process() {
	Map<String, Double> allStrategyWeights = inputCalculator.calculateWeights(inputPropertiesLoader.getSortino(), inputPropertiesLoader.getFlag());
	Double totalWeight = DEFAULT_DOUBLE;
	for (Double strategyWeight : allStrategyWeights.values()) {
	    totalWeight = totalWeight+strategyWeight;
	}
	logger.info("Total Strategy Weight = {} ", totalWeight);
	
	Map<String, Map<String, Double>> allTenDaysTicker = new HashMap<>();
	for (String date : inputPropertiesLoader.getInputDate()) {
	    allTenDaysTicker.put(date, optimalMultiStrategyProcessor.process(allStrategyWeights, totalWeight, date));
	}
	Map<String, Double> finalAveragedTickers = new HashMap<>();
	Set<String> duplicateTickers = new HashSet<>();
	for (Map<String, Double> allTickers : allTenDaysTicker.values()) {
	    for (Entry<String, Double> allTicker : allTickers.entrySet()) {
		if (finalAveragedTickers.containsKey(allTicker.getKey())) {
		    duplicateTickers.add(allTicker.getKey());
		    finalAveragedTickers.put(allTicker.getKey(), finalAveragedTickers.get(allTicker.getKey())+(allTicker.getValue()));
		} else
		    finalAveragedTickers.put(allTicker.getKey(), allTicker.getValue());
	    }
	}
	for (String ticker : duplicateTickers) {
	    finalAveragedTickers.put(ticker, finalAveragedTickers.get(ticker)/inputPropertiesLoader.getinputDateSize());
	}
	logger.info("Final set of tickers are: {}", finalAveragedTickers);
	try {
	    csvUtils.writeMapToCsv(inputPropertiesLoader.getOutputFilePath(), finalAveragedTickers);
	} catch (IOException e) {
	    logger.error("Failed to write CSV with error {}", e.getMessage());
	}
	return finalAveragedTickers;
    }
}
