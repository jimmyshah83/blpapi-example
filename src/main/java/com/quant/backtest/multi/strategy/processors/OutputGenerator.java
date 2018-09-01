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
import com.quant.backtest.multi.strategy.utils.FileUtils;

@Component
public class OutputGenerator {

    private static final Logger logger = LoggerFactory.getLogger(OutputGenerator.class);
    private final int SCALE = 2;
    private final RoundingMode ROUNDING_MODE = RoundingMode.HALF_EVEN;
    private BigDecimal deltaMultiplier;

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
	deltaMultiplier = inputPropertiesLoader.getCapital().divide(new BigDecimal("100"));
    }

    public void process() throws FileNotFoundException {
	Map<String, Double> currentActuals = multiDayOptimalMultiStrategyProcessor.process();
	Map<String, Double> previousActuals = null;
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

	for (Entry<String, Double> currentActual : currentActuals.entrySet()) {
	    if (!previousActuals.containsKey(currentActual.getKey())) {
		BigDecimal finalValue = deltaMultiplier.multiply(new BigDecimal(currentActual.getValue()));
		logger.info("BUY {} worth ${}", currentActual.getKey(), finalValue.setScale(SCALE, ROUNDING_MODE));
	    }
	}
	
	for (Entry<String, Double> previousActual : previousActuals.entrySet()) {
	    if (!currentActuals.containsKey(previousActual.getKey())) {
		BigDecimal finalValue = deltaMultiplier.multiply(new BigDecimal(previousActual.getValue()));
		logger.info("SELL {} worth ${}", previousActual.getKey(), finalValue.setScale(SCALE, ROUNDING_MODE));
		continue;
	    }
	    if (currentActuals.containsKey(previousActual.getKey())) {
		BigDecimal previousVal = new BigDecimal(previousActual.getValue()).setScale(SCALE, ROUNDING_MODE);
		BigDecimal currentVal = new BigDecimal(currentActuals.get(previousActual.getKey())).setScale(SCALE, ROUNDING_MODE);
		BigDecimal differenceVal = currentVal.subtract(previousVal);
		BigDecimal deltaVal = inputPropertiesLoader.getDelta();
		if (differenceVal.signum() == -1) {
		    if (differenceVal.abs().compareTo(deltaVal) == 1) {
			BigDecimal finalValue = deltaMultiplier.multiply(differenceVal.abs()).setScale(SCALE, ROUNDING_MODE);
			logger.info("SELL {} worth ${}", previousActual.getKey(), finalValue);
		    }
		} else if (differenceVal.compareTo(deltaVal) == 1) {
		    BigDecimal finalValue = deltaMultiplier.multiply(differenceVal).setScale(SCALE, ROUNDING_MODE);
		    logger.info("BUY {} worth ${}", previousActual.getKey(), finalValue);
		}
	    }
	}
    }
}
