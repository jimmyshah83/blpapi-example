package com.quant.backtest.multi.strategy.executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.bloomberglp.blpapi.CorrelationID;
import com.bloomberglp.blpapi.Element;
import com.bloomberglp.blpapi.Event;
import com.bloomberglp.blpapi.Message;
import com.bloomberglp.blpapi.MessageIterator;
import com.bloomberglp.blpapi.Name;
import com.bloomberglp.blpapi.Request;
import com.bloomberglp.blpapi.Service;
import com.bloomberglp.blpapi.Session;
import com.quant.backtest.multi.strategy.executor.BloombergExecutor;
import com.quant.backtest.multi.strategy.models.DailyTransaction;

import static com.quant.backtest.multi.strategy.enums.BloombergOrder.*;

@Component(value = "securityData")
public class BloombergFetchSecurityData extends BloombergExecutor {
    
    private static final Logger logger = LoggerFactory.getLogger(BloombergFetchSecurityData.class);

    private static final Name REF_DATA = new Name("ReferenceDataRequest");
    private Long CORRELATION_START_ID = 1000L;
    private final int DEFAULT_QTY = 1;
    
    @Value("${refdata.service.name}")
    private String serviceName;

    public int calculateQuantity(Session session, DailyTransaction dailyTransaction) throws Exception {
	int quantity = DEFAULT_QTY;
	Service refDataSvc = session.getService(serviceName);
	if (refDataSvc == null) {
	    logger.error("Invalid Service. Returning default Quantity {}", quantity);
	    return quantity;
	}
	CorrelationID correlationID = new CorrelationID(CORRELATION_START_ID++);
	Request request = refDataSvc.createRequest(REF_DATA.toString());
	request.append(Security.getValue(), dailyTransaction.getTicker());
	request.append(Fields.getValue(), LastPrice.getValue());
	logger.info("Sending REF DATA request with correlation ID = {}", correlationID);
	session.sendRequest(request, correlationID);
	boolean continueLoop = true;
	while (continueLoop) {
	    Event event = session.nextEvent();
	    switch (event.eventType().intValue()) {
	    case Event.EventType.Constants.PARTIAL_RESPONSE:
	    case Event.EventType.Constants.RESPONSE:
		quantity = processResponse(event, dailyTransaction);
		continueLoop = false;
		break;
	    default:
		processMiscEvents(event, session);
	    }
	}
	return quantity;
    }
    
    private int processResponse(Event event, DailyTransaction dailyTransaction) throws Exception {
	MessageIterator messageIterator = event.messageIterator();
	while (messageIterator.hasNext()) {
	    Message message = messageIterator.next();
	    logger.info("MESSAGE = {} for CORRELATION ID = {}", message.toString(), message.correlationID());
	    Element elmSecurityDataArray = message.getElement("securityData");
	    for (int valueIndex = 0; valueIndex < elmSecurityDataArray.numValues(); valueIndex++) {
		Element elmSecurityData = elmSecurityDataArray.getValueAsElement(valueIndex);
		String security = elmSecurityData.getElementAsString("security");
		boolean hasFieldErrors = elmSecurityData.hasElement("fieldExceptions", true);
		if (hasFieldErrors) {
		    Element elmFieldErrors = elmSecurityData.getElement("fieldExceptions");
		    for (int errorIndex = 0; errorIndex < elmFieldErrors.numValues(); errorIndex++) {
			Element fieldError = elmFieldErrors.getValueAsElement(errorIndex);
			Element errorInfo = fieldError.getElement("errorInfo");
			int code = errorInfo.getElementAsInt32("code");
			String strMessage = errorInfo.getElementAsString("message");
			String subCategory = errorInfo.getElementAsString("subcategory");
			logger.error("REF DATA request has Field Exception for security {} with error code = {}, category = {} and error message ={}", security, code, subCategory, strMessage);
		    }
		} 
		boolean isSecurityError = elmSecurityData.hasElement("securityError", true);
		if (isSecurityError) {
		    Element secError = elmSecurityData.getElement("securityError");
		    int code = secError.getElementAsInt32("code");
		    String errorMessage = secError.getElementAsString("message");
		    String subCategory = secError.getElementAsString("subcategory");
		    logger.error("REF DATA request has Security Error for security {} with error code = {}, category = {} and error message ={}", security, code, subCategory, errorMessage);
		} else {
		    Element elmFieldData = elmSecurityData.getElement("fieldData");
		    double lastPrice = elmFieldData.getElementAsFloat64("PX_LAST");
		    Double quantity = dailyTransaction.getValue().doubleValue() / lastPrice;
		    logger.info("Fetched Last Price = {} for Security {}. Returning Quantity = {}", lastPrice, security, quantity);
		    return quantity.intValue();
		}
	    }
	}
	return DEFAULT_QTY;
    }
}
