package com.quant.backtest.multi.strategy.properties;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.lang.NonNull;
import org.springframework.validation.annotation.Validated;

@Validated
@Configuration
@PropertySource("classpath:other.properties")
@ConfigurationProperties(prefix="date")
public class OtherPropertiesLoader {
    
    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.BASIC_ISO_DATE;
    
    @NonNull
    private String holidays;

    public List<LocalDate> getHolidays() {
	List<String> stringHolidays = Arrays.asList(this.holidays.split(","));
	if (null != stringHolidays) {
	    List<LocalDate> localDateHolidays = new ArrayList<>();
	    for (String s : stringHolidays) {
		localDateHolidays.add(LocalDate.parse(s, dateTimeFormatter));
	    }
	    return localDateHolidays;
	}
        return null;
    } 
    
    public void setHolidays(String holidays) {
        this.holidays =  holidays;
    }
    
}
