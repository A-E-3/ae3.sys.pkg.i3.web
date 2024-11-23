package ru.myx.ae3.i3.web.http;

import ru.myx.ae3.Engine;
import ru.myx.ae3.help.Format;
import ru.myx.ae3.status.StatusInfo;
import ru.myx.ae3.status.StatusProvider;

/*
 * Created on 20.12.2005
 */
/** @author myx */
public final class HttpStatusProvider implements StatusProvider {
	
	static int stConnections = 0;

	static int stConnectionsHttp = 0;

	static int stConnectionsHttps = 0;

	/** @return */
	public static final int getStatsConnections() {
		
		return HttpStatusProvider.stConnections;
	}

	/** @return */
	public static final int getStatsConnectionsHttp() {
		
		return HttpStatusProvider.stConnectionsHttp;
	}

	/** @return */
	public static final int getStatsConnectionsHttps() {
		
		return HttpStatusProvider.stConnectionsHttps;
	}

	/** @return */
	public static final int getStatsRequests() {
		
		return SocketHandler.stRequests;
	}

	/** @return */
	public static final int getStatsRequestsHttp() {
		
		return SocketHandler.stRequestsHttp;
	}

	/** @return */
	public static final int getStatsRequestsHttps() {
		
		return SocketHandler.stRequestsHttps;
	}

	@Override
	public String statusDescription() {
		
		return "HTTP Server";
	}

	@Override
	public void statusFill(final StatusInfo data) {
		
		final int stConnections = HttpStatusProvider.stConnections;
		final int stConnectionsHttp = HttpStatusProvider.stConnectionsHttp;
		final int stConnectionsHttps = HttpStatusProvider.stConnectionsHttps;
		final int stRequests = SocketHandler.stRequests;
		final int stRequestsHttp = SocketHandler.stRequestsHttp;
		final int stRequestsHttps = SocketHandler.stRequestsHttps;
		final int stBadRequests = SocketHandler.stBadRequests;
		final int stGzipped = SocketHandler.stGzipped;
		final int stChunked = SocketHandler.stChunked;
		final int stHttp10 = SocketHandler.stHttp10;
		final int stHttp11 = SocketHandler.stHttp11;
		final int stInlineParserCreations = HandlerQueue.stsInlineParserCreations;
		final int stUnexpectedFinalizations = SocketHandler.stUnexpectedFinalizations;
		final long started = HttpProtocol.STARTED;
		final long tt = Engine.fastTime() - started;
		final long tm = tt / 1000;
		data.put("Connections", Format.Compact.toDecimal(stConnections));
		data.put("Connections HTTP", Format.Compact.toDecimal(stConnectionsHttp));
		data.put("Connections HTTPS", Format.Compact.toDecimal(stConnectionsHttps));
		data.put("Conn. per second", Format.Compact.toDecimal(stConnections * 1.0 / tm));
		data.put("Conn. per second HTTP", Format.Compact.toDecimal(stConnectionsHttp * 1.0 / tm));
		data.put("Conn. per second HTTPS", Format.Compact.toDecimal(stConnectionsHttps * 1.0 / tm));
		data.put("Requests", Format.Compact.toDecimal(stRequests));
		data.put("Requests HTTP", Format.Compact.toDecimal(stRequestsHttp));
		data.put("Requests HTTPS", Format.Compact.toDecimal(stRequestsHttps));
		data.put("Rq. per second", Format.Compact.toDecimal(stRequests * 1.0 / tm));
		data.put("Rq. per second HTTP", Format.Compact.toDecimal(stRequestsHttp * 1.0 / tm));
		data.put("Rq. per second HTTPS", Format.Compact.toDecimal(stRequestsHttps * 1.0 / tm));
		data.put("Rq. per connection", Format.Compact.toDecimal(stRequests * 1.0 / stConnections));
		data.put(
				"Rq. per connection HTTP",
				stConnectionsHttp == 0
					? "N/A"
					: Format.Compact.toDecimal(stRequestsHttp * 1.0 / stConnectionsHttp));
		data.put(
				"Rq. per connection HTTPS",
				stConnectionsHttps == 0
					? "N/A"
					: Format.Compact.toDecimal(stRequestsHttps * 1.0 / stConnectionsHttps));
		data.put("Bad requests", Format.Compact.toDecimal(stBadRequests));
		data.put("Gzipped responses", Format.Compact.toDecimal(stGzipped));
		data.put("Chunked responses", Format.Compact.toDecimal(stChunked));
		data.put("HTTP/1.0 responses", Format.Compact.toDecimal(stHttp10));
		data.put("HTTP/1.1 responses", Format.Compact.toDecimal(stHttp11));
		data.put("Inline parser creations", Format.Compact.toDecimal(stInlineParserCreations));
		data.put("Unexpected finalizations", Format.Compact.toDecimal(stUnexpectedFinalizations));
		data.put("Reader initial buffer", Format.Compact.toBytes(SocketHandler.QBUFF_INIT));
		data.put("Reader buffer step", Format.Compact.toBytes(SocketHandler.QBUFF_STEP));
		data.put("Reader buffer expands", Format.Compact.toDecimal(SocketHandler.stExpands));
		data.put("Serving time", Format.Compact.toPeriod(tt));
	}

	@Override
	public String statusName() {
		
		return "http";
	}
}
