package ru.myx.ae3.i3.web;

import java.lang.ref.ReferenceQueue;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import ru.myx.ae3.Engine;
import ru.myx.ae3.answer.AbstractReplyException;
import ru.myx.ae3.answer.Reply;
import ru.myx.ae3.answer.ReplyAnswer;
import ru.myx.ae3.base.Base;
import ru.myx.ae3.base.BaseObject;
import ru.myx.ae3.binary.TransferCopier;
import ru.myx.ae3.eval.Evaluate;
import ru.myx.ae3.exec.Exec;
import ru.myx.ae3.exec.ExecProcess;
import ru.myx.ae3.flow.ObjectTarget;
import ru.myx.ae3.help.Format;
import ru.myx.ae3.l2.TargetContextAbstract;
import ru.myx.ae3.reflect.ReflectionExplicit;
import ru.myx.ae3.reflect.ReflectionHidden;
import ru.myx.ae3.reflect.ReflectionManual;
import ru.myx.ae3.report.Report;
import ru.myx.ae3.report.ReportReceiver;
import ru.myx.ae3.serve.Serve;
import ru.myx.ae3.serve.ServeRequest;
import ru.myx.ae3.vfs.Entry;
import ru.myx.ae3.vfs.EntryContainer;
import ru.myx.ae3.vfs.Storage;
import ru.myx.ae3.vfs.TreeLinkType;

/** @author myx
 *
 *         TODO: ObjectTarget interface is not really used? */
@ReflectionManual
public class WebInterface implements ObjectTarget<ServeRequest> {
	
	static final ExecProcess CTX = Exec.createProcess(Exec.getRootProcess(), "Web Interface");
	
	static final ReportReceiver LOG = Report.createReceiver("ae3.i3.web");
	
	private static final ReferenceQueue<WebTarget> queue = new ReferenceQueue<>();
	
	private static final Map<String, RuntimeTarget> cache = new HashMap<>();
	
	private static final Map<String, BaseObject> LOCAL_NAMES = new ConcurrentHashMap<>();
	
	/**
	 *
	 */
	@ReflectionExplicit
	public static final EntryContainer WEB_SITES;
	
	/**
	 *
	 */
	@ReflectionExplicit
	public static final WebTarget TARGET_UNKNOWN;
	
	private static boolean started = false;
	
	private static boolean stopped = false;
	
	private static final Object LOCK = new Object();
	
	private static boolean running = false;
	
	static {
		WEB_SITES = Storage.PROTECTED.relative("web", TreeLinkType.PUBLIC_TREE_REFERENCE).toContainer();
		
		TARGET_UNKNOWN = WebTargetUnknown.INSTANCE;
		
		WebInterface.localNameUpsert(Engine.HOST_NAME, BaseObject.createObject(null));
		WebInterface.localNameUpsert("localhost", BaseObject.createObject(null));
	}
	
	private static final void localNameInvalidate(final String name) {
		
		final int length = name.length();
		synchronized (WebInterface.cache) {
			for (final RuntimeTarget target : WebInterface.cache.values()) {
				final String tname = target.name;
				if (tname.endsWith(name)) {
					final int tlength = tname.length();
					if (tlength == length || tname.charAt(tlength - length - 1) == '.') {
						target.reference.clear();
					}
				}
			}
		}
	}
	
	/** May have some deferred/parallel tasks, return early
	 *
	 * @param query
	 * @return true */
	@ReflectionExplicit
	public final static boolean dispatch(final ServeRequest query) {
		
		if (Report.MODE_DEVEL) {
			WebInterface.LOG.event("WEB-IFACE", "QUERY-DISPATCH", String.valueOf(query));
		}
		
		final WebTarget webTarget = WebInterface.dispatcherForQuery(query);
		(webTarget == null
			? WebTargetUnknown.INSTANCE
			: webTarget).onDispatch(query);
		return true;
	}
	
	/** Happens inline (not in parallel), return when ready, doesn't handle 404 - returns null
	 * instead.
	 *
	 * @param query
	 * @return */
	@ReflectionExplicit
	public final static WebTarget dispatcherForQuery(final ServeRequest query) {
		
		/** We require client to support 'host' query header. It must be explicitly specified even
		 * when accessing using IP address as host name. */
		final String target = query.getTarget();
		if (target == null) {
			return WebTargetUpgradeYourBrowser.INSTANCE;
		}
		if (WebInterface.stopped) {
			return WebTargetServiceStopped.INSTANCE;
		}
		/** POST form parameters are to be merged with URL parameters. */
		{
			Serve.checkParsePostParameters(query);
		}
		/**
		 *
		 */
		if (Report.MODE_DEBUG) {
			WebInterface.LOG.event("WEB-IFACE", "QUERY", Format.Describe.toEcmaSource(query, ""));
		}
		
		return WebInterface.dispatcherForTarget(target);
	}
	
