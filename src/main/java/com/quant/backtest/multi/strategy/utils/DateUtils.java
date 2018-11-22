package com.quant.backtest.multi.strategy.utils;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.quant.backtest.multi.strategy.properties.OtherPropertiesLoader;

@Component
public class DateUtils {

    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.BASIC_ISO_DATE;
    private List<LocalDate> holidays;

    @Autowired
    private OtherPropertiesLoader otherPropertiesLoader;

    @PostConstruct
    private void init() {
	holidays = otherPropertiesLoader.getHolidays();
    }

    public String getCurrentDate() {
	return dateTimeFormatter.format(LocalDate.now());
    }

    public List<String> getLastNDays(int numberOfDays) {
	LocalDate initialDate = LocalDate.parse(getPreviousNWorkingDay(1), dateTimeFormatter);
	List<String> lastNDays = new ArrayList<>();
	LocalDate calculatedDate = null;
	while (numberOfDays != 0) {
	    calculatedDate = initialDate;
	    if (validateDate(calculatedDate)) {
		lastNDays.add(dateTimeFormatter.format(calculatedDate));
		initialDate = calculatedDate.minusDays(1);
		--numberOfDays;
	    } else {
		initialDate = calculatedDate.minusDays(1);
	    }
	}
	return lastNDays;
    }

    public String getPreviousNWorkingDay(int n) {
	String retVal = dateTimeFormatter.format(LocalDate.now());
	for (int i = n; i > 0; --i) {
	    retVal = getPreviousWorkingDay(retVal);
	}
	return retVal;
    }

    private String getPreviousWorkingDay(String startDate) {
	LocalDate initialDate = LocalDate.parse(startDate, dateTimeFormatter).minusDays(1);
	LocalDate calculatedDate = null;
	String retVal = "";
	int counter = 1;
	while (counter != 0) {
	    calculatedDate = initialDate;
	    if (validateDate(calculatedDate)) {
		retVal = dateTimeFormatter.format(calculatedDate);
		--counter;
	    } else {
		initialDate = calculatedDate.minusDays(1);
	    }
	}
	return retVal;
    }

    private boolean validateDate(LocalDate calculatedDate) {
	return !holidays.contains(calculatedDate) && calculatedDate.getDayOfWeek() != DayOfWeek.SATURDAY && calculatedDate.getDayOfWeek() != DayOfWeek.SUNDAY;
    }
}
