package com.quant.backtest.multi.strategy.utils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Component;

@Component
public class DateUtils {
    
    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.BASIC_ISO_DATE;
    
    public String getCurrentDate() {
	return dateTimeFormatter.format(LocalDate.now());
    }
}