	/** @param target
	 * @return */
	@ReflectionExplicit
	public final static WebTarget dispatcherForTarget(final String target) {
		
		{
			/** immediate */
			final RuntimeTarget runtimeTarget = WebInterface.cache.get(target);
			if (runtimeTarget != null) {
				final WebTarget webTarget = runtimeTarget.reference.get();
				if (webTarget != null) {
					return webTarget;
				}
			}
		}
		synchronized (WebInterface.cache) {
			/** re-check cache after entering synchronized lock */
			{
				final RuntimeTarget runtimeTarget = WebInterface.cache.get(target);
				if (runtimeTarget != null) {
					final WebTarget webTarget = runtimeTarget.reference.get();
					if (webTarget != null) {
						return webTarget;
					}
				}
			}
			/** create target */
			{
				final WebTarget webTarget = new WebTargetLoader(target);
				final RuntimeTarget runtimeTarget = new RuntimeTarget( //
						target, //
						webTarget, //
						null /** no cleaning, WebInterface.queue */ //
				);
				WebInterface.cache.put(target, runtimeTarget);
				return webTarget;
			}
		}
	}
	
	/** @param name
	 * @return */
	@ReflectionExplicit
	public static String localNameCheck(final String name) {
		
		String check = name;
		int pos = -1;
		for (;;) {
			if (WebInterface.LOCAL_NAMES.containsKey(check)) {
				return pos == -1
					? "local"
					: name.substring(0, pos) + ".local";
			}
			pos = name.indexOf('.', pos + 1);
			if (pos == -1) {
				return null;
			}
			check = name.substring(pos + 1);
		}
	}
	
	/** @param name
	 * @param key
	 * @return */
	@ReflectionExplicit
	public static boolean localNameRemove(final String name, final BaseObject key) {
		
		WebInterface.LOG.event("WEB-IFACE", "LNAME", "localNameRemove: " + name);
		synchronized (WebInterface.LOCAL_NAMES) {
			final BaseObject current = WebInterface.LOCAL_NAMES.get(name);
			if (current != null && current != key) {
				return false;
			}
			WebInterface.LOCAL_NAMES.remove(name);
		}
		WebInterface.localNameInvalidate(name);
		return true;
	}
	
	/** @param name
	 * @param key
	 * @return */
	@ReflectionExplicit
	public static boolean localNameUpsert(final String name, final BaseObject key) {
		
		if ("local".equals(name)) {
			WebInterface.LOG.event("WEB-IFACE", "LNAME", "localNameUpsert 'local' name is ignored");
			return false;
		}
		
		WebInterface.LOG.event("WEB-IFACE", "LNAME", "localNameUpsert: " + name);
		synchronized (WebInterface.LOCAL_NAMES) {
			final BaseObject current = WebInterface.LOCAL_NAMES.get(name);
			if (current != null && current != key) {
				return false;
			}
			WebInterface.LOCAL_NAMES.put(name, key);
		}
		WebInterface.localNameInvalidate(name);
		return true;
	}
	
