package com.quant.backtest.multi.strategy.utils;


import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Component;

@Component
public class DateUtils {
    
    DateTimeFormatter dateTimeFormatter = DateTimeFormatter.BASIC_ISO_DATE;

    public String getTMinus1Date(String date) {
	return LocalDate.parse(date).minusDays(1).format(dateTimeFormatter);
    }
}
