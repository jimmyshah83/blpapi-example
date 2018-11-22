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

    public int calculateQuantity(DailyTransaction dailyTransaction) throws Exception {
	double sellThreshold = calculatedVars.getTotalMarketValue() * threshold;
	double amount = dailyTransaction.getValue().doubleValue();
	Double quantity = 0.0d;
	if (dailyTransaction.getSide() == Side.SELL && amount < sellThreshold) {
	    ActualPortfolioTicker actualPortfolioTicker = mapCache.retrieve(dailyTransaction.getTicker());
	    quantity = actualPortfolioTicker.getQuantity();
	} else {
	    double latestPrice = securityData.fetchLatestPrice(dailyTransaction.getTicker());
	    quantity = amount / latestPrice;
	}
	return quantity.intValue();
    }
}
