package com.quant.backtest.multi.strategy.processors;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.ParseException;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.quant.backtest.multi.strategy.cache.EmailCache;
import com.quant.backtest.multi.strategy.cache.SimpleListCache;
import com.quant.backtest.multi.strategy.enums.Side;
import com.quant.backtest.multi.strategy.enums.Tickers;
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

    @Value("${delta}")
    private String delta;
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
	BigDecimal deltaVal = new BigDecimal(delta).setScale(Defaults.SCALE, RoundingMode.UNNECESSARY);
	BigDecimal deltaAmount = deltaVal.multiply(new BigDecimal(calculatedVars.getTotalMarketValue())).divide(new BigDecimal("100"), Defaults.SCALE, Defaults.ROUNDING_MODE);

	emailCache.append("Number of Companies in Optimal = " + optimalPortfolio.size() + "\n");
	emailCache.append("Trading Delta = " + deltaVal.toString() + "\n\n");
	emailCache.append("Trades Today: \n");
	for (Entry<String, BigDecimal> optimals : optimalPortfolio.entrySet()) {
	    if (StringUtils.containsIgnoreCase(optimals.getKey(), Tickers.CASH.toString()))
		continue;
	    if (!actualPortfolio.containsKey(optimals.getKey())) {
		BigDecimal finalValue = multiplier.multiply(optimals.getValue()).setScale(Defaults.SCALE, Defaults.ROUNDING_MODE);
		if (finalValue.compareTo(deltaAmount) >= 0) {
		    listCache.cache(new DailyTransaction(Side.BUY, optimals.getKey(), finalValue));
		    emailCache.append(Side.BUY.getName() + " $" + finalValue + " " + optimals.getKey() + "\n");
		}
	    }
	}

	for (Entry<String, BigDecimal> actuals : actualPortfolio.entrySet()) {
	    if (StringUtils.containsIgnoreCase(actuals.getKey(), Tickers.CASH.toString()))
		continue;
	    if (!optimalPortfolio.containsKey(actuals.getKey())) {
		// This would mean we would like to liquidate the entire stock.
		BigDecimal finalValue = multiplier.multiply(actuals.getValue()).setScale(Defaults.SCALE, Defaults.ROUNDING_MODE);
		listCache.cache(new DailyTransaction(Side.SELL, actuals.getKey(), finalValue));
		emailCache.append(Side.SELL.getName() + " $" + finalValue + " " + actuals.getKey() + "\n");
		continue;
	    }
	    if (optimalPortfolio.containsKey(actuals.getKey())) {
		BigDecimal previousVal = actuals.getValue();
		BigDecimal currentVal = optimalPortfolio.get(actuals.getKey());
		BigDecimal differenceVal = currentVal.subtract(previousVal);
		if (differenceVal.signum() == -1) {
		    if (differenceVal.abs().compareTo(deltaVal) == 1) {
			BigDecimal finalValue = multiplier.multiply(differenceVal.abs()).setScale(Defaults.SCALE, Defaults.ROUNDING_MODE);
			listCache.cache(new DailyTransaction(Side.SELL, actuals.getKey(), finalValue));
			emailCache.append(Side.SELL.getName() + " $" + finalValue + " " + actuals.getKey() + "\n");
		    }
		} else if (differenceVal.compareTo(deltaVal) == 1) {
		    BigDecimal finalValue = multiplier.multiply(differenceVal).setScale(Defaults.SCALE, Defaults.ROUNDING_MODE);
		    listCache.cache(new DailyTransaction(Side.BUY, actuals.getKey(), finalValue));
		    emailCache.append(Side.BUY.getName() + " $" + finalValue + " " + actuals.getKey() + "\n");
		}
	    }
	}

	// Optimal portfolio Email
	emailCache.append("\n\nOptimal Portfolio: \n");
	for (Entry<String, BigDecimal> optimals : optimalPortfolio.entrySet()) {
	    emailCache.append(optimals.getKey() + " -> " + optimals.getValue().toString() + "%\n");
	}
	// Actual portfolio Email
	emailCache.append("\n\nActual Portfolio: \n");
	for (Entry<String, BigDecimal> actuals : actualPortfolio.entrySet()) {
	    emailCache.append(actuals.getKey() + " -> " + actuals.getValue().toString() + "%\n");
	}
	// Send the email
	if (sendEmail)
	    emailUtils.sendEmail();
	return true;
    }
}
