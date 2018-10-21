package com.quant.backtest.multi.strategy.utils;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.quant.backtest.multi.strategy.enums.ActualPortfolioHeader;

/**
 * Utility class to help with the Portofolio business logic.
 * @author jiviteshshah
 */
@Component
public class PortfolioUtils {

    @Autowired
    private CsvUtils csvUtils;

    private static final String SEPARATOR = ".";
    private final NumberFormat numberFormat = NumberFormat.getInstance();
    private final String BBG_TICKER_APPENDER = " CN EQUITY";

    private Function<String, String> bbgTickerFinderFunction = a -> StringUtils.substringAfter(a, SEPARATOR).length() == 0 ? new StringBuilder(a).append(BBG_TICKER_APPENDER).toString()
	    : StringUtils.substringAfter(a, SEPARATOR).length() == 1
		    ? new StringBuilder(StringUtils.substringBefore(a, SEPARATOR)).append("/").append(StringUtils.substringAfter(a, SEPARATOR)).append(BBG_TICKER_APPENDER).toString()
		    : new StringBuilder(StringUtils.substringBefore(a, SEPARATOR)).append("-").append(StringUtils.substringAfter(a, SEPARATOR).charAt(0)).append(BBG_TICKER_APPENDER).toString();

    /**
     * Fetches the actual portfolio csv and calculates the individual portofolio % for each ticker
     * @param filePath Path to actual portfolio CSV
     * @return Map of tickers in actual portfolio and their respective portfolio % weight.
     * @throws IOException
     * @throws ParseException
     */
    public Map<String, BigDecimal> createActualPortfolio(String filePath) throws IOException, ParseException {
	Set<Map<String, Object>> actualPortfolioTickers = csvUtils.fetchActualPortfolioFromCsv(filePath);
	Double totalMarketValue = csvUtils.getTotalMarketValue();
	Map<String, BigDecimal> actualPortfolio = new HashMap<>();
	for (Map<String, Object> actualPortfolioTicker : actualPortfolioTickers) {
	    String portfolioMarketValue = (String) actualPortfolioTicker.get(ActualPortfolioHeader.MarketValue.getValue());
	    BigDecimal value = null;
	    if (StringUtils.isNotBlank(portfolioMarketValue)) {
		Double portfolioTicker = (numberFormat.parse(portfolioMarketValue).doubleValue() / totalMarketValue) * 100;
		value = new BigDecimal(portfolioTicker).setScale(Defaults.SCALE, RoundingMode.HALF_EVEN);
	    } else
		value = new BigDecimal(0d).setScale(Defaults.SCALE, RoundingMode.HALF_EVEN);
	    actualPortfolio.put((String) actualPortfolioTicker.get(ActualPortfolioHeader.Ticker.getValue()), value);

	}
	return actualPortfolio;
    }

    /**
     * Converts the morning star ticker to Bloomberg ticker.
     * @param optimalPortfolioTicker
     * @return
     */
    public String convertToBloombergTicker(String optimalPortfolioTicker) {
	return bbgTickerFinderFunction.apply(optimalPortfolioTicker);
    }

    /**
     * Should only be called after the actual portfolio has been read. If called prematurely or not available, a default of 100k would be returned.
     * @return total market value of Portfolio
     */
    public Double getTotalMarketValueOfPortfolio() {
	return csvUtils.getTotalMarketValue();
    }
}
