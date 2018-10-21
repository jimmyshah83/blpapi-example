package com.quant.backtest.multi.strategy.enums;

public enum ActualPortfolioHeader {

    Company("Company"),
    Ticker("Ticker"),
    MarketValue("Market Value");
    
    private String value;
    
    private ActualPortfolioHeader(String value) {
	this.value = value;
    }
    
    public String getValue() {
	return this.value;
    }
}
