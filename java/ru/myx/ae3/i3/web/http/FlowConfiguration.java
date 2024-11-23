/*
 * Created on 28.04.2006
 */
package ru.myx.ae3.i3.web.http;

import ru.myx.ae3.base.BaseMessage;
import ru.myx.ae3.flow.ObjectTarget;

final class FlowConfiguration {
	
	static final String[] STRS_IMS = new String[]{
			"ignore", //
			"exact", //
			"before", //
	};

	static final int IMS_MODE_IGNORE = 0;
	static final int IMS_MODE_EXACT = 1;
	static final int IMS_MODE_BEFORE = 2;
	
	final String protocolName;
	
	final boolean ignoreTargetPort;
	
	final boolean ignoreGzip;
	
	final boolean ignoreKeepAlive;
	
	final boolean reverseProxied;
	
	final int ifModifiedSinceMode;
	
	final ObjectTarget<BaseMessage> target;
	
	FlowConfiguration(
			final String protocolName,
			final boolean ignoreTargetPort,
			final boolean ignoreGzip,
			final boolean ignoreKeepAlive,
			final boolean reverseProxied,
			final int ifModifiedSinceMode,
			final ObjectTarget<BaseMessage> target) {

		this.protocolName = protocolName;
		this.ignoreTargetPort = ignoreTargetPort;
		this.ignoreGzip = ignoreGzip;
		this.ignoreKeepAlive = ignoreKeepAlive;
		this.reverseProxied = reverseProxied;
		this.ifModifiedSinceMode = ifModifiedSinceMode;
		this.target = target;
	}
}
