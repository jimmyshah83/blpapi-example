package com.quant.backtest.multi.strategy.utils;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.quant.backtest.multi.strategy.properties.DatePropertiesLoader;

@Component
public class DateUtils {

    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.BASIC_ISO_DATE;
    
    @Autowired
    private DatePropertiesLoader datePropertiesLoader;

    public String getCurrentDate() {
	return dateTimeFormatter.format(LocalDate.now());
    }

    public String decrementCurrentDate(long decrementValue) {
	return dateTimeFormatter.format(LocalDate.now().minusDays(decrementValue));
    }
    
    public List<String> getLastNDays(int numberOfDays) {
	LocalDate initialDate = LocalDate.now();
	List<LocalDate> holidays = getAllHolidays();
	List<String> lastNDays = new ArrayList<>();
	LocalDate calculatedDate = null;
	while (numberOfDays != 0) {
	    calculatedDate = initialDate;
	    if (!holidays.contains(calculatedDate) && calculatedDate.getDayOfWeek() != DayOfWeek.SATURDAY && calculatedDate.getDayOfWeek() != DayOfWeek.SUNDAY) {
		lastNDays.add(dateTimeFormatter.format(calculatedDate));
		initialDate = calculatedDate.minusDays(1);
		 --numberOfDays;
	    } else {
		initialDate = calculatedDate.minusDays(1);
	    }
	}
	return lastNDays;
    }
    
    private List<LocalDate> getAllHolidays() {
	return datePropertiesLoader.getHolidays();
    }
}
