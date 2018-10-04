package com.quant.backtest.multi.strategy.executors.handlers;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.bloomberglp.blpapi.CorrelationID;
import com.bloomberglp.blpapi.Event;
import com.bloomberglp.blpapi.EventHandler;
import com.bloomberglp.blpapi.Message;
import com.bloomberglp.blpapi.MessageIterator;
import com.bloomberglp.blpapi.Name;
import com.bloomberglp.blpapi.Request;
import com.bloomberglp.blpapi.Service;
import com.bloomberglp.blpapi.Session;
import com.quant.backtest.multi.strategy.models.DailyTransaction;

@Component(value = "emsxHandler")
public class EMSXMessageHandler implements EventHandler {

    private static final Logger logger = LoggerFactory.getLogger(EMSXMessageHandler.class);

    private static final Name SESSION_STARTED = new Name("SessionStarted");
    private static final Name SESSION_STARTUP_FAILURE = new Name("SessionStartupFailure");
    private static final Name SERVICE_OPENED = new Name("ServiceOpened");
    private static final Name SERVICE_OPEN_FAILURE = new Name("ServiceOpenFailure");
    private static final Name ERROR_INFO = new Name("ErrorInfo");
    private static final Name CREATE_ORDER = new Name("CreateOrder");

    private List<CorrelationID> requestIds;
    private List<DailyTransaction> daiyTransactions;

//    Test how this would look like with a single ticket
    public void setDaiyTransactions(List<DailyTransaction> daiyTransactions) {
        this.daiyTransactions = daiyTransactions;
    }

    @Value("${service.name}")
    private String serviceName;

    @Override
    public void processEvent(Event event, Session session) {
	try {
	    switch (event.eventType().intValue()) {
	    case Event.EventType.Constants.SESSION_STATUS:
		processSessionEvent(event, session);
		break;
	    case Event.EventType.Constants.SERVICE_STATUS:
		processServiceEvent(event, session);
		break;
	    case Event.EventType.Constants.RESPONSE:
		processResponseEvent(event, session);
		break;
	    default:
		processMiscEvents(event, session);
		break;
	    }
	} catch (Exception e) {
	    logger.error("Error Handling event {}", e);
	    e.printStackTrace();
	}
    }

    private boolean processSessionEvent(Event event, Session session) throws Exception {
	logger.info("Processing {}", event.eventType().toString());
	MessageIterator msgIter = event.messageIterator();
	while (msgIter.hasNext()) {
	    Message msg = msgIter.next();
	    if (msg.messageType().equals(SESSION_STARTED)) {
		logger.info("Session STARTED");
		session.openServiceAsync(serviceName);
	    } else if (msg.messageType().equals(SESSION_STARTUP_FAILURE)) {
		logger.warn("Error: Session startup failed");
		return false;
	    }
	}
	return true;
    }

    private boolean processServiceEvent(Event event, Session session) {
	logger.info("Processing {}", event.eventType().toString());
	MessageIterator msgIter = event.messageIterator();
	while (msgIter.hasNext()) {
	    Message msg = msgIter.next();
	    if (msg.messageType().equals(SERVICE_OPENED)) {
		logger.info("Service opened...");
		Service service = session.getService(serviceName);
		Request request = service.createRequest("CreateOrder");
		request.set("EMSX_TICKER", "IBM US Equity");
		request.set("EMSX_AMOUNT", 1000);
		request.set("EMSX_ORDER_TYPE", "MKT");
		request.set("EMSX_TIF", "DAY");
		request.set("EMSX_HAND_INSTRUCTION", "ANY");
		request.set("EMSX_SIDE", "BUY");
		request.set("EMSX_BROKER", "????????");
		logger.info("Created REQUEST {}", request.toString());
		CorrelationID requestID = new CorrelationID();
		requestIds.add(requestID);
		// Submit the request
		try {
		    session.sendRequest(request, requestID);
		} catch (Exception ex) {
		    logger.error("Failed to send the request {}", ex);
		    return false;
		}
	    } else if (msg.messageType().equals(SERVICE_OPEN_FAILURE)) {
		logger.warn("Error: Service failed to open");
		return false;
	    }
	}
	return true;
    }

    private boolean processResponseEvent(Event event, Session session) throws Exception {
	logger.info("Processing {}", event.eventType().toString());
	MessageIterator msgIter = event.messageIterator();
	while (msgIter.hasNext()) {
	    Message msg = msgIter.next();
	    logger.debug("MESSAGE: {}", msg.toString());
	    logger.info("CORRELATION ID: {}", msg.correlationID());
	    if (event.eventType() == Event.EventType.RESPONSE &&  requestIds.contains(msg.correlationID())) {
		logger.info("Message Type = {}", msg.messageType());
		if (msg.messageType().equals(ERROR_INFO)) {
		    Integer errorCode = msg.getElementAsInt32("ERROR_CODE");
		    String errorMessage = msg.getElementAsString("ERROR_MESSAGE");
		    logger.warn("ERROR CODE: {} \tERROR MESSAGE: {}", errorCode, errorMessage);
		} else if (msg.messageType().equals(CREATE_ORDER)) {
		    Integer emsx_sequence = msg.getElementAsInt32("EMSX_SEQUENCE");
		    String message = msg.getElementAsString("MESSAGE");
		    logger.warn("EMSX_SEQUENCE: {} \tMESSAGE: {}", emsx_sequence, message);
		}
		session.stop();
	    }
	}
	return true;
    }

    private boolean processMiscEvents(Event event, Session session) throws Exception {
	logger.info("Processing {}", event.eventType().toString());
	MessageIterator msgIter = event.messageIterator();
	while (msgIter.hasNext()) {
	    Message msg = msgIter.next();
	    logger.debug("IGNORING MESSAGE = {}", msg);
	}
	return true;
    }
}
