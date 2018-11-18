package com.quant.backtest.multi.strategy.executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bloomberglp.blpapi.Event;
import com.bloomberglp.blpapi.Message;
import com.bloomberglp.blpapi.MessageIterator;
import com.bloomberglp.blpapi.Session;

public abstract class BloombergExecutor {
    
    private static final Logger logger = LoggerFactory.getLogger(BloombergExecutor.class);
    
    protected void processMiscEvents(Event event, Session session) throws Exception {
	MessageIterator msgIter = event.messageIterator();
	while (msgIter.hasNext()) {
	    Message msg = msgIter.next();
	    logger.trace("MISC Event {}. Ignoring message = {}", event.eventType().toString(), msg);
	}
    }
}
