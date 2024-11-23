package ru.myx.ae3.i3.web.http;

import ru.myx.ae3.base.BaseMessage;
import ru.myx.ae3.base.BaseObject;
import ru.myx.ae3.binary.TransferSocket;
import ru.myx.ae3.flow.ObjectSource;
import ru.myx.ae3.flow.ObjectTarget;
import ru.myx.ae3.flow.SourceBuffered;
import ru.myx.ae3.help.Convert;
import ru.myx.ae3.produce.ObjectFactory;
import ru.myx.ae3.serve.ServeRequest;

/*
 * Created on 09.02.2005
 */
/** @author myx */
public final class FactoryHttpParser implements ObjectFactory<TransferSocket, BaseMessage> {

	private static final Class<?>[] TARGETS = {
			ServeRequest.class
	};

	private static final Class<?>[] SOURCES = {
			TransferSocket.class
	};

	private static final String[] VARIETY = {
			"http_parser", HttpProtocol.PNAME_HTTP
	};

	@Override
	public final boolean accepts(final String variant, final BaseObject attributes, final Class<?> source) {

		return true;
	}

	@Override
	public final ObjectTarget<TransferSocket> connect(final String variant, final BaseObject attributes, final Class<?> source, final ObjectTarget<BaseMessage> target) {

		final boolean ignoreTargetPort = Convert.MapEntry.toBoolean(attributes, "ignoreTargetPort", false);
		final boolean ignoreGzip = Convert.MapEntry.toBoolean(attributes, "ignoreGzip", false);
		final boolean ignoreKeepAlive = Convert.MapEntry.toBoolean(attributes, "ignoreKeepAlive", false);
		final boolean reverseProxied = Convert.MapEntry.toBoolean(attributes, "reverseProxied", false);
		final int ifModifiedSinceMode = Convert.MapEntry.toInt(attributes, "ifModifiedSince", FlowConfiguration.STRS_IMS, 1);
		return new TargetHttp(target, ignoreTargetPort, ignoreGzip, ignoreKeepAlive, reverseProxied, ifModifiedSinceMode);
	}

	@Override
	public final ObjectSource<BaseMessage> prepare(final String variant, final BaseObject attributes, final TransferSocket socket) {

		final SourceBuffered buffer = new SourceBuffered(false);
		final boolean ignoreTargetPort = Convert.MapEntry.toBoolean(attributes, "ignoreTargetPort", false);
		final boolean ignoreGzip = Convert.MapEntry.toBoolean(attributes, "ignoreGzip", false);
		final boolean ignoreKeepAlive = Convert.MapEntry.toBoolean(attributes, "ignoreKeepAlive", false);
		final boolean reverseProxied = Convert.MapEntry.toBoolean(attributes, "reverseProxied", false);
		final int ifModifiedSinceMode = Convert.MapEntry.toInt(attributes, "ifModifiedSince", FlowConfiguration.STRS_IMS, 1);
		final FlowConfiguration configuration = new FlowConfiguration(
				HttpProtocol.PNAME_HTTP,
				ignoreTargetPort,
				ignoreGzip,
				ignoreKeepAlive,
				reverseProxied,
				ifModifiedSinceMode,
				buffer.getFrontDoor());
		if (socket.getSource().connectTarget(HandlerQueue.getParser(socket, configuration))) {
			HttpStatusProvider.stConnections++;
			HttpStatusProvider.stConnectionsHttp++;
			return buffer;
		}
		return null;
	}

	@Override
	public final BaseMessage produce(final String variant, final BaseObject attributes, final TransferSocket socket) {

		final SourceBuffered buffer = new SourceBuffered(true);
		final boolean ignoreTargetPort = Convert.MapEntry.toBoolean(attributes, "ignoreTargetPort", false);
		final boolean ignoreGzip = Convert.MapEntry.toBoolean(attributes, "ignoreGzip", false);
		final boolean ignoreKeepAlive = Convert.MapEntry.toBoolean(attributes, "ignoreKeepAlive", false);
		final boolean reverseProxied = Convert.MapEntry.toBoolean(attributes, "reverseProxied", false);
		final int ifModifiedSinceMode = Convert.MapEntry.toInt(attributes, "ifModifiedSince", FlowConfiguration.STRS_IMS, 1);
		final FlowConfiguration configuration = new FlowConfiguration(
				HttpProtocol.PNAME_HTTP,
				ignoreTargetPort,
				ignoreGzip,
				ignoreKeepAlive,
				reverseProxied,
				ifModifiedSinceMode,
				buffer.getFrontDoor());
		if (socket.getSource().connectTarget(HandlerQueue.getParser(socket, configuration))) {
			HttpStatusProvider.stConnections++;
			HttpStatusProvider.stConnectionsHttp++;
			while (!buffer.isReady() && !buffer.isExhausted()) {
				try {
					Thread.sleep(50L);
				} catch (final InterruptedException e) {
					return null;
				}
			}
			return buffer.isExhausted()
				? null
				: buffer.next();
		}
		return null;
	}

	@Override
	public final Class<?>[] sources() {

		return FactoryHttpParser.SOURCES;
	}

	@Override
	public final Class<?>[] targets() {

		return FactoryHttpParser.TARGETS;
	}

	@Override
	public final String[] variety() {

		return FactoryHttpParser.VARIETY;
	}
}
