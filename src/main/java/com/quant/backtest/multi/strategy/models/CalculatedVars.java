package com.quant.backtest.multi.strategy.models;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter @Setter @ToString @NoArgsConstructor
@Component
public class CalculatedVars {
    
    @Value("sell.threshold")
    private String sellThreshold;

    private double totalMarketValue;
    
    public double getSellThreshold() {
        return totalMarketValue * Double.valueOf(sellThreshold);
    }   
}
