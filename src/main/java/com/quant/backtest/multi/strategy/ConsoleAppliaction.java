package com.quant.backtest.multi.strategy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.quant.backtest.multi.strategy.processors.OptimalMultiStrategyProcessor;

@SpringBootApplication
public class ConsoleAppliaction implements CommandLineRunner {
    
    private static final Logger logger = LoggerFactory.getLogger(ConsoleAppliaction.class);
    
    @Autowired
    private OptimalMultiStrategyProcessor optimalMultiStrategyProcessor;
    
    public static void main(String[] args) throws Exception {
	SpringApplication application = new SpringApplication(ConsoleAppliaction.class);
	application.run("");
    }
    
    @Override
    public void run(String... args) throws Exception {
	logger.info("Starting Application");
	optimalMultiStrategyProcessor.process();
    }

}
