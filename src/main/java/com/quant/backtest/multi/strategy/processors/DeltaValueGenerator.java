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
import com.quant.backtest.multi.strategy.properties.InputPropertiesLoader;
import com.quant.backtest.multi.strategy.utils.CsvUtils;
import com.quant.backtest.multi.strategy.utils.DateUtils;
import com.quant.backtest.multi.strategy.utils.FileUtils;

@Component
public class DeltaValueGenerator {
    
    private static final Logger logger = LoggerFactory.getLogger(DeltaValueGenerator.class);
    
    @Autowired
    private MultiDayOptimalMultiStrategyProcessor multiDayOptimalMultiStrategyProcessor;
    
    @Autowired
    private InputPropertiesLoader inputPropertiesLoader;    
    @Autowired
    private FilePropertiesLoader filePropertiesLoader;
    @Autowired
    private CsvUtils csvUtils;
    @Autowired
    private DateUtils dateUtils;
    @Autowired
    private FileUtils fileUtils;

    public void process() {
	Map<String, Double> currentActuals = multiDayOptimalMultiStrategyProcessor.process();
	Map<String, Double> previousActuals = null;
	String filePath = "";
	long decValue = 0;
	while(!fileUtils.doesFileExists(filePath)) {
		filePath = filePropertiesLoader.getOutputFilePath()+"actual-"+dateUtils.decrementCurrentDate(++decValue)+".csv";
	}
	logger.info("Fetching Previous File from Path {}", filePath);
	try {
	    previousActuals = csvUtils.readCsvToMap(filePath);
	} catch (IOException e) {
	    logger.error("Error reading previous file {}",e);
	    e.printStackTrace();
	}
	
	for (Entry<String, Double> currentActual : currentActuals.entrySet()) {
		if (!previousActuals.containsKey(currentActual.getKey())) {
			logger.info("BUY {} percent {}",  currentActual.getKey(), currentActual.getValue());
		}
	}	
	for (Entry<String, Double> previousActual : previousActuals.entrySet()) {
		if (!currentActuals.containsKey(previousActual.getKey())) {
			logger.info("SELL {} percent {}",  previousActual.getKey(), previousActual.getValue());
			continue;
		}
	    if (currentActuals.containsKey(previousActual.getKey())) {
			BigDecimal previousVal = new BigDecimal(previousActual.getValue()).setScale(2, RoundingMode.HALF_EVEN);
			BigDecimal currentVal = new BigDecimal(currentActuals.get(previousActual.getKey())).setScale(2, RoundingMode.HALF_EVEN);
			BigDecimal differenceVal = currentVal.subtract(previousVal);
			BigDecimal deltaVal = inputPropertiesLoader.getDelta();
			if (differenceVal.signum() == -1) {
			    if (differenceVal.abs().compareTo(deltaVal) == 1)
				 logger.info("SELL {} percent {}",  previousActual.getKey(), differenceVal.abs());
			} else if (differenceVal.compareTo(deltaVal) == 1)
			    logger.info("BUY {} percent {}",  previousActual.getKey(), differenceVal);
	    }
	}
    }
}
