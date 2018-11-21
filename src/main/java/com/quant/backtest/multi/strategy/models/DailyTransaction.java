package com.quant.backtest.multi.strategy.models;

import java.math.BigDecimal;

import com.quant.backtest.multi.strategy.enums.Side;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter @Setter @ToString
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
}
