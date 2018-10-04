package com.quant.backtest.multi.strategy.executors;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.bloomberglp.blpapi.EventHandler;
import com.bloomberglp.blpapi.Session;
import com.bloomberglp.blpapi.SessionOptions;

@Component(value = "createOrder")
public class BBGCrateOrder {

    private static final Logger logger = LoggerFactory.getLogger(BBGCrateOrder.class);
    
    @Autowired
    private EventHandler emsxHandler;

    @Value("${service.host}")
    private String hostName;
    @Value("${service.port}")
    private int hostPort;

    public void placeOrder() {
	logger.debug("Starting bloomberg session");
	try {
	    createAndStartSessionSession();
	} catch (Exception e) {
	    logger.error("ERROR starting session {}", e);
	}
    }

    private Session createAndStartSessionSession() throws IOException {
	SessionOptions sessionOptions = new SessionOptions();
	sessionOptions.setServerHost(hostName);
	sessionOptions.setServerPort(hostPort);
	Session session = new Session(sessionOptions, emsxHandler);
	session.startAsync();
	return session;
    }
}