	/** @param owner
	 * @param context
	 * @param result
	 * @return */
	@ReflectionExplicit
	public static ReplyAnswer replyFromObject(final String owner, final WebContext<?> context, final BaseObject result) {
		
		if (result == null) {
			return null;
		}
		if (result instanceof final Entry entry) {
			return Reply.entry(
					owner + ":ENTRY", //
					context.getQuery(),
					entry.toBinary(),
					entry.getKey() //
			)//
					.setContentDisposition("inline" /* alt: "file" */)//
			;
		}
		if (result instanceof final TransferCopier binary) {
			return Reply.binary(owner + ":BINARY", context.getQuery(), binary);
		}
		if (result instanceof final ReplyAnswer replyAnswer) {
			return replyAnswer;
		}
		if (result != BaseObject.UNDEFINED && result != BaseObject.NULL) {
			try {
				context.transform(result).baseValue();
			} catch (final AbstractReplyException r) {
				final ReplyAnswer reply = r.getReply();
				final int code = Base.getInt(result, "code", -1);
				if (code >= 400 && code < 600 && BaseObject.FALSE == result.baseGet("delay", null)) {
					reply.setAttribute("X-Delay", BaseObject.FALSE);
				}
				return code == -1 || reply.getCode() != 200
					? reply
					: reply.setCode(code);
			} catch (final Throwable t) {
				return Reply
						.string(
								owner + ":EXCEPTION",
								context.getQuery(),
								Format.Throwable.toText(t) + "\r\nWhile trying to display: " + Format.Describe.toEcmaSource(result, "") + "") //
						.setAttribute("Content-Type", "text/plain") //
						.setCode(Reply.CD_EXCEPTION);
			}
			{
				final ReplyAnswer reply = context.getResultReply();
				if (reply != null) {
					final int code = Base.getInt(result, "code", -1);
					if (code >= 400 && code < 600 && BaseObject.FALSE == result.baseGet("delay", null)) {
						reply.setAttribute("X-Delay", BaseObject.FALSE);
					}
					return code == -1 || reply.getCode() != 200
						? reply
						: reply.setCode(code);
				}
			}
			if (context instanceof final TargetContextAbstract<?> targetContextAbstract) {
				final BaseObject replacement = targetContextAbstract.getResultLayout();
				if (replacement != result) {
					final int newCode = Base.getInt(replacement, "code", -1);
					final int useCode = newCode == -1
						? Base.getInt(result, "code", -1)
						: newCode;
					final ReplyAnswer reply = Reply.object(owner + ":OBJECT", context.getQuery(), replacement) //
							.setAttribute("Content-Type", "text/plain");
					if (useCode >= 400 && useCode < 600 && BaseObject.FALSE == result.baseGet("delay", null)) {
						reply.setAttribute("X-Delay", BaseObject.FALSE);
					}
					return useCode == -1 || reply.getCode() != 200
						? reply
						: reply.setCode(useCode);
				}
			}
		}
		{
			final Object base = result.baseValue();
			if (base != null && base != result) {
				return WebInterface.replyFromObject(owner, context, base);
			}
		}
		return Reply.string(owner + ":UNKNOWN", context.getQuery(), String.valueOf(result)) //
				.setAttribute("Content-Type", "text/plain") //
				.setCode(Reply.CD_UNSUPPORTED_FORMAT);
	}
	
	/** @param owner
	 * @param context
	 * @param result
	 * @return */
	@ReflectionHidden
	public static ReplyAnswer replyFromObject(final String owner, final WebContext<?> context, final Object result) {
		
		if (result == null) {
			return null;
		}
		if (result instanceof Entry) {
			final Entry entry = (Entry) result;
			return Reply.entry(
					owner + ":ENTRY", //
					context.getQuery(),
					entry.toBinary(),
					entry.getKey() //
			)//
					.setContentDisposition("inline" /* alt: "file" */)//
			;
		}
		if (result instanceof TransferCopier) {
			final TransferCopier binary = (TransferCopier) result;
			return Reply.binary(owner + ":BINARY", context.getQuery(), binary);
		}
		if (result instanceof ReplyAnswer) {
			return (ReplyAnswer) result;
		}
		if (result != BaseObject.UNDEFINED && result != BaseObject.NULL) {
			final BaseObject baseResult = Base.forUnknown(result);
			try {
				context.transform(baseResult).baseValue();
			} catch (final AbstractReplyException r) {
				final ReplyAnswer reply = r.getReply();
				final int code = Base.getInt(baseResult, "code", -1);
				if (code >= 400 && code < 600 && BaseObject.FALSE == baseResult.baseGet("delay", null)) {
					reply.setAttribute("X-Delay", BaseObject.FALSE);
				}
				return code == -1 || reply.getCode() != 200
					? reply
					: reply.setCode(code);
			} catch (final Throwable t) {
				return Reply
						.string(
								owner + ":EXCEPTION",
								context.getQuery(),
								Format.Throwable.toText(t) + "\r\nWhile trying to display: " + Format.Describe.toEcmaSource(result, "") + "") //
						.setAttribute("Content-Type", "text/plain") //
						.setCode(Reply.CD_EXCEPTION);
			}
			{
				final ReplyAnswer reply = context.getResultReply();
				if (reply != null) {
					final int code = Base.getInt(baseResult, "code", -1);
					if (code >= 400 && code < 600 && BaseObject.FALSE == baseResult.baseGet("delay", null)) {
						reply.setAttribute("X-Delay", BaseObject.FALSE);
					}
					return code == -1 || reply.getCode() != 200
						? reply
						: reply.setCode(code);
				}
			}
			if (context instanceof TargetContextAbstract<?>) {
				final BaseObject replacement = ((TargetContextAbstract<?>) context).getResultLayout();
				if (replacement != result) {
					final int newCode = Base.getInt(replacement, "code", -1);
					final int useCode = newCode == -1
						? Base.getInt(baseResult, "code", -1)
						: newCode;
					final ReplyAnswer reply = Reply.object(owner + ":OBJECT", context.getQuery(), replacement) //
							.setAttribute("Content-Type", "text/plain");
					if (useCode >= 400 && useCode < 600 && BaseObject.FALSE == baseResult.baseGet("delay", null)) {
						reply.setAttribute("X-Delay", BaseObject.FALSE);
					}
					return useCode == -1 || reply.getCode() != 200
						? reply
						: reply.setCode(useCode);
				}
			}
		}
		return Reply.string(owner + ":UNKNOWN", context.getQuery(), String.valueOf(result)) //
				.setAttribute("Content-Type", "text/plain") //
				.setCode(Reply.CD_UNSUPPORTED_FORMAT);
	}
	
