package com.quant.backtest.multi.strategy.utils;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.supercsv.cellprocessor.ParseBigDecimal;
import org.supercsv.cellprocessor.ParseDouble;
import org.supercsv.cellprocessor.constraint.NotNull;
import org.supercsv.cellprocessor.ift.CellProcessor;
import org.supercsv.io.CsvBeanReader;
import org.supercsv.io.CsvBeanWriter;
import org.supercsv.io.CsvListWriter;
import org.supercsv.io.ICsvBeanReader;
import org.supercsv.io.ICsvBeanWriter;
import org.supercsv.io.ICsvListWriter;
import org.supercsv.prefs.CsvPreference;

import com.quant.backtest.multi.strategy.cache.SimpleMapCache;
import com.quant.backtest.multi.strategy.models.ActualPortfolioTicker;
import com.quant.backtest.multi.strategy.models.CalculatedVars;
import com.quant.backtest.multi.strategy.models.PreviousDayTrade;

/**
 * CSV utility class to read / write all CSV files
 * @author jiviteshshah
 */
@Component
public class CsvUtils {
    
    private static final Logger logger = LoggerFactory.getLogger(CsvUtils.class);

    @Autowired
    private DateUtils dateUtils;
    @Autowired
    private SimpleMapCache<String, ActualPortfolioTicker> mapCache;
    @Autowired
    private CalculatedVars calculatedVars;
    
    private final CellProcessor[] actualPortfolioCellProcessors = new CellProcessor[] {
	new NotNull(), // Ticker
	new ParseDouble(), // Quantity
	new ParseBigDecimal() // Market Value
    };
    
    private final Map<String, CellProcessor> DESIRED_COLUMNS = new HashMap<>();
    
    @PostConstruct
    public void init() {
	DESIRED_COLUMNS.put("Side", new NotNull());
	DESIRED_COLUMNS.put("BloombergID", new NotNull());
	DESIRED_COLUMNS.put("TgtQty", new ParseDouble());
	DESIRED_COLUMNS.put("AvgFillPx", new ParseDouble());
	DESIRED_COLUMNS.put("Commission", new ParseDouble());
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
     * Fetch T-2 actual portfolio 
     * @param filePath path to CSV file
     * @return list of all the tickers in the actual portfolio
     */
    public List<ActualPortfolioTicker> fetchActualPortfolioTickers(String filePath) {
	ICsvBeanReader beanReader = null;
	List<ActualPortfolioTicker> actualPortfolioTickers =  new ArrayList<>();
	try {
	    try {
		beanReader = new CsvBeanReader(new FileReader(filePath), CsvPreference.STANDARD_PREFERENCE);
		final String[] header = beanReader.getHeader(true);
		ActualPortfolioTicker actualPortfolioTicker;
		while((actualPortfolioTicker = beanReader.read(ActualPortfolioTicker.class, header, actualPortfolioCellProcessors)) != null) {
		    actualPortfolioTickers.add(actualPortfolioTicker);
		}
		return actualPortfolioTickers;
	    } catch (FileNotFoundException e) {
		logger.error("Could not find actual portfolio from location: {}", filePath);
		return null;
	    } catch (IOException e) {
		logger.error("Could not handle actual portfolio from location: {}", filePath);
		return null;
	    } catch (Exception e) {
		logger.error("Could not read the contents of actual portfolio from location: {}", filePath);
		return null;
	    }
	} finally {
	    if(beanReader != null ) {
                try {
		    beanReader.close();
		} catch (IOException e) {
		    logger.warn("Unable to close the bean reader: {}", filePath);
		}
	    }
	}
    }
    
    /**
     * Write the new actual portfolio (T-1) back to file
     * @param filePath path where you want the CSV to be written
     */
    public void writeActualPortfolio(String filePath) {
	ICsvBeanWriter beanWriter = null;
	try {
	    try {
		beanWriter = new CsvBeanWriter(new FileWriter(filePath), CsvPreference.STANDARD_PREFERENCE);
		final String[] header = new String[] { "Ticker", "Quantity", "Market_Value" };
		beanWriter.writeHeader(header);
		for (final Entry<String, ActualPortfolioTicker> actualPortfolioTickersMap : mapCache.entrySet()) {
		    ActualPortfolioTicker actualPortfolioTicker = actualPortfolioTickersMap.getValue();
                    beanWriter.write(actualPortfolioTicker, header);
                    calculatedVars.setTotalMarketValue(calculatedVars.getTotalMarketValue()+actualPortfolioTicker.getMarket_Value().doubleValue());
		}
	    } catch (Exception e) {
		logger.error("Could not write actual portfolio to location: {}", filePath);
		return;
	    }
	} finally {
	    if( beanWriter != null ) {
                try {
		    beanWriter.close();
		} catch (IOException e) {
		    logger.warn("Failed to close bean writer.");
		}
	    }
	}
    }
    
    /**
     * Fetch T-1 trader file with all previous day trades
     * @param filePath path to CSV file
     * @return list of all the transactions in the trader file from previous day
     */
    public List<PreviousDayTrade> fetchPreviousDayTrades(String filePath) {
	ICsvBeanReader beanReader = null;
	List<PreviousDayTrade> previousDayTrades =  new ArrayList<>();
	try {
	    try {
		beanReader = new CsvBeanReader(new FileReader(filePath), CsvPreference.STANDARD_PREFERENCE);
		final String[] header = beanReader.getHeader(true);
		final CellProcessor[] processors =  new CellProcessor[header.length];
		    for (int i = 0; i < header.length; i++) {
		        final CellProcessor processor = DESIRED_COLUMNS.get(header[i]);
		        if (processor != null) {
		          processors[i] = processor; // set up processor for desired columns
		        } else {
		          header[i] = null; // skip undesired columns
		        }
		      }
		PreviousDayTrade previousDayTrade;
		while((previousDayTrade = beanReader.read(PreviousDayTrade.class, header, processors)) != null) {
		    previousDayTrades.add(previousDayTrade);
		}
		return previousDayTrades;
	    } catch (FileNotFoundException e) {
		logger.error("Could not find actual portfolio from location: {}", filePath);
		return null;
	    } catch (IOException e) {
		logger.error("Could not handle actual portfolio from location: {}", filePath);
		return null;
	    } catch (Exception e) {
		logger.error("Could not read the contents of actual portfolio from location: {}", filePath);
		return null;
	    }
	} finally {
	    if(beanReader != null ) {
                try {
		    beanReader.close();
		} catch (IOException e) {
		    logger.warn("Unable to close the bean reader: {}", filePath);
		}
	    }
	}
    }
}
