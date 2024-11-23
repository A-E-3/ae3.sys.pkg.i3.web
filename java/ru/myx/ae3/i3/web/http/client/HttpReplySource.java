package ru.myx.ae3.i3.web.http.client;

import ru.myx.ae3.answer.ReplyAnswer;
import ru.myx.ae3.base.BaseFunction;
import ru.myx.ae3.base.BaseMessage;
import ru.myx.ae3.base.BaseObject;
import ru.myx.ae3.binary.TransferCollector;
import ru.myx.ae3.binary.TransferCopier;
import ru.myx.ae3.binary.TransferSource;
import ru.myx.ae3.exec.Exec;
import ru.myx.ae3.exec.ExecProcess;
import ru.myx.ae3.exec.ResultHandler;
import ru.myx.ae3.reflect.Reflect;
import ru.myx.ae3.reflect.ReflectionExplicit;
import ru.myx.ae3.reflect.ReflectionManual;

@ReflectionManual
class HttpReplySource extends HttpReply<HttpReplySource> {

	private static final BaseObject PROTOTYPE = Reflect.classToBasePrototype(HttpReplySource.class);

	private ReplyParser parser;

	private final TransferCollector source;

	private ExecProcess callbackCtx = null;
	private BaseFunction callback = null;

	public HttpReplySource(
			final String owner,
			final ReplyParser parser,
			final BaseMessage query,
			final int code,
			final String headMessage,
			final String protoVersion,
			final BaseObject headers,
			final TransferCollector source) {

		super(owner, query, headMessage, protoVersion, code, headers);
		this.parser = parser;
		this.source = source;
	}

	@Override
	public void cancel() {

		final ReplyParser parser = this.parser;
		if (parser != null) {
			this.parser = null;
			parser.cancel();
		}
	}

	@Override
	public BaseObject basePrototype() {

		return HttpReplySource.PROTOTYPE;
	}

	@Override
	public TransferSource getObject() {

		return this.source;
	}

	@Override
	public Class<?> getObjectClass() {

		return TransferSource.class;
	}

	@Override
	public boolean isEmpty() {

		return false;
	}

	@Override
	public boolean isObject() {

		return true;
	}

	@Override
	public ReplyAnswer nextClone(final BaseMessage query) {

		throw new UnsupportedOperationException(this.getClass().getSimpleName() + ": cloning is not supported!");
	}

	/**
	 *
	 * Scripting friendly version
	 *
	 * Used in require('http') for example.
	 *
	 * @param ctx
	 *
	 * @param callbackOnReceived
	 */
	@ReflectionExplicit
	public void setOnReceived(final ExecProcess ctx, final BaseFunction callbackOnReceived) {

		if (this.callback != null) {
			throw new IllegalStateException("Callback is already set!");
		}
		if (this.parser == null) {

			final ReplyAnswer reply = new HttpReplyBinary(//
					"REPLY-SOURCE",
					this.query,
					this.code,
					this.headMessage,
					this.protoVersion,
					this.attributes,
					this.source.toBinary()//
			);

			final ExecProcess callbackCtx = Exec.createProcess(ReplyParser.CTX, ctx, "HttpClient.HttpReplySource, onReceived Callback Context");

			Exec.callAsyncForkUnrelated(//
					callbackCtx,
					"HttpClient.HttpReplySource.Reply: query: " + reply.getQuery(),
					callbackOnReceived,
					BaseObject.UNDEFINED,
					ResultHandler.FU_BNN_NXT,
					reply//
			);
		} else {
			this.parser.setCallback(ctx, callbackOnReceived);
		}
	}
	
	@ReflectionExplicit
	public void setOnDoneRead(final ExecProcess ctx, final BaseFunction callback) {
		
		// System.err.println(">>> >>> parser setCallback: callback: " +
		// callback);
		
		if (this.callback != null) {
			throw new IllegalStateException("Callback is already set!");
		}
		this.callbackCtx = Exec.createProcess(ReplyParser.CTX, ctx, "HttpClient.HttpReplySource, onDoneRead Callback Context");
		this.callback = callback;
	}
	
	void onDoneRead() {

		this.parser = null;
		final BaseFunction callback = this.callback;
		if (callback != null) {
			final ExecProcess callbackCtx = this.callbackCtx;
			this.callback = null;
			this.callbackCtx = null;

			final TransferCopier binary = this.source.toBinary();
			final HttpReply<?> reply = binary.length() == 0
				? new HttpReplyEmpty(//
						"REPLY-SOURCE",
						this.query,
						this.code,
						this.headMessage,
						this.protoVersion,
						this.attributes)
				: new HttpReplyBinary(//
						"REPLY-SOURCE",
						this.query,
						this.code,
						this.headMessage,
						this.protoVersion,
						this.attributes,
						binary);

			Exec.callAsyncForkUnrelated(//
					callbackCtx,
					"HttpClient.HttpReplySource.Reply: query: " + reply.getQuery(),
					callback,
					BaseObject.UNDEFINED,
					ResultHandler.FU_BNN_NXT,
					reply//
			);
		} else {
			this.source.close();
		}
	}
}
