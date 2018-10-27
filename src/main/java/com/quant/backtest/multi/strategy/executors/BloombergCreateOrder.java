package com.quant.backtest.multi.strategy.executors;

import java.io.IOException;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
import com.quant.backtest.multi.strategy.cache.SimpleListBasedCache;
import com.quant.backtest.multi.strategy.enums.Tickers;
import com.quant.backtest.multi.strategy.executor.BloombergExecutor;
import com.quant.backtest.multi.strategy.models.DailyTransaction;

import static com.quant.backtest.multi.strategy.enums.BloombergOrder.*;

/**
 * Creates a session with Bloomberg and places the order daily. It uses the
 * desktop API, BLPAPI, hence the user needs to be logged in to Bloomberg at all
 * times.
 * 
 * @author jiviteshshah
 */
@Component(value = "createOrder")
public class BloombergCreateOrder extends BloombergExecutor {

    private static final Logger logger = LoggerFactory.getLogger(BloombergCreateOrder.class);

    private static final Name ERROR_INFO = new Name("ErrorInfo");
    private static final Name CREATE_ORDER = new Name("CreateOrder");

    @Value("${service.host}")
    private String hostName;
    @Value("${service.port}")
    private int hostPort;
    @Value("${service.name}")
    private String serviceName;
    @Value("${bloomberg.order.type}")
    private String orderType;
    @Value("${bloomberg.order.tif}")
    private String tif;
    @Value("${bloomberg.order.handInstruction}")
    private String handInstruction;
    
    @Autowired
    private BloombergFetchSecurityData securityData;
    @Autowired
    private SimpleListBasedCache<DailyTransaction> listCache;

    /**
     * Creates a session with Bloomberg and places the order daily
     * 
     * @param dailyTransactions list of transactions
     * @throws IOException
     * @throws InterruptedException
     * @throws Exception
     */
    public void placeOrder() throws IOException, InterruptedException, Exception {
	logger.debug("Starting BLOOMBERG session");
	Session session = super.createSession(hostName, hostPort);
	if (session.start() && session.openService(serviceName)) {
	    Service service = session.getService(serviceName);
	    for (DailyTransaction dailyTransaction : listCache.fetchCache()) {
		if (StringUtils.equalsIgnoreCase(Tickers.CASH.name(), dailyTransaction.getTicker())) 
		    continue;
		logger.info("Processing Transaction {} ", dailyTransaction.toString());
		Request request = service.createRequest(CREATE_ORDER.toString());
		request.set(Ticker.getValue(), dailyTransaction.getTicker());
		request.set(Amount.getValue(), securityData.calculateQuantity(session, dailyTransaction));
		request.set(OrderType.getValue(), orderType);
		request.set(Tif.getValue(), tif);
		request.set(HandInstruction.getValue(), handInstruction);
		request.set(Side.getValue(), dailyTransaction.getSide().getName());
		session.sendRequest(request, new CorrelationID());
		boolean continueLoop = true;
		while (continueLoop) {
		    Event event = session.nextEvent();
		    switch (event.eventType().intValue()) {
		    case Event.EventType.Constants.PARTIAL_RESPONSE:
		    case Event.EventType.Constants.RESPONSE:
			processResponse(event);
			continueLoop = false;
			break;
		    default:
			processMiscEvents(event, session);
		    }
		}
	    }
	    logger.debug("BLOOMBERG SESSION completed.");
	    session.stop();
	}
    }

    private void processResponse(Event event) throws Exception {
	logger.debug("Processing RESPOINSE Event {}", event.eventType().toString());
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
		    logger.error("ERROR CODE: {} \tERROR MESSAGE: {}", errorCode, errorMessage);
		} else if (msg.messageType().equals(CREATE_ORDER)) {
		    Integer emsx_sequence = msg.getElementAsInt32("EMSX_SEQUENCE");
		    String message = msg.getElementAsString("MESSAGE");
		    logger.debug("EMSX_SEQUENCE: {} \tMESSAGE: {}", emsx_sequence, message);
		}
	    }
	}
    }
}
