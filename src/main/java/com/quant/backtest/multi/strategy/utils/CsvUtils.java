package com.quant.backtest.multi.strategy.utils;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.supercsv.cellprocessor.Optional;
import org.supercsv.cellprocessor.constraint.NotNull;
import org.supercsv.cellprocessor.ift.CellProcessor;
import org.supercsv.io.CsvListWriter;
import org.supercsv.io.CsvMapReader;
import org.supercsv.io.ICsvListWriter;
import org.supercsv.io.ICsvMapReader;
import org.supercsv.prefs.CsvPreference;

import com.quant.backtest.multi.strategy.enums.ActualPortfolioHeader;

@Component
public class CsvUtils {

    @Autowired
    private DateUtils dateUtils;
    private Double totalMarketCap;
    private final NumberFormat numberFormat = NumberFormat.getInstance();
    
    private final Map<String, CellProcessor> DESIRED_COLUMNS = new HashMap<>();
    
    @PostConstruct
    public void init() {
	DESIRED_COLUMNS.put(ActualPortfolioHeader.Company.getValue(), new NotNull());
	DESIRED_COLUMNS.put(ActualPortfolioHeader.Ticker.getValue(), new Optional());
	DESIRED_COLUMNS.put(ActualPortfolioHeader.MarketValue.getValue(), new Optional());
    }

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

    public void writeMapToCsv(String filePath, Map<String, BigDecimal> map) throws IOException {
	final String[] header = new String[] { "Ticker", "Percent Weight" };
	try (ICsvListWriter listWriter = new CsvListWriter(new FileWriter(filePath + "optimal-" + dateUtils.getCurrentDate() + ".csv"), CsvPreference.STANDARD_PREFERENCE)) {
	    listWriter.write(header[0], header[1]);
	    for (Entry<String, BigDecimal> row : map.entrySet()) {
		listWriter.write(row.getKey(), row.getValue());
	    }
	}
    }
    
    public Set<Map<String, Object>> fetchActualPortfolioFromCsv(String filePath) throws Exception {
	ICsvMapReader mapReader = null;
	Map<String, Object> actualPortfolioRow;
	Set<Map<String, Object>> actualPortfolioSet = new HashSet<>();
	try {
	    mapReader = new CsvMapReader(new FileReader(filePath), CsvPreference.STANDARD_PREFERENCE);
	    final String[] header = mapReader.getHeader(true);
	    final CellProcessor[] processors =  new CellProcessor[header.length];
	    for (int i = 0; i < header.length; i++) {
	        final CellProcessor processor = DESIRED_COLUMNS.get(header[i]);
	        if (processor != null) {
	          processors[i] = processor; // set up processor for desired columns
	        } else {
	          header[i] = null; // skip undesired columns
	        }
	      }
	    while ((actualPortfolioRow = mapReader.read(header, processors)) != null) {
//		Check to end the loop when we have the market Value
		if (StringUtils.equalsIgnoreCase("Total", StringUtils.trim((String) actualPortfolioRow.get(ActualPortfolioHeader.Company.getValue())))) {
		    totalMarketCap = numberFormat.parse(StringUtils.trim((String) actualPortfolioRow.get(ActualPortfolioHeader.MarketValue.getValue()))).doubleValue();
		    break;
		}
		actualPortfolioSet.add(actualPortfolioRow);
		System.out.println(String.format("lineNo=%s, rowNo=%s, autal Portfolio row=%s", mapReader.getLineNumber(), mapReader.getRowNumber(), actualPortfolioRow));
	    }
	} finally {
	    if (mapReader != null) {
		mapReader.close();
	    }
	}
	return actualPortfolioSet;
    }
    
    public Double getTotalMarketValue() {
	return totalMarketCap;
    }
    
}
