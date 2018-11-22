package com.quant.backtest.multi.strategy.models;

import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter @Setter @ToString @NoArgsConstructor
@Component
public class CalculatedVars {

    private double totalMarketValue;
}
