package com.quant.backtest.multi.strategy.properties;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.NonNull;
import org.springframework.validation.annotation.Validated;

@Validated
@Configuration
@PropertySource("classpath:quant-input.properties")
@ConfigurationProperties(prefix="input")
public class InputPropertiesLoader {

    private Map<String, BigDecimal> sortino = new HashMap<>();
    private Map<String, BigDecimal> flag = new HashMap<>();
    @NonNull
    @DateTimeFormat(pattern="yyyyMMdd")
    private Date date;
    
    public Map<String, BigDecimal> getSortino() {
        return sortino;
    }
    
    public Map<String, BigDecimal> getFlag() {
        return flag;
    }
    
    public Date getDate() {
        return date;
    }
    public void setDate(Date date) {
        this.date = date;
    }
}
