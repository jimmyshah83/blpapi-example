package com.quant.backtest.multi.strategy.enums;

public enum BloombergOrder {

    Ticker("EMSX_TICKER"),
    Amount("EMSX_AMOUNT"),
    OrderType("EMSX_ORDER_TYPE"),
    Tif("EMSX_TIF"),
    HandInstruction("EMSX_HAND_INSTRUCTION"),
    Side("EMSX_SIDE");
    
    private String value;
    
    private BloombergOrder(String value) {
	this.value = value;
    }
    
    public String getValue() {
	return this.value;
    }
}
