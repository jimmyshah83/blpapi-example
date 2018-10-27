package com.quant.backtest.multi.strategy.executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bloomberglp.blpapi.Event;
import com.bloomberglp.blpapi.Message;
import com.bloomberglp.blpapi.MessageIterator;
import com.bloomberglp.blpapi.Session;
import com.bloomberglp.blpapi.SessionOptions;

public abstract class BloombergExecutor {
    
    private static final Logger logger = LoggerFactory.getLogger(BloombergExecutor.class);

    protected Session createSession(String hostName, int hostPort) {
	SessionOptions sessionOptions = new SessionOptions();
	sessionOptions.setServerHost(hostName);
	sessionOptions.setServerPort(hostPort);
	return new Session(sessionOptions);
    }
    
    protected void processMiscEvents(Event event, Session session) throws Exception {
	MessageIterator msgIter = event.messageIterator();
	while (msgIter.hasNext()) {
	    Message msg = msgIter.next();
	    logger.debug("MISC Event {}. Ignoring message = {}", event.eventType().toString(), msg);
	}
    }
}
