package com.quant.backtest.multi.strategy.executors;

import java.util.ArrayList;
import java.util.List;

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
import com.bloomberglp.blpapi.SessionOptions;
import com.quant.backtest.multi.strategy.models.DailyTransaction;

@Component(value = "createOrder")
public class BBGCreateOrder {

    private static final Logger logger = LoggerFactory.getLogger(BBGCreateOrder.class);

    private static final Name ERROR_INFO = new Name("ErrorInfo");
    private static final Name CREATE_ORDER = new Name("CreateOrder");

    @Value("${service.host}")
    private String hostName;
    @Value("${service.port}")
    private int hostPort;
    @Value("${service.name}")
    private String serviceName;

    public void placeOrder(List<DailyTransaction> dailyTransactions) throws Exception {
	logger.debug("Starting bloomberg session");
	Session session = createSession();
	List<CorrelationID> correlationIDs = new ArrayList<>();
	if (session.start() && session.openService(serviceName)) {
	    Service service = session.getService(serviceName);
	    for (DailyTransaction dailyTransaction : dailyTransactions) {
		logger.info("Processing Transaction {} ", dailyTransaction.toString());
		Request request = service.createRequest("CreateOrder");
		request.set("EMSX_TICKER", "IBM US Equity");
		request.set("EMSX_AMOUNT", 1000);
		request.set("EMSX_ORDER_TYPE", "MKT");
		request.set("EMSX_TIF", "DAY");
		request.set("EMSX_HAND_INSTRUCTION", "ANY");
		request.set("EMSX_SIDE", "BUY");
		request.set("EMSX_BROKER", "????????");
		CorrelationID correlationID = new CorrelationID();
		correlationIDs.add(correlationID);
		session.sendRequest(request, correlationID);
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

	    }
	}
    }

    private Session createSession() {
	SessionOptions sessionOptions = new SessionOptions();
	sessionOptions.setServerHost(hostName);
	sessionOptions.setServerPort(hostPort);
	return new Session(sessionOptions);
    }

    private void processMiscEvents(Event event, Session session) throws Exception {
	logger.info("Processing MISC EVENT {}", event.eventType().toString());
	MessageIterator msgIter = event.messageIterator();
	while (msgIter.hasNext()) {
	    Message msg = msgIter.next();
	    logger.debug("IGNORING MESSAGE = {}", msg);
	}
    }

    private void processResponse(Event event, Session session) throws Exception {
	logger.info("Processing RESPOINSE EVENT {}", event.eventType().toString());
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
		} else if (msg.messageType().equals(CREATE_ORDER)) {
		    Integer emsx_sequence = msg.getElementAsInt32("EMSX_SEQUENCE");
		    String message = msg.getElementAsString("MESSAGE");
		    logger.warn("EMSX_SEQUENCE: {} \tMESSAGE: {}", emsx_sequence, message);
		}
		session.stop();
	    }
	}
    }
}
