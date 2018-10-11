package com.quant.backtest.multi.strategy.processors;

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.quant.backtest.multi.strategy.enums.Tickers;
import com.quant.backtest.multi.strategy.properties.InputPropertiesLoader;
import com.quant.backtest.multi.strategy.utils.CsvUtils;

@Component
public class OptimalMultiStrategyProcessor {

    private static final Logger logger = LoggerFactory.getLogger(OptimalMultiStrategyProcessor.class);
    private final Double DEFAULT_DOUBLE = 0.0d;

    @Autowired
    private InputPropertiesLoader inputPropertiesLoader;
    @Autowired
    private CsvUtils csvUtils;
    @Value("${min.ticker.for.cash}")
    private int minCashTickers;

    public Map<String, Double> process(Map<String, Double> allStrategyWeights, Double totalWeight, String date) throws FileNotFoundException {
	Map<String, List<String>> allStrategyTickers = new HashMap<>();
	for (String strategyName : inputPropertiesLoader.getStrategy().values()) {
	    List<String> tickers = csvUtils.readBacktestedCsv(inputPropertiesLoader.getFilePath() + strategyName + "/" + strategyName + ".BUY.STG." + date + ".csv");
	    if (inputPropertiesLoader.isUseCash() && tickers.size() < minCashTickers) {
		IntStream.range(tickers.size(), minCashTickers).forEach(i -> tickers.add(Tickers.CASH.toString()));
	    } else if (!inputPropertiesLoader.isUseCash() && tickers.size() == 0) {
		tickers.add("CASH");
	    }
	    allStrategyTickers.put(strategyName, tickers);
	}

	Map<String, Double> optimalWeightedStrategy = new HashMap<>();
	for (Entry<String, Double> strategyWeight : allStrategyWeights.entrySet()) {
	    if (DEFAULT_DOUBLE.equals(strategyWeight.getValue()))
		continue;
	    Double individualTickerWeightPercent = ((strategyWeight.getValue() / totalWeight) * 100.00d) / (allStrategyTickers.get(strategyWeight.getKey()).size());
	    logger.info("Individual % weight {} for each ticker in evenly balanced startegy {} with tickers {}", individualTickerWeightPercent, strategyWeight.getKey(),
		    allStrategyTickers.get(strategyWeight.getKey()).size());
	    for (String ticker : allStrategyTickers.get(strategyWeight.getKey())) {
		if (optimalWeightedStrategy.containsKey(ticker))
		    optimalWeightedStrategy.put(ticker, optimalWeightedStrategy.get(ticker) + individualTickerWeightPercent);
		else
		    optimalWeightedStrategy.put(ticker, individualTickerWeightPercent);
	    }
	}
	logger.info("Optimal {} day strategy = {}", date, optimalWeightedStrategy);
	return optimalWeightedStrategy;
    }
}
