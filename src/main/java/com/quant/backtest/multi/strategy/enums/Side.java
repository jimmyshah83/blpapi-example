package com.quant.backtest.multi.strategy.enums;

public enum Side {
    
    BUY("BUY"),
    SELL("SELL");
    
    private String name;

    Side(String name) {
	this.name = name;
    }
    
    public String getName() {
	return name;
    }
}
