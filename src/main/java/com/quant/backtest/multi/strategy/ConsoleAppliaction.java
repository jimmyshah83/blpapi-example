package com.quant.backtest.multi.strategy;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.ParseException;
import java.time.LocalDate;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.Banner;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.quant.backtest.multi.strategy.executors.BBGCreateOrder;
import com.quant.backtest.multi.strategy.models.DailyTransaction;
import com.quant.backtest.multi.strategy.processors.ResultProcessor;

/**
 * Main {@linkplain CommandLineRunner} responsible to start the application
 */
@SpringBootApplication
public class ConsoleAppliaction implements CommandLineRunner {
    
    private static final Logger logger = LoggerFactory.getLogger(ConsoleAppliaction.class);

    @Autowired
    private ResultProcessor resultProcessor;
    
    @Autowired
    private BBGCreateOrder createOrder;

    public static void main(String[] args) throws Exception {
	SpringApplication application = new SpringApplication(ConsoleAppliaction.class);
	application.setBannerMode(Banner.Mode.OFF);
	application.run("");
    }

    @Override
    public void run(String... args) {
	List<DailyTransaction> dailyTransactions = null;
	try {
	    dailyTransactions = resultProcessor.process();
	    if (null != dailyTransactions)
		createOrder.placeOrder(dailyTransactions);
	    else
		logger.info("No transactions recorded on {}", LocalDate.now());
	} catch (FileNotFoundException e) {
	    logger.error("FILE UNAVAILABLE, error message: {}", e);
	    e.printStackTrace();
	} catch (IOException e) {
	    logger.error("COULD not access path to file, error message: {}", e);
	    e.printStackTrace();
	} catch (InterruptedException e) {
	    logger.error("ERROR establishing BBG SESSION, error message: {}", e);
	    e.printStackTrace();
	} catch (ParseException e) {
	    logger.error("ERROR Reading CSV file, error message: {}", e);
	    e.printStackTrace();
	} catch (Exception e) {
	    logger.error("ERROR. Something went wrong with BBG create order, error message: {}", e);
	    e.printStackTrace();
	}
    }
}
