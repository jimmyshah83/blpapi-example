package com.quant.backtest.multi.strategy.executor;

import com.bloomberglp.blpapi.Session;
import com.bloomberglp.blpapi.SessionOptions;

public abstract class BloombergExecutor {

    protected Session createSession(String hostName, int hostPort) {
	SessionOptions sessionOptions = new SessionOptions();
	sessionOptions.setServerHost(hostName);
	sessionOptions.setServerPort(hostPort);
	return new Session(sessionOptions);
    }
}
