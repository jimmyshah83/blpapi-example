package com.quant.backtest.multi.strategy.utils;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.NumberFormat;
import java.text.ParseException;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

/**
 * CSV utility class to read / write all CSV files
 * @author jiviteshshah
 */
@Component
public class CsvUtils {
    
    private static final Logger logger = LoggerFactory.getLogger(CsvUtils.class);

    @Autowired
    private DateUtils dateUtils;
    private Double totalMarketVale;
    private final NumberFormat numberFormat = NumberFormat.getInstance();
    
    private final Map<String, CellProcessor> DESIRED_COLUMNS = new HashMap<>();
    
    @PostConstruct
    public void init() {
	DESIRED_COLUMNS.put(ActualPortfolioHeader.Company.getValue(), new NotNull());
	DESIRED_COLUMNS.put(ActualPortfolioHeader.Ticker.getValue(), new Optional());
	DESIRED_COLUMNS.put(ActualPortfolioHeader.MarketValue.getValue(), new Optional());
    }

    /**
     * Fetch Morning star back tested CSV.
     * @param filePath Path to back tested CSV file.
     * @return 
     * @throws FileNotFoundException
     */
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

    /**
     * Write the optimal portfolio to CSV
     * @param filePath Path where the optimal portfolio would be written.
     * @param map Map that would be written as CSV body.
     * @throws IOException
     */
    public void writeMapToCsv(String filePath, Map<String, BigDecimal> map) throws IOException {
	final String[] header = new String[] { "Ticker", "Percent Weight" };
	try (ICsvListWriter listWriter = new CsvListWriter(new FileWriter(filePath + "optimal-" + dateUtils.getCurrentDate() + ".csv"), CsvPreference.STANDARD_PREFERENCE)) {
	    listWriter.write(header[0], header[1]);
	    for (Entry<String, BigDecimal> row : map.entrySet()) {
		listWriter.write(row.getKey(), row.getValue());
	    }
	}
    }
    
    /**
     * Fethc the actual portfolio into a Map with 3 columns Company, Ticker and Market Value and ignoring the rest.
     * @param filePath Path to Actual portfolio
     * @return The actual portfolio
     * @throws IOException
     * @throws ParseException
     */
    public Set<Map<String, Object>> fetchActualPortfolioFromCsv(String filePath) throws IOException, ParseException {
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
		    totalMarketVale = numberFormat.parse(StringUtils.trim((String) actualPortfolioRow.get(ActualPortfolioHeader.MarketValue.getValue()))).doubleValue();
		    break;
		}
		actualPortfolioSet.add(actualPortfolioRow);
		logger.debug("lineNo=%s, rowNo=%s, autal Portfolio row=%s", mapReader.getLineNumber(), mapReader.getRowNumber(), actualPortfolioRow);
	    }
	} finally {
	    if (mapReader != null) {
		mapReader.close();
	    }
	}
	return actualPortfolioSet;
    }
    
    /**
     * Get the total market value of Portfolio. If called prematurely or not available, a default of 100k would be returned.
     * @return total market value of Portfolio
     */
    public Double getTotalMarketValue() {
	return java.util.Optional.ofNullable(totalMarketVale).orElse(100000d);
    }
    
}
