/*
 * Created on 19.04.2006
 */
package ru.myx.ae3.i3.web.http;

import java.util.function.Function;

final class KeepAliveReadConnector implements Function<KeepAliveParserConnector, Object> {
	@Override
	public final Object apply(final KeepAliveParserConnector parser) {
		parser.reconnect();
		return null;
	}
	
	@Override
	public final String toString() {
		return "KEEP-ALIVE RECONNECTOR";
	}
}
