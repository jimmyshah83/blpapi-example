package com.quant.backtest.multi.strategy.properties;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.lang.NonNull;
import org.springframework.validation.annotation.Validated;

@Validated
@Configuration
@PropertySource("classpath:quant-file.properties")
@ConfigurationProperties(prefix="file")
public class FilePropertiesLoader {

    private Map<String, Integer> strategy = new HashMap<>();
    @NonNull
    private String filePath;
    
    public Map<String, Integer> getStrategy() {
        return strategy;
    }
    public void setStrategy(Map<String, Integer> strategy) {
        this.strategy = strategy;
    }
    public String getFilePath() {
        return filePath;
    }
    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
}
