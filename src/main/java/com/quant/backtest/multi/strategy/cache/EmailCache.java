package com.quant.backtest.multi.strategy.cache;

import javax.annotation.PostConstruct;

import org.springframework.stereotype.Component;

@Component
public class EmailCache {

    private StringBuffer stringBuffer = new StringBuffer();
    
    @PostConstruct
    public void init() {
	stringBuffer.append("DKFC Quant Managed Account.\n\n");
    }
    
    public void append(String s) {
	stringBuffer.append(s);
    }
    
    public String getValue() {
	return stringBuffer.toString();
    }
}
