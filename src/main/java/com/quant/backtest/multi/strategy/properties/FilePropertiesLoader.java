package com.quant.backtest.multi.strategy.properties;

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
    private String inputDate;
    @NonNull
    private String filePath;
    
    public String getFilePath() {
        return filePath;
    }
    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
    public String getInputDate() {
        return inputDate;
    }
    public void setInputDate(String inputDate) {
        this.inputDate = inputDate;
    }
}
