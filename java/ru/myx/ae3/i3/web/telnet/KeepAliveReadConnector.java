/*
 * Created on 19.04.2006
 */
package ru.myx.ae3.i3.web.telnet;

import java.util.function.Function;

final class KeepAliveReadConnector implements Function<TelnetSocketHandler, Object> {
	@Override
	public final Object apply(final TelnetSocketHandler parser) {
		parser.reconnect();
		return null;
	}
	
	@Override
	public final String toString() {
		return "KEEP-ALIVE RECONNECTOR";
	}
}
