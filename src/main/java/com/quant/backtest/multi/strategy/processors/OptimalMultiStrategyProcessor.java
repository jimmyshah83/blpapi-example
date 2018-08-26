package com.quant.backtest.multi.strategy.processors;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.quant.backtest.multi.strategy.properties.InputPropertiesLoader;
import com.quant.backtest.multi.strategy.utils.CsvUtils;

@Component
public class OptimalMultiStrategyProcessor {

    private static final Logger logger = LoggerFactory.getLogger(OptimalMultiStrategyProcessor.class);
    private final Double DEFAULT_DOUBLE = 0.0d;

    @Autowired
    private InputPropertiesLoader inputPropertiesLoader;
    @Autowired
    private CsvUtils csvUtils;

    public Map<String, Double> process(Map<String, Double> allStrategyWeights, Double totalWeight, String date) {
	Map<String, Set<String>> allStrategyTickers = new HashMap<>();
	for (String strategyName : inputPropertiesLoader.getStrategy().values()) {
	    allStrategyTickers.put(strategyName, csvUtils.readBacktestedCsv(inputPropertiesLoader.getFilePath() + strategyName + "/" + strategyName + "-" + date + ".csv"));
	}

	Map<String, Double> optimalWeightedStrategy = new HashMap<>();
	for (Entry<String, Double> strategyWeight : allStrategyWeights.entrySet()) {
	    if (DEFAULT_DOUBLE.equals(strategyWeight.getValue()))
		continue;
	    /**
	     * TODO: If count is < 3 then get the difference from 3 and add CASH as a ticker? 
	     */
	    Double individualTickerWeightPercent = ((strategyWeight.getValue() / totalWeight) * 100.00d) / (allStrategyTickers.get(strategyWeight.getKey()).size());
	    logger.info("Individual % weight {} for each ticker in evenly balanced startegy {} with ticers {}", individualTickerWeightPercent, strategyWeight.getKey(),
		    allStrategyTickers.get(strategyWeight.getKey()).size());
	    for (String ticker : allStrategyTickers.get(strategyWeight.getKey())) {
		if (optimalWeightedStrategy.containsKey(ticker))
		    optimalWeightedStrategy.put(ticker, optimalWeightedStrategy.get(ticker) + individualTickerWeightPercent);
		else
		    optimalWeightedStrategy.put(ticker, individualTickerWeightPercent);
	    }
	}
	logger.info("Optimal {} day strategy = {}", date, optimalWeightedStrategy);
	return optimalWeightedStrategy;
    }
}
