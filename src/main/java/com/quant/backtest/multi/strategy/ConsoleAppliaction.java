package com.quant.backtest.multi.strategy;

import java.io.FileNotFoundException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.Banner;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.quant.backtest.multi.strategy.executors.BBGCrateOrder;
import com.quant.backtest.multi.strategy.executors.handlers.EMSXMessageHandler;
import com.quant.backtest.multi.strategy.models.DailyTransaction;
import com.quant.backtest.multi.strategy.processors.OutputGenerator;

@SpringBootApplication
public class ConsoleAppliaction implements CommandLineRunner {
    
    private static final Logger logger = LoggerFactory.getLogger(ConsoleAppliaction.class);

    @Autowired
    private OutputGenerator outputGenerator;
    
    @Autowired
    private BBGCrateOrder createOrder;
    @Autowired
    private EMSXMessageHandler emsxHandler;

    public static void main(String[] args) throws Exception {
	SpringApplication application = new SpringApplication(ConsoleAppliaction.class);
	application.setBannerMode(Banner.Mode.OFF);
	application.run("");
    }

    @Override
    public void run(String... args) {
	try {
	    List<DailyTransaction> dailyTransactions = outputGenerator.process();
	    emsxHandler.setDaiyTransactions(dailyTransactions);
	    createOrder.placeOrder();
	} catch (FileNotFoundException e) {
	    logger.error("------------- ERROR RUNNING APPLICATION ------------- {}", e.getMessage());
	    logger.error("------------- SHUTTING DOWN -------------");
	    e.printStackTrace();
	}
    }
}
