package com.quant.backtest.multi.strategy.processors;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.quant.backtest.multi.strategy.calculators.InputCalculator;
import com.quant.backtest.multi.strategy.properties.FilePropertiesLoader;
import com.quant.backtest.multi.strategy.properties.InputPropertiesLoader;
import com.quant.backtest.multi.strategy.readers.CsvReader;
import com.quant.backtest.multi.strategy.utils.DateUtils;

@Component
public class OptimalMultiStrategyProcessor {

    private static final Logger logger = LoggerFactory.getLogger(OptimalMultiStrategyProcessor.class);

    @Autowired
    private InputCalculator inputCalculator;
    @Autowired
    private InputPropertiesLoader inputPropertiesLoader;
    @Autowired
    private FilePropertiesLoader filePropertiesLoader;
    @Autowired
    private CsvReader csvReader;
    @Autowired
    private DateUtils dateUtils;

    public void process() {
	Map<String, BigDecimal> strategyWeights = inputCalculator.calculateWeights(inputPropertiesLoader.getSortino(),
		inputPropertiesLoader.getFlag());
	Map<String, Set<String>> allStrategyTickers = new HashMap<>();
	for (String strategyName : inputPropertiesLoader.getStrategy().values()) {
	    allStrategyTickers.put(strategyName,
		    csvReader.readCsv(filePropertiesLoader.getFilePath() + strategyName + "/BARNBY GROWTH "
			    + strategyName + ".Buy.STG." + dateUtils.getTMinus1Date(filePropertiesLoader.getInputDate())
			    + ".csv"));
	}
	Map<String, BigDecimal> optimalWeightedStrategy = new HashMap<>();
	for (Entry<String, BigDecimal> strategyWeight : strategyWeights.entrySet()) {
	    for (String ticker : allStrategyTickers.get(strategyWeight.getKey())) {
		BigDecimal individualWeight = strategyWeight.getValue().divide(BigDecimal.valueOf(allStrategyTickers.get(strategyWeight.getKey()).size()), RoundingMode.HALF_EVEN);
		if (BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN).equals(individualWeight)) 
		    break;
		if (optimalWeightedStrategy.containsKey(ticker))
		    optimalWeightedStrategy.put(ticker, optimalWeightedStrategy.get(ticker).add(individualWeight));
		else
		    optimalWeightedStrategy.put(ticker, individualWeight);
	    }
	}
    }
}
