package com.quant.backtest.multi.strategy.models;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter @Setter @NoArgsConstructor @ToString
public class PreviousDayTrade {

    private Map<String, Object> dailyTrades = new ConcurrentHashMap<>();
    
    private String Side;
    private String BloombergID;
    private Double TgtQty;
    private Double AvgFillPx;
    private Double Commission;
    private Double bookValue;
    private Double marketValue;

    public Map<String, Object> toMap() {
	dailyTrades.put("Side", Side);
	dailyTrades.put("Ticker", BloombergID);
	dailyTrades.put("Quantity", TgtQty);
	dailyTrades.put("Average Fill", AvgFillPx);
	dailyTrades.put("Commission", Commission);
	dailyTrades.put("bookValue", bookValue);
	dailyTrades.put("marketValue", marketValue);
	return dailyTrades;
    }
}
