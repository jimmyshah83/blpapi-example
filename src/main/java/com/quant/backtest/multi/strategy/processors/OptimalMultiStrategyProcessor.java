package com.quant.backtest.multi.strategy.processors;

import java.math.BigDecimal;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.quant.backtest.multi.strategy.calculators.InputCalculator;
import com.quant.backtest.multi.strategy.properties.InputPropertiesLoader;

@Component
public class OptimalMultiStrategyProcessor {
    
    private static final Logger logger = LoggerFactory.getLogger(OptimalMultiStrategyProcessor.class);
    
    @Autowired
    private InputCalculator inputCalculator;
    @Autowired
    private InputPropertiesLoader inputPropertiesLoader;

    public void process() {
	Map<String, BigDecimal> weights = inputCalculator.calculateWeights(inputPropertiesLoader.getSortino(), inputPropertiesLoader.getFlag());
	// Read the CSV files in separate sets
	// Calculate percentages for each and store in a map 
	logger.info("-------- {}", weights.values());
    }
}
