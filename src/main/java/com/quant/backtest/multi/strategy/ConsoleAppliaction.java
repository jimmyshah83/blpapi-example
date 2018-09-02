package com.quant.backtest.multi.strategy;

import java.io.FileNotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.Banner;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.quant.backtest.multi.strategy.processors.OutputGenerator;

@SpringBootApplication
public class ConsoleAppliaction implements CommandLineRunner {
    
    private static final Logger logger = LoggerFactory.getLogger(ConsoleAppliaction.class);

    @Autowired
    private OutputGenerator outputGenerator;

    public static void main(String[] args) throws Exception {
	SpringApplication application = new SpringApplication(ConsoleAppliaction.class);
	application.setBannerMode(Banner.Mode.OFF);
	application.run("");
    }

    @Override
    public void run(String... args) {
	try {
	    outputGenerator.process();
	} catch (FileNotFoundException e) {
	    logger.error("------------- ERROR RUNNING APPLICATION ------------- {}", e.getMessage());
	    logger.error("------------- SHUTTING DOWN -------------");
	    e.printStackTrace();
	}
    }
}
