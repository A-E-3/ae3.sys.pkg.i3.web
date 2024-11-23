package ru.myx.ae3.i3.web.http.client;

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
public final class HttpClientStatusProvider implements StatusProvider {
	static int	stConnections		= 0;
	
	static int	stConnectionsHttp	= 0;
	
	static int	stConnectionsHttps	= 0;
	
	/**
	 * @return
	 */
	public static final int getStatsConnections() {
		return HttpClientStatusProvider.stConnections;
	}
	
	/**
	 * @return
	 */
	public static final int getStatsConnectionsHttp() {
		return HttpClientStatusProvider.stConnectionsHttp;
	}
	
	/**
	 * @return
	 */
	public static final int getStatsConnectionsHttps() {
		return HttpClientStatusProvider.stConnectionsHttps;
	}
	
	/**
	 * @return
	 */
	public static final int getStatsRequests() {
		return ReplyParser.stRequests;
	}
	
	/**
	 * @return
	 */
	public static final int getStatsRequestsFinished() {
		return ReplyParser.stRequestsFinished;
	}
	
	/**
	 * for http.js
	 */
	public static final void incrementConnectionsHttp() {
		++HttpClientStatusProvider.stConnectionsHttp;
		++HttpClientStatusProvider.stConnections;
	}
	
	/**
	 * for http.js
	 */
	public static final void incrementConnectionsHttps() {
		++HttpClientStatusProvider.stConnectionsHttps;
		++HttpClientStatusProvider.stConnections;
	}
	
	@Override
	public String statusDescription() {
		return "HTTP Client";
	}
	
	@Override
	public void statusFill(final StatusInfo data) {
		final int stConnections = HttpClientStatusProvider.stConnections;
		final int stConnectionsHttp = HttpClientStatusProvider.stConnectionsHttp;
		final int stConnectionsHttps = HttpClientStatusProvider.stConnectionsHttps;
		final int stRequests = ReplyParser.stRequests;
		final int stRequestsFinished = ReplyParser.stRequestsFinished;
		final int stBadRequests = ReplyParser.stBadRequests;
		final int stGzipped = ReplyParser.stGzipped;
		final int stChunked = ReplyParser.stChunked;
		data.put( "Connections", Format.Compact.toDecimal( stConnections ) );
		data.put( "Connections HTTP", Format.Compact.toDecimal( stConnectionsHttp ) );
		data.put( "Connections HTTPS", Format.Compact.toDecimal( stConnectionsHttps ) );
		data.put( "Requests", Format.Compact.toDecimal( stRequests ) );
		data.put( "RequestsFinished", Format.Compact.toDecimal( stRequestsFinished ) );
		data.put( "Rq. per connection", Format.Compact.toDecimal( stRequests * 1.0 / stConnections ) );
		data.put( "Bad requests", Format.Compact.toDecimal( stBadRequests ) );
		data.put( "Gzipped responses", Format.Compact.toDecimal( stGzipped ) );
		data.put( "Chunked responses", Format.Compact.toDecimal( stChunked ) );
		data.put( "Reader initial buffer", Format.Compact.toBytes( ReplyParser.QBUFF_INIT ) );
		data.put( "Reader buffer step", Format.Compact.toBytes( ReplyParser.QBUFF_STEP ) );
		data.put( "Reader buffer expands", Format.Compact.toDecimal( ReplyParser.stExpands ) );
	}
	
	@Override
	public String statusName() {
		return "http-client";
	}
}
