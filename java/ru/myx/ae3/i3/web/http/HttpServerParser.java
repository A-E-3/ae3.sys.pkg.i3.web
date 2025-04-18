package ru.myx.ae3.i3.web.http;

import ru.myx.ae3.base.Base;
import ru.myx.ae3.base.BaseFunction;
import ru.myx.ae3.base.BaseObject;
import ru.myx.ae3.binary.TransferSocket;
import ru.myx.ae3.binary.TransferTarget;
import ru.myx.ae3.exec.Exec;
import ru.myx.ae3.exec.ExecProcess;
import ru.myx.ae3.help.Convert;
import ru.myx.ae3.i3.web.http.CallbackLocal.FunctionQueryToCallback;

/** @author myx */
public class HttpServerParser {

	static final ExecProcess CTX = Exec.getRootProcess();
	private FunctionQueryToCallback callback;

	private final TransferTarget parser;
	private final TransferSocket socket;

	/** @param ctx
	 * @param socket
	 * @param callback
	 * @param https
	 * @param attributes */
	public HttpServerParser(final ExecProcess ctx, final TransferSocket socket, final BaseFunction callback, final boolean https, final BaseObject attributes) {

		this.socket = socket;
		this.callback = new CallbackLocal.FunctionQueryToCallback(ctx, callback);

		final boolean ignoreTargetPort = Convert.MapEntry.toBoolean(attributes, "ignoreTargetPort", false);
		final boolean ignoreGzip = Convert.MapEntry.toBoolean(attributes, "ignoreGzip", false);
		final boolean ignoreKeepAlive = Convert.MapEntry.toBoolean(attributes, "ignoreKeepAlive", false);
		final boolean reverseProxied = Convert.MapEntry.toBoolean(attributes, "reverseProxied", false);
		final int ifModifiedSinceMode = Convert.MapEntry.toInt(attributes, "ifModifiedSince", FlowConfiguration.STRS_IMS, 1);
		final FlowConfiguration configuration = new FlowConfiguration(
				Base.getString(
						attributes,
						"factory",
						https
							? HttpProtocol.PNAME_HTTPS
							: HttpProtocol.PNAME_HTTP),
				ignoreTargetPort,
				ignoreGzip,
				ignoreKeepAlive,
				reverseProxied,
				ifModifiedSinceMode,
				this.callback);

		final TransferTarget parser = this.parser = HandlerQueue.getParser(socket, configuration);

		if (socket.getSource().connectTarget(parser)) {
			HttpStatusProvider.stConnections++;
			HttpStatusProvider.stConnectionsHttp++;
			return;
		}

		HttpStatusProvider.stConnections++;
		HttpStatusProvider.stConnectionsHttp++;
	}

	@Override
	public String toString() {

		return "[HttpServerParser(socket=" + this.socket //
				+ ", parser=" + this.parser //
				+ " )]"//
		;
	}

}
