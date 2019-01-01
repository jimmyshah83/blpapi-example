package com.quant.backtest.multi.strategy.utils;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.quant.backtest.multi.strategy.cache.SimpleMapCache;
import com.quant.backtest.multi.strategy.enums.Side;
import com.quant.backtest.multi.strategy.enums.Tickers;
import com.quant.backtest.multi.strategy.executors.BloombergFetchSecurityData;
import com.quant.backtest.multi.strategy.models.ActualPortfolioTicker;
import com.quant.backtest.multi.strategy.models.CalculatedVars;
import com.quant.backtest.multi.strategy.models.PreviousDayTrade;

/**
 * Utility class to help with the Portfolio business logic.
 * 
 * @author jiviteshshah
 */
@Component
public class PortfolioUtils {

    private static final Logger logger = LoggerFactory.getLogger(PortfolioUtils.class);

    @Autowired
    private CsvUtils csvUtils;
    @Autowired
    private FileUtils fileUtils;
    @Autowired
    private DateUtils dateUtils;
    @Autowired
    private BloombergFetchSecurityData securityData;
    @Autowired
    private SimpleMapCache<String, ActualPortfolioTicker> mapCache;
    @Autowired
    private CalculatedVars calculatedVars;

    @Value("${trader.filePath}")
    private String previousDayTradeFilePath;
    @Value("${actual.portfolio.FilePath}")
    private String actualPortfolioFilePath;

    private static final String SEPARATOR = ".";
    private final String BBG_TICKER_APPENDER = " CN EQUITY";
    private final String TRADER_TICKER_APPENDER = " EQUITY";

    private Function<String, String> bbgTickerFinderFunction = a -> StringUtils.substringAfter(a, SEPARATOR).length() == 0 ? new StringBuilder(a).append(BBG_TICKER_APPENDER).toString()
	    : StringUtils.substringAfter(a, SEPARATOR).length() == 1
		    ? new StringBuilder(StringUtils.substringBefore(a, SEPARATOR)).append("/").append(StringUtils.substringAfter(a, SEPARATOR)).append(BBG_TICKER_APPENDER).toString()
		    : new StringBuilder(StringUtils.substringBefore(a, SEPARATOR)).append("-").append(StringUtils.substringAfter(a, SEPARATOR).charAt(0)).append(BBG_TICKER_APPENDER).toString();

