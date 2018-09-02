package com.quant.backtest.multi.strategy.processors;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.quant.backtest.multi.strategy.calculators.InputCalculator;
import com.quant.backtest.multi.strategy.properties.InputPropertiesLoader;
import com.quant.backtest.multi.strategy.utils.CsvUtils;
import com.quant.backtest.multi.strategy.utils.DateUtils;

@Component
public class MultiDayOptimalMultiStrategyProcessor {

    private static final Logger logger = LoggerFactory.getLogger(MultiDayOptimalMultiStrategyProcessor.class);

    private final Double DEFAULT_DOUBLE = 0.0d;
    private final Double TOTAL_OPTIMAL = 100.0d;

    @Autowired
    private InputCalculator inputCalculator;
    @Autowired
    private InputPropertiesLoader inputPropertiesLoader;
    @Autowired
    private DateUtils dateUtils;
    @Autowired
    private OptimalMultiStrategyProcessor optimalMultiStrategyProcessor;
    @Autowired
    private CsvUtils csvUtils;

    public Map<String, Double> process() throws FileNotFoundException {
	Map<String, Double> allStrategyWeights = inputCalculator.calculateWeights(inputPropertiesLoader.getSortino(), inputPropertiesLoader.getFlag());
	Double totalWeight = DEFAULT_DOUBLE;
	for (Double strategyWeight : allStrategyWeights.values()) {
	    totalWeight = totalWeight + strategyWeight;
	}
	logger.info("Total Strategy Weight = {} ", totalWeight);

	Map<String, Map<String, Double>> allTenDaysTicker = new HashMap<>();
	for (String date : dateUtils.getLastNDays(inputPropertiesLoader.getNumberOfDays())) {
	    allTenDaysTicker.put(date, optimalMultiStrategyProcessor.process(allStrategyWeights, totalWeight, date));
	}
	Map<String, Double> finalAveragedTickers = new HashMap<>();
	for (Map<String, Double> allTickers : allTenDaysTicker.values()) {
	    for (Entry<String, Double> allTicker : allTickers.entrySet()) {
		if (finalAveragedTickers.containsKey(allTicker.getKey())) {
		    finalAveragedTickers.put(allTicker.getKey(), finalAveragedTickers.get(allTicker.getKey()) + (allTicker.getValue()));
		} else
		    finalAveragedTickers.put(allTicker.getKey(), allTicker.getValue());
	    }
	}
	Map<String, Double> optimalPortfolio = new HashMap<>();
	double totalPercentOptimalPortfolio = DEFAULT_DOUBLE;
	for (Entry<String, Double> finalAveragedTickerEntry : finalAveragedTickers.entrySet()) {
	    double averagedVal = finalAveragedTickerEntry.getValue() / 10;
	    totalPercentOptimalPortfolio+=averagedVal;
	    optimalPortfolio.put(finalAveragedTickerEntry.getKey(), averagedVal);
	}
	if (totalPercentOptimalPortfolio < TOTAL_OPTIMAL) {
	    optimalPortfolio.put("CASH", TOTAL_OPTIMAL-totalPercentOptimalPortfolio);
	    finalAveragedTickers.put("CASH", TOTAL_OPTIMAL-totalPercentOptimalPortfolio);
	}
	logger.info("Final set of tickers are: {}", optimalPortfolio);
	try {
	    csvUtils.writeMapToCsv(inputPropertiesLoader.getOutputFilePath(), optimalPortfolio);
	} catch (IOException e) {
	    logger.error("Failed to write CSV with error {}", e.getMessage());
	}
	return finalAveragedTickers;
    }
}
