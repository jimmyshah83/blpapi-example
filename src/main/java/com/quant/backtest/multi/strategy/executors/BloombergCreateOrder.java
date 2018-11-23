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
import com.quant.backtest.multi.strategy.cache.SimpleListCache;
import com.quant.backtest.multi.strategy.enums.Tickers;
import com.quant.backtest.multi.strategy.executor.utils.BloombergUtils;
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
    private Long CORRELATION_START_ID = 1L;
    
    @Value("${bloomberg.order.type}")
    private String orderType;
    @Value("${bloomberg.order.tif}")
    private String tif;
    @Value("${bloomberg.order.handInstruction}")
    private String handInstruction;
    @Value("${service.name}")
    private String serviceName;
    
    @Autowired
    private SimpleListCache<DailyTransaction> listCache;
    @Autowired
    private BloombergSession bloombergSession;
    @Autowired
    private BloombergUtils bbgUtils;

    /**
     * Creates a session with Bloomberg and places the order daily
     * 
     * @param dailyTransactions list of transactions
     * @throws IOException
     * @throws InterruptedException
     * @throws Exception
     */
    public void placeOrder() throws IOException, InterruptedException, Exception {
	logger.info("Starting BLOOMBERG session");
	Session session = bloombergSession.getSession();
	Service service = session.getService(serviceName);
	for (DailyTransaction dailyTransaction : listCache.fetchCache()) {
	    if (StringUtils.containsIgnoreCase(dailyTransaction.getTicker(), Tickers.CASH.name()))
		continue;
	    CorrelationID correlationID = new CorrelationID(CORRELATION_START_ID++);
	    logger.info("Processing Transaction {} with Request/Correlation ID {} ", dailyTransaction.toString(), correlationID);
	    Request request = service.createRequest(CREATE_ORDER.toString());
	    request.set(Ticker.getValue(), dailyTransaction.getTicker());
	    request.set(Amount.getValue(), bbgUtils.calculateQuantity(dailyTransaction));
	    request.set(OrderType.getValue(), orderType);
	    request.set(Tif.getValue(), tif);
	    request.set(HandInstruction.getValue(), handInstruction);
	    request.set(Side.getValue(), dailyTransaction.getSide().getName());
	    session.sendRequest(request, correlationID);
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
	logger.info("BLOOMBERG SESSION completed.");
	session.stop();
    }

    private void processResponse(Event event) throws Exception {
	logger.debug("Processing RESPONSE Event {}", event.eventType().toString());
	MessageIterator messagegIterator = event.messageIterator();
	while (messagegIterator.hasNext()) {
	    Message message = messagegIterator.next();
	    logger.info("MESSAGE = {} for CORRELATION ID = {}", message.toString(), message.correlationID());
	    if (event.eventType() == Event.EventType.RESPONSE) {
		logger.info("Message Type = {}", message.messageType());
		if (message.messageType().equals(ERROR_INFO)) {
		    Integer errorCode = message.getElementAsInt32("ERROR_CODE");
		    String errorMessage = message.getElementAsString("ERROR_MESSAGE");
		    logger.error("ERROR CODE: {} \tERROR MESSAGE: {}", errorCode, errorMessage);
		} else if (message.messageType().equals(CREATE_ORDER)) {
		    Integer emsx_sequence = message.getElementAsInt32("EMSX_SEQUENCE");
		    logger.info("EMSX_SEQUENCE: {} \tMESSAGE: {}", emsx_sequence, message.getElementAsString("MESSAGE"));
		}
	    }
	}
    }
}
