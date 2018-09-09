package com.quant.backtest.multi.strategy.utils;

import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.supercsv.io.CsvListWriter;
import org.supercsv.io.ICsvListWriter;
import org.supercsv.prefs.CsvPreference;

@Component
public class CsvUtils {

    private final String[] header = new String[] { "Ticker", "Percent Weight" };

    @Autowired
    private DateUtils dateUtils;

    public List<String> readBacktestedCsv(String filePath) throws FileNotFoundException {
	List<String> tickers = null;
	try (Stream<String> stream = Files.lines(Paths.get(filePath))) {
	    tickers = stream.skip(4).map(line -> {
		String[] str = line.split(",");
		return str[0];
	    }).collect(Collectors.toList());
	} catch (IOException ioe) {
	    throw new FileNotFoundException("Cannot find file : " + filePath);
	}
	return tickers;
    }

    public void writeMapToCsv(String filePath, Map<String, Double> map) throws IOException {
	try (ICsvListWriter listWriter = new CsvListWriter(
		new FileWriter(filePath + "actual-" + dateUtils.getCurrentDate() + ".csv"),
		CsvPreference.STANDARD_PREFERENCE)) {
	    listWriter.write(header[0], header[1]);
	    for (Entry<String, Double> row : map.entrySet()) {
		listWriter.write(row.getKey(), new BigDecimal(row.getValue()).setScale(2, RoundingMode.HALF_EVEN));
	    }
	}
    }

    public Map<String, Double> readCsvToMap(String filePath) throws IOException {
	try (Stream<String> lines = Files.lines(Paths.get(filePath))) {
	    return lines.map(line -> line.split(",")).skip(1)
		    .collect(Collectors.toMap(line -> line[0], line -> Double.valueOf(line[1])));
	}
    }
}
