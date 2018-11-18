package com.quant.backtest.multi.strategy.processors;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.PostConstruct;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.quant.backtest.multi.strategy.cache.EmailCache;
import com.quant.backtest.multi.strategy.cache.SimpleListCache;
import com.quant.backtest.multi.strategy.enums.Side;
import com.quant.backtest.multi.strategy.models.CalculatedVars;
import com.quant.backtest.multi.strategy.models.DailyTransaction;
import com.quant.backtest.multi.strategy.utils.Defaults;
import com.quant.backtest.multi.strategy.utils.EmailUtils;
import com.quant.backtest.multi.strategy.utils.PortfolioUtils;

/**
 * Processor that reads both Optimal and Actual portfolio's and identifies
 * tickers for Bloomberg execution
 * 
 * @author jiviteshshah
 */
@Component
public class ResultProcessor {

    private List<Double> xList;

    @Value("${delta.divider}")
    private String automationXValue;
    @Value("${default.delta}")
    private String defaultDelta;
    @Value("${send.email}")
    private boolean sendEmail;
    
    @Autowired
    private MultiDayOptimalMultiStrategyProcessor multiDayOptimalMultiStrategyProcessor;
    @Autowired
    private PortfolioUtils portfolioUtils;
    @Autowired
    private SimpleListCache<DailyTransaction> listCache;
    @Autowired
    private EmailUtils emailUtils;
    @Autowired
    private CalculatedVars calculatedVars;
    @Autowired
    private EmailCache emailCache;

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
	Map<String, BigDecimal> optimalPortfolio = multiDayOptimalMultiStrategyProcessor.process();
	 Map<String, BigDecimal> actualPortfolio = portfolioUtils.createActualPortfolio();
	if (null == actualPortfolio)
	    return false;

	BigDecimal multiplier = new BigDecimal(calculatedVars.getTotalMarketValue()).divide(new BigDecimal("100.00"), Defaults.SCALE, Defaults.ROUNDING_MODE);
	for (Double x : xList) {
	    Double deltaDouble = (100 / optimalPortfolio.size()) / x;
	    BigDecimal deltaVal = new BigDecimal(deltaDouble).setScale(1, Defaults.ROUNDING_MODE);

	    emailCache.append("Number of Companies in Optimal = " + optimalPortfolio.size() + "\n");
	    emailCache.append("Trading Delta = " + deltaVal.toString() + "\n\n");
	    emailCache.append("Trades Today: \n");
	    for (Entry<String, BigDecimal> currentActual : optimalPortfolio.entrySet()) {
		if (!actualPortfolio.containsKey(currentActual.getKey())) {
		    BigDecimal finalValue = multiplier.multiply(currentActual.getValue()).setScale(Defaults.SCALE, Defaults.ROUNDING_MODE);
		    listCache.cache(new DailyTransaction(Side.BUY, currentActual.getKey(), finalValue));
		    emailCache.append(Side.BUY.getName() + " $" + finalValue + " " + currentActual.getKey() + "\n");
		}
	    }

	    for (Entry<String, BigDecimal> previousActual : actualPortfolio.entrySet()) {
		if (!optimalPortfolio.containsKey(previousActual.getKey())) {
		    BigDecimal finalValue = multiplier.multiply(previousActual.getValue()).setScale(Defaults.SCALE, Defaults.ROUNDING_MODE);
		    listCache.cache(new DailyTransaction(Side.SELL, previousActual.getKey(), finalValue));
		    emailCache.append(Side.SELL.getName() + " $" + finalValue + " " + previousActual.getKey() + "\n");
		    continue;
		}
		if (optimalPortfolio.containsKey(previousActual.getKey())) {
		    BigDecimal previousVal = previousActual.getValue();
		    BigDecimal currentVal = optimalPortfolio.get(previousActual.getKey());
		    BigDecimal differenceVal = currentVal.subtract(previousVal);
		    if (differenceVal.signum() == -1) {
			if (differenceVal.abs().compareTo(deltaVal) == 1) {
			    BigDecimal finalValue = multiplier.multiply(differenceVal.abs()).setScale(Defaults.SCALE, Defaults.ROUNDING_MODE);
			    listCache.cache(new DailyTransaction(Side.SELL, previousActual.getKey(), finalValue));
			    emailCache.append(Side.SELL.getName() + " $" + finalValue + " " + previousActual.getKey() + "\n");
			}
		    } else if (differenceVal.compareTo(deltaVal) == 1) {
			BigDecimal finalValue = multiplier.multiply(differenceVal).setScale(Defaults.SCALE, Defaults.ROUNDING_MODE);
			listCache.cache(new DailyTransaction(Side.BUY, previousActual.getKey(), finalValue));
			emailCache.append(Side.BUY.getName() + " $" + finalValue + " " + previousActual.getKey() + "\n");
		    }
		}
	    }
	    if (sendEmail)
		emailUtils.sendEmail();
	}
	return true;
    }
}
