package com.quant.backtest.multi.strategy.processors;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.quant.backtest.multi.strategy.properties.FilePropertiesLoader;
import com.quant.backtest.multi.strategy.utils.CsvUtils;

@Component
public class DeltaValueGenerator {
    
    private static final Logger logger = LoggerFactory.getLogger(DeltaValueGenerator.class);
    
    @Autowired
    private MultiDayOptimalMultiStrategyProcessor multiDayOptimalMultiStrategyProcessor;
    
    @Autowired
    private CsvUtils csvUtils;
    
    @Autowired
    private FilePropertiesLoader filePropertiesLoader;

    public void process() {
	Map<String, Double> currentActuals = multiDayOptimalMultiStrategyProcessor.process();
	Map<String, Double> previousActuals = null;
	try {
	    previousActuals = csvUtils.readCsvToMap(filePropertiesLoader.getOutputFilePath()+"actual-20180721.csv");
	} catch (IOException e) {
	    logger.error("Error reading previous file {}",e);
	    e.printStackTrace();
	}
	for (Entry<String, Double> previousActual : previousActuals.entrySet()) {
	    if (currentActuals.containsKey(previousActual.getKey())) {
		BigDecimal previousVal = new BigDecimal(previousActual.getValue()).setScale(2, RoundingMode.HALF_EVEN);
		BigDecimal currentVal = new BigDecimal(currentActuals.get(previousActual.getKey())).setScale(2, RoundingMode.HALF_EVEN);
		BigDecimal differenceVal = currentVal.subtract(previousVal);
		BigDecimal deltaVal = filePropertiesLoader.getDelta();
		if (differenceVal.signum() == -1) {
		    if (differenceVal.abs().compareTo(deltaVal) == 1)
			 logger.info("SELL {} with difference {}",  previousActual.getKey(), differenceVal.toString());
		} else if (differenceVal.compareTo(deltaVal) == 1)
		    logger.info("BUY {} with difference {}",  previousActual.getKey(), differenceVal.toString());
	    }
	}
    }
}
