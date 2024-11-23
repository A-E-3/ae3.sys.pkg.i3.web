package ru.myx.ae3.i3.web.telnet;

import ru.myx.ae3.binary.TransferSocket;
import ru.myx.ae3.binary.TransferTarget;
import ru.myx.ae3.flow.ObjectTarget;
import ru.myx.ae3.report.Report;

/*
 * Created on 30.11.2005

 */
final class TelnetSocketTarget implements ObjectTarget<TransferSocket> {
	
	private static final String OWNER = "HTTP_TARGET";

	private final FlowConfiguration configuration;

	TelnetSocketTarget(final boolean ignoreTargetPort, final boolean ignoreGzip, final boolean ignoreKeepAlive, final boolean reverseProxied) {
		this.configuration = new FlowConfiguration(Telnet.PNAME_TELNET, ignoreTargetPort, ignoreGzip, ignoreKeepAlive, reverseProxied);
		if (Report.MODE_DEBUG) {
			Telnet.LOG.event(TelnetSocketTarget.OWNER, "INITIALIZING", "ignoreTargetPort=" + ignoreTargetPort);
		}
	}

	@Override
	public final boolean absorb(final TransferSocket socket) {
		
		final TransferTarget parser = TelnetHandlerQueue.getParser(socket, this.configuration);
		if (Report.MODE_DEBUG) {
			Telnet.LOG.event(TelnetSocketTarget.OWNER, "CONNECTING", "socket=" + socket.getIdentity() + ", parser=" + parser);
		}
		final boolean result = socket.getSource().connectTarget(parser);
		if (result) {
			TelnetStatusProvider.stConnections++;
		}
		return result;
	}

	@Override
	public final Class<? extends TransferSocket> accepts() {
		
		return TransferSocket.class;
	}

	@Override
	public final String toString() {
		
		return TelnetSocketTarget.OWNER;
	}
}
