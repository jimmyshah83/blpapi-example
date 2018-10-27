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
import com.bloomberglp.blpapi.Request;
import com.bloomberglp.blpapi.Service;
import com.bloomberglp.blpapi.Session;
import com.quant.backtest.multi.strategy.executor.BloombergExecutor;
import com.quant.backtest.multi.strategy.models.DailyTransaction;

import static com.quant.backtest.multi.strategy.enums.BloombergOrder.*;

@Component(value = "securityData")
public class BloombergFetchSecurityData extends BloombergExecutor {
    
    private static final Logger logger = LoggerFactory.getLogger(BloombergFetchSecurityData.class);

    @Value("${refdata.service.name}")
    private String serviceName;

    public int calculateQuantity(Session session, DailyTransaction dailyTransaction) throws Exception {
	int quantity = 1;
	Service refDataSvc = session.getService("//blp/refdata");
	if (refDataSvc == null) {
	    logger.error("Invalud Service");
	    return quantity;
	}
	Request request = refDataSvc.createRequest("ReferenceDataRequest");
	request.append(Security.getValue(), dailyTransaction.getTicker());
	request.append(Fields.getValue(), LastPrice.getValue());
	session.sendRequest(request, new CorrelationID());
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
	MessageIterator msgIter = event.messageIterator();
	while (msgIter.hasNext()) {
	    Message message = msgIter.next();
	    logger.debug("MESSAGE: {}", message.toString());
	    logger.info("CORRELATION ID: {}", message.correlationID());
	    Element elmSecurityDataArray = message.getElement("securityData");
	    for (int valueIndex = 0; valueIndex < elmSecurityDataArray.numValues(); valueIndex++) {
		Element elmSecurityData = elmSecurityDataArray.getValueAsElement(valueIndex);
		String security = elmSecurityData.getElementAsString("security");
		boolean hasFieldErrors = elmSecurityData.hasElement("fieldExceptions", true);
		if (hasFieldErrors) {
		    Element elmFieldErrors = elmSecurityData.getElement("fieldExceptions");
		    for (int errorIndex = 0; errorIndex < elmFieldErrors.numValues(); errorIndex++) {
			Element fieldError = elmFieldErrors.getValueAsElement(errorIndex);
			String fieldId = fieldError.getElementAsString("fieldId");

			Element errorInfo = fieldError.getElement("errorInfo");
			String source = errorInfo.getElementAsString("source");
			int code = errorInfo.getElementAsInt32("code");
			String category = errorInfo.getElementAsString("category");
			String strMessage = errorInfo.getElementAsString("message");
			String subCategory = errorInfo.getElementAsString("subcategory");

			System.err.println();
			System.err.println();
			System.err.println("\tfield error: " + security);
			System.err.println(String.format("\tfieldId = %s", fieldId));
			System.err.println(String.format("\tsource = %s", source));
			System.err.println(String.format("\tcode = %s", code));
			System.err.println(String.format("\tcategory = %s", category));
			System.err.println(String.format("\terrorMessage = %s", strMessage));
			System.err.println(String.format("\tsubCategory = %s", subCategory));
		    }
		} 
		boolean isSecurityError = elmSecurityData.hasElement("securityError", true);
		if (isSecurityError) {
		    Element secError = elmSecurityData.getElement("securityError");
		    String source = secError.getElementAsString("source");
		    int code = secError.getElementAsInt32("code");
		    String category = secError.getElementAsString("category");
		    String errorMessage = secError.getElementAsString("message");
		    String subCategory = secError.getElementAsString("subcategory");

		    System.err.println("security error");
		    System.err.println(String.format("source = %s", source));
		    System.err.println(String.format("code = %s", code));
		    System.err.println(String.format("category = %s", category));
		    System.err.println(String.format("errorMessage = %s", errorMessage));
		    System.err.println(String.format("subCategory = %s", subCategory));
		} else {
		    Element elmFieldData = elmSecurityData.getElement("fieldData");
		    Double quantity = dailyTransaction.getValue().doubleValue() / elmFieldData.getElementAsFloat64("PX_LAST");
		    return quantity.intValue();
		}
	    }
	}
	return 1;
    }
}
