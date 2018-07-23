package com.quant.backtest.multi.strategy.properties;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.lang.NonNull;
import org.springframework.validation.annotation.Validated;

@Validated
@Configuration
@PropertySource("classpath:quant-file.properties")
@ConfigurationProperties
public class FilePropertiesLoader {

    @NonNull
    private List<String> inputDate;
    @NonNull
    private String filePath;
    @NonNull
    private String outputFilePath;
    @NonNull
    private BigDecimal delta;
    
    public String getFilePath() {
        return filePath;
    }
    public void setFilePath(String filePath) {
        this.filePath = filePath;
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
    public BigDecimal getDelta() {
        return delta;
    }
    public void setDelta(String delta) {
        this.delta = new BigDecimal(delta).setScale(2, RoundingMode.HALF_EVEN);
    }
    public String getOutputFilePath() {
        return outputFilePath;
    }
    public void setOutputFilePath(String outputFilePath) {
        this.outputFilePath = outputFilePath;
    }
}
