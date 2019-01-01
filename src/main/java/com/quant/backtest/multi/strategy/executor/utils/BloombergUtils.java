package com.quant.backtest.multi.strategy.executor.utils;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.quant.backtest.multi.strategy.cache.SimpleMapCache;
import com.quant.backtest.multi.strategy.enums.Side;
import com.quant.backtest.multi.strategy.executors.BloombergFetchSecurityData;
import com.quant.backtest.multi.strategy.models.ActualPortfolioTicker;
import com.quant.backtest.multi.strategy.models.CalculatedVars;
import com.quant.backtest.multi.strategy.models.DailyTransaction;

@Component(value = "bbgUtils")
public class BloombergUtils {
    
    @Value("${delta}")
    private String delta;
    
    @Autowired
    private BloombergFetchSecurityData securityData;
    @Autowired
    private CalculatedVars calculatedVars;
    @Autowired
    private SimpleMapCache<String, ActualPortfolioTicker> mapCache;
    private double threshold = 0.0d;
    
    @PostConstruct
    public void init() {
	threshold = Double.valueOf(delta) / 100.0d;
    }

    /**
     * Calculates the quantity based on latest price fetched from Bloomberg.
     * This has a small calculations on sell side where we liquidate everything below a threshold 
     * and if the remaining stocks are less than 5.
     * @param dailyTransaction the entire Bloomberg transaction
     * @return the quantity to be bought / sold
     * @throws Exception
     */
    public int calculateQuantity(DailyTransaction dailyTransaction) throws Exception {
	double amount = dailyTransaction.getValue().doubleValue();
	if (dailyTransaction.getSide() == Side.SELL && amount < (calculatedVars.getTotalMarketValue() * threshold)) {
	    ActualPortfolioTicker actualPortfolioTicker = mapCache.retrieve(dailyTransaction.getTicker());
	    return actualPortfolioTicker.getQuantity().intValue();
	} else {
	    double latestPrice = securityData.fetchLatestPrice(dailyTransaction.getTicker());
	    Double quantity = amount / latestPrice;
	    // Check if the quantity to be sold is within 5 stocks, then sell the entire actual portfolio
	    if (dailyTransaction.getSide() == Side.SELL) {
		ActualPortfolioTicker actualPortfolioTicker = mapCache.retrieve(dailyTransaction.getTicker());
		if (Math.abs(actualPortfolioTicker.getQuantity().intValue()-quantity.intValue()) < 5)
		    return actualPortfolioTicker.getQuantity().intValue();
	    }
	    return quantity.intValue();
	}
    }
}
