package com.quant.backtest.multi.strategy.models;

import java.math.BigDecimal;

import com.quant.backtest.multi.strategy.enums.Side;

public class DailyTransaction {

    private Side side;
    private String ticker;
    private BigDecimal value;
    
    public DailyTransaction(Side side, String ticker, BigDecimal value) {
	super();
	this.side = side;
	this.ticker = ticker;
	this.value = value;
    }
    
    public Side getSide() {
        return side;
    }
    public String getTicker() {
        return ticker;
    }
    public BigDecimal getValue() {
        return value;
    }
    
    @Override
    public String toString() {
	return "DailyTransaction [side=" + side + ", ticker=" + ticker + ", value=" + value + "]";
    }
}
