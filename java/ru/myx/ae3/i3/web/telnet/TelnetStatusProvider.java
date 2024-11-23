package ru.myx.ae3.i3.web.telnet;

import ru.myx.ae3.Engine;
import ru.myx.ae3.help.Format;
import ru.myx.ae3.status.StatusInfo;
import ru.myx.ae3.status.StatusProvider;

/*
 * Created on 20.12.2005
 */
/**
 * @author myx
 * 
 */
public final class TelnetStatusProvider implements StatusProvider {
	static int	stConnections		= 0;
	
	static int	stConnectionsTelnet	= 0;
	
	@Override
	public String statusDescription() {
		return "TELNET protocol";
	}
	
	@Override
	public void statusFill(final StatusInfo data) {
		final int stConnections = TelnetStatusProvider.stConnections;
		final int stConnectionsTelnet = TelnetStatusProvider.stConnectionsTelnet;
		final int stRequests = TelnetSocketHandler.stRequests;
		final int stBadRequests = TelnetSocketHandler.stBadRequests;
		final long started = Telnet.STARTED;
		final long tt = Engine.fastTime() - started;
		final long tm = tt / 1000;
		final int stInlineParserCreations = TelnetHandlerQueue.stsInlineParserCreations;
		final int stUnexpectedFinalizations = TelnetSocketHandler.stUnexpectedFinalizations;
		data.put( "Connections", Format.Compact.toDecimal( stConnections ) );
		data.put( "Connections TELNET", Format.Compact.toDecimal( stConnectionsTelnet ) );
		data.put( "Conn. per second", Format.Compact.toDecimal( stConnections * 1.0 / tm ) );
		data.put( "Conn. per second TELNET", Format.Compact.toDecimal( stConnectionsTelnet * 1.0 / tm ) );
		data.put( "Requests", Format.Compact.toDecimal( stRequests ) );
		data.put( "Bad requests", Format.Compact.toDecimal( stBadRequests ) );
		data.put( "Inline parser creations", Format.Compact.toDecimal( stInlineParserCreations ) );
		data.put( "Unexpected finalizations", Format.Compact.toDecimal( stUnexpectedFinalizations ) );
		data.put( "Reader initial buffer", Format.Compact.toBytes( TelnetSocketHandler.QBUFF_INIT ) );
		data.put( "Reader buffer step", Format.Compact.toBytes( TelnetSocketHandler.QBUFF_STEP ) );
		data.put( "Reader buffer expands", Format.Compact.toDecimal( TelnetSocketHandler.stExpands ) );
		data.put( "Serving time", Format.Compact.toPeriod( tt ) );
	}
	
	@Override
	public String statusName() {
		return "telnet";
	}
}
