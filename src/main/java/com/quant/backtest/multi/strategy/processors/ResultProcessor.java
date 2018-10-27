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
import com.quant.backtest.multi.strategy.cache.SimpleListBasedCache;
import com.quant.backtest.multi.strategy.enums.Side;
import com.quant.backtest.multi.strategy.models.DailyTransaction;
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
    @Value("${actual.portfolio.FilePath}")
    private String actualPortfolioFilePath;
    
    @Autowired
    private MultiDayOptimalMultiStrategyProcessor multiDayOptimalMultiStrategyProcessor;
    @Autowired
    private PortfolioUtils portfolioUtils;
    @Autowired
    private DateUtils dateUtils;
    @Autowired
    private FileUtils fileUtils;
    @Autowired
    private SimpleListBasedCache<DailyTransaction> listCache;
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
    public Boolean process() throws IOException, ParseException {
	Map<String, BigDecimal> ptimalPortfolio = multiDayOptimalMultiStrategyProcessor.process();
	Map<String, BigDecimal> actualPortfolio = null;
	String filePath = actualPortfolioFilePath + "actual-" + dateUtils.getPreviousWorkingDay() + ".csv";
	if (!fileUtils.doesFileExists(filePath)) {
	    logger.error("Actual portfolio does not exist. STOPPING execution");
	    return false;
	}
	logger.info("Fetching Actual portfolio from Path {}", filePath);
	actualPortfolio = portfolioUtils.createActualPortfolio(filePath);

	BigDecimal multiplier = new BigDecimal(portfolioUtils.getTotalMarketValueOfPortfolio()).divide(new BigDecimal("100").setScale(Defaults.SCALE, RoundingMode.HALF_EVEN));

	for (Double x : xList) {
	    Double deltaDouble = (100 / ptimalPortfolio.size()) / x;
	    BigDecimal deltaVal = new BigDecimal(deltaDouble).setScale(1, RoundingMode.HALF_EVEN);

	    StringBuilder builder = new StringBuilder("Daily Details with DELTA " + deltaVal.toString() + "\n\n");
	    for (Entry<String, BigDecimal> currentActual : ptimalPortfolio.entrySet()) {
		if (!actualPortfolio.containsKey(currentActual.getKey())) {
		    BigDecimal finalValue = multiplier.multiply(currentActual.getValue()).setScale(Defaults.SCALE, RoundingMode.HALF_EVEN);
		    listCache.cache(new DailyTransaction(Side.BUY, currentActual.getKey(), finalValue));
		    builder.append(Side.BUY.getName() + " " + currentActual.getKey() + " worth $" + finalValue + "\n");
		}
	    }

	    for (Entry<String, BigDecimal> previousActual : actualPortfolio.entrySet()) {
		if (!ptimalPortfolio.containsKey(previousActual.getKey())) {
		    BigDecimal finalValue = multiplier.multiply(previousActual.getValue());
		    listCache.cache(new DailyTransaction(Side.SELL, previousActual.getKey(), finalValue));
		    builder.append(Side.SELL.getName() + " " + previousActual.getKey() + " worth $" + finalValue + "\n");
		    continue;
		}
		if (ptimalPortfolio.containsKey(previousActual.getKey())) {
		    BigDecimal previousVal = previousActual.getValue();
		    BigDecimal currentVal = ptimalPortfolio.get(previousActual.getKey());
		    BigDecimal differenceVal = currentVal.subtract(previousVal);
		    if (differenceVal.signum() == -1) {
			if (differenceVal.abs().compareTo(deltaVal) == 1) {
			    BigDecimal finalValue = multiplier.multiply(differenceVal.abs());
			    listCache.cache(new DailyTransaction(Side.SELL, previousActual.getKey(), finalValue));
			    builder.append(Side.SELL.getName() + " " + previousActual.getKey() + " worth $" + finalValue + "\n");
			}
		    } else if (differenceVal.compareTo(deltaVal) == 1) {
			BigDecimal finalValue = multiplier.multiply(differenceVal);
			listCache.cache(new DailyTransaction(Side.BUY, previousActual.getKey(), finalValue));
			builder.append(Side.BUY.getName() + " " + previousActual.getKey() + " worth $" + finalValue + "\n");
		    }
		}
	    }
	    emailUtils.sendEmail(builder.toString());
	}
	return true;
    }
}
