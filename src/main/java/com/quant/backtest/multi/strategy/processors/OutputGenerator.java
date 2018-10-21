package com.quant.backtest.multi.strategy.processors;

import java.io.FileNotFoundException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.quant.backtest.multi.strategy.enums.Side;
import com.quant.backtest.multi.strategy.models.DailyTransaction;
import com.quant.backtest.multi.strategy.properties.InputPropertiesLoader;
import com.quant.backtest.multi.strategy.utils.DateUtils;
import com.quant.backtest.multi.strategy.utils.Defaults;
import com.quant.backtest.multi.strategy.utils.EmailUtils;
import com.quant.backtest.multi.strategy.utils.FileUtils;
import com.quant.backtest.multi.strategy.utils.PortfolioUtils;

@Component
public class OutputGenerator {

    private static final Logger logger = LoggerFactory.getLogger(OutputGenerator.class);

    @Autowired
    private MultiDayOptimalMultiStrategyProcessor multiDayOptimalMultiStrategyProcessor;
    @Autowired
    private InputPropertiesLoader inputPropertiesLoader;
    @Autowired
    private PortfolioUtils portfolioUtils;
    @Autowired
    private DateUtils dateUtils;
    @Autowired
    private FileUtils fileUtils;
    @Autowired
    private EmailUtils emailUtils;

    public List<DailyTransaction> process() throws FileNotFoundException {
	Map<String, BigDecimal> currentOptimals = multiDayOptimalMultiStrategyProcessor.process();
	Map<String, BigDecimal> actualPortfolio = null;
	String filePath = inputPropertiesLoader.getOutputFilePath() + "actual-" + dateUtils.getPreviousWorkingDay() + ".csv";
	if (!fileUtils.doesFileExists(filePath)) {
	    throw new FileNotFoundException("Cannot find file : " + filePath);
	}
	logger.info("Fetching Previous File from Path {}", filePath);
	try {
	    actualPortfolio = portfolioUtils.createActualPortfolio(filePath);
	} catch (Exception e) {
	    logger.error("Error reading previous file {}", e);
	    e.printStackTrace();
	}
	
	BigDecimal multiplier = new BigDecimal(portfolioUtils.getTotalMarketValueOfPortfolio()).divide(new BigDecimal("100").setScale(Defaults.SCALE, RoundingMode.HALF_EVEN));
	
	List<DailyTransaction> dailyTransactions = new ArrayList<>();
	StringBuilder builder = new StringBuilder("Daily Details \n");
	for (Entry<String, BigDecimal> currentActual : currentOptimals.entrySet()) {
	    if (!actualPortfolio.containsKey(currentActual.getKey())) {
		BigDecimal finalValue = multiplier.multiply(currentActual.getValue()).setScale(Defaults.SCALE, RoundingMode.HALF_EVEN);
		dailyTransactions.add(new DailyTransaction(Side.BUY, currentActual.getKey(), finalValue));
		builder.append(Side.BUY.getName() + " " + currentActual.getKey() + " worth $" + finalValue + "\n");
		logger.info("BUY {} worth ${}", currentActual.getKey(), finalValue);
	    }
	}

	BigDecimal deltaVal = inputPropertiesLoader.getDelta();
	for (Entry<String, BigDecimal> previousActual : actualPortfolio.entrySet()) {
	    if (!currentOptimals.containsKey(previousActual.getKey())) {
		BigDecimal finalValue = multiplier.multiply(previousActual.getValue());
		dailyTransactions.add(new DailyTransaction(Side.SELL, previousActual.getKey(), finalValue));
		builder.append(Side.SELL.getName() + " " + previousActual.getKey() + " worth $" + finalValue + "\n");
		logger.info("SELL {} worth ${}", previousActual.getKey(), finalValue);
		continue;
	    }
	    if (currentOptimals.containsKey(previousActual.getKey())) {
		BigDecimal previousVal = previousActual.getValue();
		BigDecimal currentVal = currentOptimals.get(previousActual.getKey());
		BigDecimal differenceVal = currentVal.subtract(previousVal);
		if (differenceVal.signum() == -1) {
		    if (differenceVal.abs().compareTo(deltaVal) == 1) {
			BigDecimal finalValue = multiplier.multiply(differenceVal.abs());
			dailyTransactions.add(new DailyTransaction(Side.SELL, previousActual.getKey(), finalValue));
			builder.append(Side.SELL.getName() + " " + previousActual.getKey() + " worth $" + finalValue + "\n");
			logger.info("SELL {} worth ${}", previousActual.getKey(), finalValue);
		    }
		} else if (differenceVal.compareTo(deltaVal) == 1) {
		    BigDecimal finalValue = multiplier.multiply(differenceVal);
		    dailyTransactions.add(new DailyTransaction(Side.BUY, previousActual.getKey(), finalValue));
		    builder.append(Side.BUY.getName() + " " + previousActual.getKey() + " worth $" + finalValue + "\n");
		    logger.info("BUY {} worth ${}", previousActual.getKey(), finalValue);
		}
	    }
	}
	emailUtils.sendEmail(builder.toString());
	return dailyTransactions;
    }
}