	/** @param query
	 * @param answer */
	@ReflectionExplicit
	public static void sendReply(final ServeRequest query, final ReplyAnswer answer) {
		
		assert query != null;
		assert answer != null;
		final Function<ReplyAnswer, Boolean> responseTarget;
		try {
			responseTarget = query.getResponseTarget();
		} catch (final Throwable e) {
			Report.exception(WebInterface.LOG, "WSM-QD", "Exception getting response target", e);
			return;
		}
		if (responseTarget == null) {
			WebInterface.LOG.event("WSM-QD", "ERROR", "Response target is NULL while sending reply");
			return;
		}
		try {
			responseTarget.apply(answer);
		} catch (final Throwable e) {
			Report.exception(WebInterface.LOG, "WSM-QD", "Exception while sending reply", e);
			return;
		}
	}
	
	/**
	 *
	 */
	@ReflectionExplicit
	public static final void startDefaultInterfaces() {
		
		synchronized (WebInterface.LOCK) {
			if (WebInterface.started) {
				return;
			}
			WebInterface.started = true;
		}
		WebInterface.LOG.event("WEB-IFACE", "START-DEFAULT", "Starting default interfaces...");
		
		if (!Evaluate.evaluateBoolean("require('ru.myx.ae3.internal/service/Service').start('web')", WebInterface.CTX, null)) {
			throw new RuntimeException("Interfaces failed to start!");
		}
		
		/** if (!Evaluate.evaluateBoolean(
		 * "require('ru.myx.ae3.internal/interfaces/Default').start()", WebInterface.CTX, null )) {
		 * throw new RuntimeException( "Interfaces failed to start!" ); } **/
		
		synchronized (WebInterface.LOCK) {
			WebInterface.running = true;
		}
		WebInterface.LOG.event("WEB-IFACE", "START-DEFAULT", "Started default interfaces");
	}
	
	/** @param timeout
	 * @return */
	@ReflectionExplicit
	public static final boolean waitMore(final long timeout) {
		
		synchronized (WebInterface.LOCK) {
			if (WebInterface.running) {
				try {
					WebInterface.LOCK.wait(timeout);
				} catch (final InterruptedException e) {
					return false;
				}
			}
			return WebInterface.running;
		}
	}
	
	static final void registerTarget(final String target, final WebTarget instance) {
		
		final RuntimeTarget runtimeTarget = new RuntimeTarget(//
				target,
				instance,
				null /** no cleaning, WebInterface.queue */ //
		);
		synchronized (WebInterface.cache) {
			WebInterface.cache.put(target, runtimeTarget);
		}
	}
	
	/** Needs to be accessible, used in real startup script */
	@ReflectionExplicit
	public WebInterface() {
		
		//
	}
	
	@Override
	public boolean absorb(final ServeRequest query) {
		
		if (Report.MODE_DEBUG) {
			WebInterface.LOG.event("WEB-IFACE", "QUERY-ABSORB", String.valueOf(query));
		}
		return WebInterface.dispatch(query);
	}
	
	@Override
	public Class<? extends ServeRequest> accepts() {
		
		return ServeRequest.class;
	}
	
	@Override
	public void close() {
		
		WebInterface.LOG.event("WEB-IFACE", "CLOSED", "Web Interface RequestHandler is closed.");
	}
	
	@Override
	public final String toString() {

		return "[public " + this.getClass().getSimpleName() + "]";
	}

	/** @return */
	@SuppressWarnings("static-method")
	protected final boolean stop() {
		
		synchronized (WebInterface.LOCK) {
			WebInterface.running = false;
			WebInterface.stopped = true;
			WebInterface.LOCK.notifyAll();
		}
		return true;
	}
}