    /**
     * Fetches the actual portfolio CSV and calculates the individual portfolio
     * % for each ticker
     * 
     * @param filePath
     *            Path to actual portfolio CSV
     * @return Map of tickers in actual portfolio and their respective portfolio
     *         % weight.
     * @throws IOException
     * @throws ParseException
     */
    public Map<String, BigDecimal> createActualPortfolio() throws IOException, ParseException {
	// 1. Fetch actual with 3 columns
	String filePath = new StringBuffer(actualPortfolioFilePath).append(dateUtils.getPreviousNWorkingDay(2)).append(".csv").toString();
	if (!fileUtils.doesFileExists(filePath)) {
	    logger.error("Actual portfolio does not exist. STOPPING execution");
	    return null;
	}
	List<ActualPortfolioTicker> actualPortfolioTickers = csvUtils.fetchActualPortfolioTickers(filePath);
	// We will deal with Maps from now on as its faster. The below map is created with BBG ID as key and the actual object as value
	for (ActualPortfolioTicker actualPortfolioTicker : actualPortfolioTickers) {
	    mapCache.cache(actualPortfolioTicker.getTicker(), actualPortfolioTicker);
	}

	// 2. Fetch previous day trades with 5 columns into the object
	List<PreviousDayTrade> previousDayTrades = csvUtils.fetchPreviousDayTrades(previousDayTradeFilePath + dateUtils.getPreviousNWorkingDay(1) + ".csv");
	if (null == actualPortfolioTickers || null == previousDayTrades)
	    return null;
	
	if (previousDayTrades.size() != 0) {
	    // Cash will be modified on every trader transaction. Assuming cash has to be present in the actual portfolio.
	    ActualPortfolioTicker actualPortfolioCashTicker = mapCache.retrieve(Tickers.CASH.toString());
	    
	    // 2a. Calculate the book value & market value for previous day trades
	    for (PreviousDayTrade previousDayTrade : previousDayTrades) {
		previousDayTrade.setBloombergID(StringUtils.trim(previousDayTrade.getBloombergID())+TRADER_TICKER_APPENDER);
		double latestPrice = 0.0d;
		try {
		    latestPrice = securityData.fetchLatestPrice(previousDayTrade.getBloombergID());
		    previousDayTrade.setMarketValue(previousDayTrade.getFillQty() * latestPrice);
		} catch (Exception e) {
		    logger.error("Error fetching latest Security Price for ticker: {}. Continuing processing without this ticker.", previousDayTrade.getBloombergID());
		    continue;
		}

		// 3. Merge the previous into actual
		if (StringUtils.equalsIgnoreCase(previousDayTrade.getSide(), Side.BUY.getName())) {
			previousDayTrade.setBookValue((previousDayTrade.getFillQty() * previousDayTrade.getAvgFillPx()) + previousDayTrade.getCommission());
		    if (mapCache.containsKey(previousDayTrade.getBloombergID())) {
			ActualPortfolioTicker actualPortfolioTicker = mapCache.retrieve(previousDayTrade.getBloombergID());
			actualPortfolioTicker.setQuantity(actualPortfolioTicker.getQuantity() + previousDayTrade.getFillQty());
			actualPortfolioTicker.setMarket_Value(new BigDecimal(actualPortfolioTicker.getQuantity() * latestPrice).setScale(Defaults.SCALE, Defaults.ROUNDING_MODE));
		    } else {
			ActualPortfolioTicker actualPortfolioTicker = new ActualPortfolioTicker();
			actualPortfolioTicker.setQuantity(previousDayTrade.getFillQty());
			actualPortfolioTicker.setTicker(previousDayTrade.getBloombergID());
			actualPortfolioTicker.setMarket_Value(new BigDecimal(previousDayTrade.getMarketValue()).setScale(Defaults.SCALE, Defaults.ROUNDING_MODE));
			mapCache.cache(previousDayTrade.getBloombergID(), actualPortfolioTicker);
		    }
		    actualPortfolioCashTicker.setMarket_Value(actualPortfolioCashTicker.getMarket_Value().subtract(new BigDecimal(previousDayTrade.getBookValue())).setScale(Defaults.SCALE, Defaults.ROUNDING_MODE));
		} else {
			previousDayTrade.setBookValue((previousDayTrade.getFillQty() * previousDayTrade.getAvgFillPx()) - previousDayTrade.getCommission());
		    // It is a sell so Actual should contain this ticker.
		    ActualPortfolioTicker actualPortfolioTicker = mapCache.retrieve(previousDayTrade.getBloombergID());
		    if (0.0 != actualPortfolioTicker.getQuantity() - previousDayTrade.getFillQty()) {
			actualPortfolioTicker.setQuantity(actualPortfolioTicker.getQuantity() - previousDayTrade.getFillQty());
			//actualPortfolioTicker.setMarket_Value(actualPortfolioTicker.getMarket_Value().subtract(new BigDecimal(previousDayTrade.getMarketValue())).setScale(Defaults.SCALE, Defaults.ROUNDING_MODE));
			actualPortfolioTicker.setMarket_Value(new BigDecimal(actualPortfolioTicker.getQuantity() * latestPrice).setScale(Defaults.SCALE, Defaults.ROUNDING_MODE));
		    } else {
			mapCache.remove(actualPortfolioTicker.getTicker());
		    }
		    actualPortfolioCashTicker.setMarket_Value(actualPortfolioCashTicker.getMarket_Value().add(new BigDecimal(previousDayTrade.getBookValue())).setScale(Defaults.SCALE, Defaults.ROUNDING_MODE));
		}
	    }
	}
	
	// Change all the remaining with the latest price
	for (Entry<String, ActualPortfolioTicker> actualPortfolioTickersMap : mapCache.entrySet()) {
		ActualPortfolioTicker actualPortfolioTicker = actualPortfolioTickersMap.getValue();
		if (StringUtils.equalsIgnoreCase(Tickers.CASH.toString(), actualPortfolioTicker.getTicker())) 
			continue;
		double latestPrice = 0.0d;
		try {
			latestPrice = securityData.fetchLatestPrice(actualPortfolioTicker.getTicker());
		} catch (Exception e) {
			logger.error("Error fetching latest Security Price for ticker: {}. Continuing processing without this ticker.", actualPortfolioTicker.getTicker());
		    continue;
		}
		actualPortfolioTicker.setMarket_Value(new BigDecimal(actualPortfolioTicker.getQuantity() * latestPrice).setScale(Defaults.SCALE, Defaults.ROUNDING_MODE));
	}
	
	// 4. Write the actual portfolio as T-1
	csvUtils.writeActualPortfolio(actualPortfolioFilePath + dateUtils.getPreviousNWorkingDay(1) + ".csv");
	return createPortfolioWeightedHoldings();
    }

    /**
     * Converts the morning star ticker to Bloomberg ticker.
     * 
     * @param optimalPortfolioTicker
     * @return
     */
    public String convertToBloombergTicker(String optimalPortfolioTicker) {
	return bbgTickerFinderFunction.apply(optimalPortfolioTicker);
    }
    
    private Map<String, BigDecimal> createPortfolioWeightedHoldings() {
	Map<String, BigDecimal> actualPortfolio = new HashMap<>();
	for (Entry<String, ActualPortfolioTicker> actualPortfolioTickersMap : mapCache.entrySet()) {
	    ActualPortfolioTicker actualPortfolioTicker = actualPortfolioTickersMap.getValue();
	    BigDecimal multipliedVal = actualPortfolioTicker.getMarket_Value().multiply(new BigDecimal("100.00")).setScale(Defaults.SCALE, Defaults.ROUNDING_MODE);
	    BigDecimal percentHolding = multipliedVal.divide(new BigDecimal(calculatedVars.getTotalMarketValue()), Defaults.SCALE, Defaults.ROUNDING_MODE);
	    actualPortfolio.put(actualPortfolioTicker.getTicker(), percentHolding);
	}
	return actualPortfolio;
    }
}
