package com.quant.backtest.multi.strategy.properties;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.validation.annotation.Validated;

@Validated
@Configuration
@PropertySource("classpath:quant-input.properties")
@ConfigurationProperties
public class InputPropertiesLoader {

    private Map<String, BigDecimal> sortino = new HashMap<>();
    private Map<String, BigDecimal> flag = new HashMap<>();
    private Map<String, String> strategy = new HashMap<>();
    
    public void setSortino(Map<String, BigDecimal> sortino) {
        this.sortino = sortino;
    }
    public void setFlag(Map<String, BigDecimal> flag) {
        this.flag = flag;
    }
    public Map<String, BigDecimal> getSortino() {
        return sortino;
    }
    public Map<String, BigDecimal> getFlag() {
        return flag;
    }
    public Map<String, String> getStrategy() {
        return strategy;
    }
    public void setStrategy(Map<String, String> strategy) {
        this.strategy = strategy;
    }
}
