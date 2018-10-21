package com.quant.backtest.multi.strategy.executors;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.bloomberglp.blpapi.CorrelationID;
import com.bloomberglp.blpapi.Event;
import com.bloomberglp.blpapi.Message;
import com.bloomberglp.blpapi.MessageIterator;
import com.bloomberglp.blpapi.Name;
import com.bloomberglp.blpapi.Request;
import com.bloomberglp.blpapi.Service;
import com.bloomberglp.blpapi.Session;
import com.quant.backtest.multi.strategy.executor.BloombergExecutor;

@Component(value = "refData")
public class BloombergReferenceData extends BloombergExecutor {

    private static final Logger logger = LoggerFactory.getLogger(BloombergReferenceData.class);
    
    private static final Name ERROR_INFO = new Name("ErrorInfo");
    private static final Name FETCH_PORTFOLIO = new Name("PortfolioDataRequest");

    @Value("${service.host}")
    private String hostName;
    @Value("${service.port}")
    private int hostPort;
    @Value("${refdata.service.name}")
    private String serviceName;

    public void fetchActualPortfolio() throws IOException, InterruptedException, Exception {
	logger.debug("Starting BLOOMBERG session");
	Session session = super.createSession(hostName, hostPort);
	if (session.start() && session.openService(serviceName)) {
	    Service service = session.getService(serviceName);
	    Request request = service.createRequest("PortfolioDataRequest");
	    request.getElement("securities").appendValue("UXXXXXXX-X Client");
	    request.getElement("fields").appendValue("PORTFOLIO_MEMBER");
	    request.getElement("fields").appendValue("PORTFOLIO_MPOSITION");
	    request.getElement("fields").appendValue("PORTFOLIO_MWEIGHT");
	    request.getElement("fields").appendValue("PORTFOLIO_DATA");
	    session.sendRequest(request, new CorrelationID());
	    boolean continueLoop = true;
	    while (continueLoop) {
		Event event = session.nextEvent();
		switch (event.eventType().intValue()) {
		case Event.EventType.Constants.PARTIAL_RESPONSE:
		case Event.EventType.Constants.RESPONSE:
		    processResponse(event, session);
		    continueLoop = false;
		    break;
		default:
		    processMiscEvents(event, session);
		}
	    }
	    logger.debug("BLOOMBERG Portfolio request completed.");
	    session.stop();
	}
    }

    private void processMiscEvents(Event event, Session session) throws Exception {
	MessageIterator msgIter = event.messageIterator();
	while (msgIter.hasNext()) {
	    Message msg = msgIter.next();
	    logger.debug("MISC Event {}. Ignoring message = {}", event.eventType().toString(), msg);
	}
    }

    private void processResponse(Event event, Session session) throws Exception {
	logger.info("Processing RESPOINSE Event {}", event.eventType().toString());
	MessageIterator msgIter = event.messageIterator();
	while (msgIter.hasNext()) {
	    Message msg = msgIter.next();
	    logger.debug("MESSAGE: {}", msg.toString());
	    logger.info("CORRELATION ID: {}", msg.correlationID());
	    if (event.eventType() == Event.EventType.RESPONSE) {
		logger.info("Message Type = {}", msg.messageType());
		if (msg.messageType().equals(ERROR_INFO)) {
		    Integer errorCode = msg.getElementAsInt32("ERROR_CODE");
		    String errorMessage = msg.getElementAsString("ERROR_MESSAGE");
		    logger.warn("ERROR CODE: {} \tERROR MESSAGE: {}", errorCode, errorMessage);
		} else if (msg.messageType().equals(FETCH_PORTFOLIO)) {
		    Integer emsx_sequence = msg.getElementAsInt32("EMSX_SEQUENCE");
		    String message = msg.getElementAsString("MESSAGE");
		    logger.warn("EMSX_SEQUENCE: {} \tMESSAGE: {}", emsx_sequence, message);
		}
	    }
	}
    }
}
