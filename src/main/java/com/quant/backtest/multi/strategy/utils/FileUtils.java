package com.quant.backtest.multi.strategy.utils;

import java.io.File;

import org.springframework.stereotype.Component;

@Component
public class FileUtils {

    public boolean doesFileExists(String filePath) {
	return new File(filePath).exists();
    }
}
