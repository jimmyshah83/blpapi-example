package com.quant.backtest.multi.strategy.processors;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.quant.backtest.multi.strategy.properties.InputPropertiesLoader;
import com.quant.backtest.multi.strategy.utils.CsvUtils;
import com.quant.backtest.multi.strategy.utils.DateUtils;
import com.quant.backtest.multi.strategy.utils.Defaults;
import com.quant.backtest.multi.strategy.utils.FileUtils;

@Component
public class OutputGenerator {

    private static final Logger logger = LoggerFactory.getLogger(OutputGenerator.class);
    private BigDecimal multiplier;

    @Autowired
    private MultiDayOptimalMultiStrategyProcessor multiDayOptimalMultiStrategyProcessor;
    @Autowired
    private InputPropertiesLoader inputPropertiesLoader;
    @Autowired
    private CsvUtils csvUtils;
    @Autowired
    private DateUtils dateUtils;
    @Autowired
    private FileUtils fileUtils;
    
    @PostConstruct
    public void init() {
	multiplier = inputPropertiesLoader.getCapital().divide(new BigDecimal("100").setScale(Defaults.SCALE, RoundingMode.HALF_EVEN));
    }

    public void process() throws FileNotFoundException {
	BigDecimal deltaVal = inputPropertiesLoader.getDelta();
	Map<String, BigDecimal> currentActuals = multiDayOptimalMultiStrategyProcessor.process();
	Map<String, BigDecimal> previousActuals = null;
	String filePath = inputPropertiesLoader.getOutputFilePath() + "actual-" + dateUtils.getPreviousWorkingDay() + ".csv";
	if (!fileUtils.doesFileExists(filePath)) {
	    throw new FileNotFoundException("Cannot find file : " + filePath);
	}
	logger.info("Fetching Previous File from Path {}", filePath);
	try {
	    previousActuals = csvUtils.readCsvToMap(filePath);
	} catch (IOException e) {
	    logger.error("Error reading previous file {}", e);
	    e.printStackTrace();
	}

	for (Entry<String, BigDecimal> currentActual : currentActuals.entrySet()) {
	    if (!previousActuals.containsKey(currentActual.getKey())) {
		BigDecimal finalValue = multiplier.multiply(currentActual.getValue());
		logger.info("BUY {} worth ${}", currentActual.getKey(), finalValue);
	    }
	}
	
	for (Entry<String, BigDecimal> previousActual : previousActuals.entrySet()) {
	    if (!currentActuals.containsKey(previousActual.getKey())) {
		BigDecimal finalValue = multiplier.multiply(previousActual.getValue());
		logger.info("SELL {} worth ${}", previousActual.getKey(), finalValue);
		continue;
	    }
	    if (currentActuals.containsKey(previousActual.getKey())) {
		BigDecimal previousVal = previousActual.getValue();
		BigDecimal currentVal = currentActuals.get(previousActual.getKey());
		BigDecimal differenceVal = currentVal.subtract(previousVal);
		if (differenceVal.signum() == -1) {
		    if (differenceVal.abs().compareTo(deltaVal) == 1) {
			BigDecimal finalValue = multiplier.multiply(differenceVal.abs());
			logger.info("SELL {} worth ${}", previousActual.getKey(), finalValue);
		    }
		} else if (differenceVal.compareTo(deltaVal) == 1) {
		    BigDecimal finalValue = multiplier.multiply(differenceVal);
		    logger.info("BUY {} worth ${}", previousActual.getKey(), finalValue);
		}
	    }
	}
    }
}
