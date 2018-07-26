package com.quant.backtest.multi.strategy.properties;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.validation.annotation.Validated;

@Validated
@Configuration
@ConfigurationProperties
public class InputPropertiesLoader {

    private Map<String, Double> sortino = new HashMap<>();
    private Map<String, Integer> flag = new HashMap<>();
    private Map<String, String> strategy = new HashMap<>();
    @NonNull
    private BigDecimal delta;
    @NonNull
    private List<String> inputDate;
    @NonNull
    private String filePath;
    @NonNull
    private String outputFilePath;
    @NonNull
    private BigDecimal capital;
    
    public void setSortino(Map<String, Double> sortino) {
        this.sortino = sortino;
    }
    public void setFlag(Map<String, Integer> flag) {
        this.flag = flag;
    }
    public Map<String, Double> getSortino() {
        return sortino;
    }
    public Map<String, Integer> getFlag() {
        return flag;
    }
    public Map<String, String> getStrategy() {
        return strategy;
    }
    public void setStrategy(Map<String, String> strategy) {
        this.strategy = strategy;
    }
    public BigDecimal getDelta() {
        return delta;
    }
    public void setDelta(String delta) {
        this.delta = new BigDecimal(delta).setScale(2, RoundingMode.HALF_EVEN);
    }
    public List<String> getInputDate() {
        return inputDate;
    }
    public void setInputDate(String inputDate) {
        this.inputDate = Arrays.asList(inputDate.split(","));
    }
    public Double getinputDateSize() {
    	return Double.valueOf(inputDate.size());
    }
    public String getFilePath() {
        return filePath;
    }
    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
    public String getOutputFilePath() {
        return outputFilePath;
    }
    public void setOutputFilePath(String outputFilePath) {
        this.outputFilePath = outputFilePath;
    }
	public void setCapital(String capital) {
		this.capital = new BigDecimal(capital).setScale(2, RoundingMode.HALF_EVEN);;
	}
	public BigDecimal getCapital() {
		return capital;
	}
}
