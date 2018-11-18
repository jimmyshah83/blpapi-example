package com.quant.backtest.multi.strategy.models;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter @Setter @NoArgsConstructor @ToString
public class ActualPortfolioTicker {
    
    private Map<String, Object> portfolioMap = new ConcurrentHashMap<>();
    
    private String Ticker;
    private Double Quantity;
    private BigDecimal Market_Value;

    public Map<String, Object> toMap() {
	portfolioMap.put("Quantity",Quantity);
	portfolioMap.put("Ticker",Ticker);
	portfolioMap.put("Market Value",Market_Value);
	return portfolioMap;
    }
}
