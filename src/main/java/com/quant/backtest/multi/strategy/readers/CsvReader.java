package com.quant.backtest.multi.strategy.readers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.stereotype.Component;

@Component
public class CsvReader {

    public Set<String> readCsv(String pathName) {
	Set<String> tickers = null;
	try (Stream<String> stream = Files.lines(Paths.get(pathName))) {
	    tickers = stream.skip(4).map(line -> {
		String[] str = line.split(",");
		return str[0];
	    }).collect(Collectors.toSet());
	} catch (IOException ioe) {
	    ioe.printStackTrace();
	}
	return tickers;
    }
}
