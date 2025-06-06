/*
 * Created on 27.12.2005
 */
package ru.myx.ae3.i3.web.http;

import ru.myx.ae3.base.BaseMessage;
import ru.myx.ae3.binary.TransferSocket;
import ru.myx.ae3.binary.TransferTarget;
import ru.myx.ae3.flow.ObjectTarget;
import ru.myx.ae3.report.Report;

final class TargetHttps implements ObjectTarget<TransferSocket> {

	private static final String OWNER = "HTTPS_TARGET";

	private final FlowConfiguration configuration;

	TargetHttps(
			final ObjectTarget<BaseMessage> target,
			final boolean ignoreTargetPort,
			final boolean ignoreGzip,
			final boolean ignoreKeepAlive,
			final boolean reverseProxied,
			final int ifModifiedSinceMode) {

		this.configuration = new FlowConfiguration(//
				HttpProtocol.PNAME_HTTPS,
				ignoreTargetPort,
				ignoreGzip,
				ignoreKeepAlive,
				reverseProxied,
				ifModifiedSinceMode,
				target//
		);

		if (Report.MODE_DEBUG) {
			HttpProtocol.LOG.event(TargetHttps.OWNER, "INITIALIZING", "target=" + target + ", ignoreTargetPort=" + ignoreTargetPort);
		}
	}

	@Override
	public final boolean absorb(final TransferSocket socket) {

		final TransferTarget parser = HandlerQueue.getParser(socket, this.configuration);
		if (Report.MODE_DEBUG) {
			HttpProtocol.LOG.event(TargetHttps.OWNER, "CONNECTING", "Socket=" + socket.getIdentity() + ", Target=" + this.configuration.target + ", Parser=" + parser);
		}
		socket.getSource().connectTarget(parser);
		HttpStatusProvider.stConnections++;
		HttpStatusProvider.stConnectionsHttps++;
		return true;
	}

	@Override
	public final Class<? extends TransferSocket> accepts() {

		return TransferSocket.class;
	}

	@Override
	public final String toString() {

		return TargetHttps.OWNER;
	}
}
