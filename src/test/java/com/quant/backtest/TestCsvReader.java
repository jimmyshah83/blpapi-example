package com.quant.backtest;

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang.StringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
public class TestCsvReader {

    @Test
    public void readCsvLineByLineTest() throws Exception {
	try (Reader reader = Files.newBufferedReader(Paths.get("/Users/jiviteshshah/Quant/application/Actual Portfolio/Holdings 42305285 October 29, 2018.csv"));
		CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withAllowMissingColumnNames())) {
	    for (CSVRecord csvRecord : csvParser) {
		if (StringUtils.equalsIgnoreCase("Important Information", csvRecord.get(0)))
		    break;
		if (csvRecord.getRecordNumber() == 9) {
		    System.out.println("Total Market Value = " + csvRecord.get(5));
		}
		if (csvRecord.getRecordNumber() >= 15) {
		    if (StringUtils.equalsIgnoreCase("Canadian Holdings", csvRecord.get(0))) {
			System.out.println("Symbol = " + csvRecord.get(2));
			System.out.println("Quantity = " + csvRecord.get(4));
			System.out.println("Market Value = " + csvRecord.get(10));
		    }
		}
	    }
	}
    }
}
