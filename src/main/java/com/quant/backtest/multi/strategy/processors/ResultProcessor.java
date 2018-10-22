package com.quant.backtest.multi.strategy.processors;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.PostConstruct;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.quant.backtest.multi.strategy.enums.Side;
import com.quant.backtest.multi.strategy.models.DailyTransaction;
import com.quant.backtest.multi.strategy.properties.InputPropertiesLoader;
import com.quant.backtest.multi.strategy.utils.DateUtils;
import com.quant.backtest.multi.strategy.utils.Defaults;
import com.quant.backtest.multi.strategy.utils.EmailUtils;
import com.quant.backtest.multi.strategy.utils.FileUtils;
import com.quant.backtest.multi.strategy.utils.PortfolioUtils;

/**
 * Processor that reads both Optimal and Actual portfolio's and identifies
 * tickers for Bloomberg execution
 * 
 * @author jiviteshshah
 */
@Component
public class ResultProcessor {

    private static final Logger logger = LoggerFactory.getLogger(ResultProcessor.class);

    private List<Double> xList;

    @Value("${X.delta}")
    private String automationXValue;
    @Value("${default.delta}")
    private String defaultDelta;
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

    @PostConstruct
    public void init() {
	xList = new ArrayList<>();
	if (StringUtils.isNotBlank(automationXValue)) {
	    for (String s : Lists.newArrayList(Splitter.on(",").split(automationXValue))) {
		xList.add(Double.valueOf(s));
	    }
	} else
	    xList.add(Double.valueOf(defaultDelta));
    }

    /**
     * Combines the Optimal and Actual portfolio's to create a daily
     * transaction.
     * 
     * @return List of transactions to be sent to Bloomberg
     * @throws IOException
     * @throws ParseException
     */
    public List<DailyTransaction> process() throws IOException, ParseException {
	Map<String, BigDecimal> currentOptimals = multiDayOptimalMultiStrategyProcessor.process();
	Map<String, BigDecimal> actualPortfolio = null;
	String filePath = inputPropertiesLoader.getOutputFilePath() + "actual-" + dateUtils.getPreviousWorkingDay() + ".csv";
	if (!fileUtils.doesFileExists(filePath)) {
	    logger.warn("Actual portfolio does not exist. STOPPING execution");
	    return null;
	}
	logger.info("Fetching Actual portfolio from Path {}", filePath);
	actualPortfolio = portfolioUtils.createActualPortfolio(filePath);

	BigDecimal multiplier = new BigDecimal(portfolioUtils.getTotalMarketValueOfPortfolio()).divide(new BigDecimal("100").setScale(Defaults.SCALE, RoundingMode.HALF_EVEN));

	List<DailyTransaction> dailyTransactions = null;
	for (Double x : xList) {
	    dailyTransactions = new ArrayList<>();
	    Double deltaDouble = (100 / currentOptimals.size()) / x;
	    BigDecimal deltaVal = new BigDecimal(deltaDouble).setScale(1, RoundingMode.HALF_EVEN);

	    StringBuilder builder = new StringBuilder("Daily Details with DELTA " + deltaVal.toString() + "\n\n");
	    for (Entry<String, BigDecimal> currentActual : currentOptimals.entrySet()) {
		if (!actualPortfolio.containsKey(currentActual.getKey())) {
		    BigDecimal finalValue = multiplier.multiply(currentActual.getValue()).setScale(Defaults.SCALE, RoundingMode.HALF_EVEN);
		    dailyTransactions.add(new DailyTransaction(Side.BUY, currentActual.getKey(), finalValue));
		    builder.append(Side.BUY.getName() + " " + currentActual.getKey() + " worth $" + finalValue + "\n");
		    logger.info("BUY {} worth ${}", currentActual.getKey(), finalValue);
		}
	    }

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
	}
	return dailyTransactions;
    }
}
