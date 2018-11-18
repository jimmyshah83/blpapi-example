package com.quant.backtest.multi.strategy.executors;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.bloomberglp.blpapi.Session;
import com.bloomberglp.blpapi.SessionOptions;

@Component
public class BloombergSession {
    
    private static final Logger logger = LoggerFactory.getLogger(BloombergSession.class);
    
    @Value("${service.host}")
    private String hostName;
    @Value("${service.port}")
    private int hostPort;
    @Value("${service.name}")
    private String serviceName;
    
    private Session session;

    @PostConstruct
    public void init() {
	SessionOptions sessionOptions = new SessionOptions();
	sessionOptions.setServerHost(hostName);
	sessionOptions.setServerPort(hostPort);
	session = new Session(sessionOptions);
	if (session.start() && session.openService(serviceName)) {
	    logger.info("Bloomberg SESSION established.");
	}
    }
    
    public Session getSession() {
	return session;
    }
}
