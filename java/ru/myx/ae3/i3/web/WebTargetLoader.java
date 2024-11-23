package ru.myx.ae3.i3.web;

import java.util.function.Function;

import ru.myx.ae3.Engine;
import ru.myx.ae3.act.Act;
import ru.myx.ae3.answer.ReplyAnswer;
import ru.myx.ae3.base.Base;
import ru.myx.ae3.base.BaseObject;
import ru.myx.ae3.base.BasePrimitiveString;
import ru.myx.ae3.common.WaitTimeoutException;
import ru.myx.ae3.eval.Evaluate;
import ru.myx.ae3.exec.Exec;
import ru.myx.ae3.exec.ExecArgumentsEmpty;
import ru.myx.ae3.exec.ExecProcess;
import ru.myx.ae3.exec.ProgramAssembly;
import ru.myx.ae3.exec.ProgramPart;
import ru.myx.ae3.exec.ResultHandler;
import ru.myx.ae3.help.Format;
import ru.myx.ae3.report.Report;
import ru.myx.ae3.serve.ServeRequest;
import ru.myx.ae3.vfs.Entry;
import ru.myx.util.QueueStackRecord;

final class WebTargetLoader //
		extends
			QueueStackRecord<ServeRequest> //
		implements
			WebTarget,
			Function<ExecProcess, Void> //
{

	private static final ExecProcess CTX;

	static final BaseObject HOST_ROUTER;

	static final ProgramPart HOST_ROUTER_MATCH;

	static final BaseObject UNDEFINED_HANDLER;

	private static final BasePrimitiveString STR_TARGET_VAR = Base.forString("target");

	static {
		CTX = Exec.createProcess(WebInterface.CTX, "Web Target Loader");

		HOST_ROUTER = Evaluate.evaluateObject(
				"require('ru.myx.ae3.internal/web/HostRouter')", //
				WebTargetLoader.CTX,
				null);

		UNDEFINED_HANDLER = Evaluate.evaluateObject(
				"require('ru.acmcms.handlers/UndefinedHandler')", //
				WebTargetLoader.CTX,
				null);

		final ProgramAssembly assembly = new ProgramAssembly(WebTargetLoader.CTX);
		// this is HOST_ROUTER
		Evaluate.compileExpression(assembly, "this.getHandlerForHost(target)", ResultHandler.FA_BNN_NXT);
		HOST_ROUTER_MATCH = assembly.toProgram(0);
	}

	public static BaseObject resolveHostRouterTarget(final ExecProcess ctx, final BasePrimitiveString target) {

		ctx.vmFrameEntryExCall(//
				true,
				WebTargetLoader.HOST_ROUTER,
				WebTargetLoader.HOST_ROUTER_MATCH,
				ExecArgumentsEmpty.INSTANCE,
				ResultHandler.FA_BNN_NXT//
		);
		
		ctx.vmScopeDeriveContext(WebTargetLoader.CTX);
		ctx.contextCreateMutableBinding(WebTargetLoader.STR_TARGET_VAR, target, false);
		
		return WebTargetLoader.HOST_ROUTER_MATCH.execCallPreparedInilne(ctx);
	}

	private WebTarget result;

	private final BasePrimitiveString target;

	WebTargetLoader(final String targetId) {

		this.target = Base.forString(targetId);
		this.result = null;
		final ExecProcess ctx = Exec.createProcess(WebTargetLoader.CTX, "Target: " + targetId);
		Act.launch(ctx, this, ctx);
	}

	@Override
	public Void apply(final ExecProcess ctx) {

		WebInterface.LOG.event("WEB-IFACE", "LOADER", "Target: " + this + ", initializing...");
		{
			final Entry targetEntry = WebInterface.WEB_SITES.getContentElement(//
					this.target.baseToJavaString(), //
					null//
			).baseValue();

			if (targetEntry != null) {
				assert false : "Should be 'done', this=" + this + ", targetEntry=" + targetEntry;
			}
		}
		if (this.result == null) {
			try {
				BasePrimitiveString target = this.target;
				if (Report.MODE_DEBUG) {
					WebInterface.LOG.event("WEB-IFACE", "LOADER", "Target: " + this);
				}
				search : for (;;) {
					
					final BaseObject handler = WebTargetLoader.resolveHostRouterTarget(ctx, target);

					final Object baseHandler = handler.baseValue();
					if (baseHandler instanceof WebTarget) {
						WebInterface.LOG.event("WEB-IFACE", "LOADER", "Target: " + this + ", handler found: " + target + ", " + baseHandler);
						this.result = ((WebTarget) baseHandler).getWaitRealTarget();
						break search;
					}

					{
						final WebTarget shareTarget = WebTargetShareObject.probeTarget(target, handler);
						if (shareTarget != null) {
							WebInterface.LOG.event("WEB-IFACE", "LOADER", "Target: " + this + ", creating share: " + target);
							this.result = shareTarget;
							break search;
						}
					}

					{
						final String targetReplacement = WebInterface.localNameCheck(target.baseToJavaString());
						if (targetReplacement != null) {
							target = Base.forString(targetReplacement);
							WebInterface.LOG.event("WEB-IFACE", "LOADER", "Target: " + this + ", localName matched: " + target);
							continue search;
						}
					}
					{
						WebInterface.LOG.event("WEB-IFACE", "LOADER", "Target: " + this + " - UNKNOWN");
						this.result = WebTargetUnknown.INSTANCE;
						break search;
					}
				}
			} catch (final Throwable t) {
				WebInterface.LOG.event("WEB-IFACE", "LOADER", "Target: " + this + " - ERROR: " + Format.Throwable.toText(t));
				this.result = new WebTargetError(t);
			}
		}
		WebInterface.LOG.event("WEB-IFACE", "LOADER", "Target: " + this + ", registering result: " + this.result);
		WebInterface.registerTarget(this.target.baseToJavaString(), this.result);
		{
			int count = 0;
			for (;; ++count) {
				final ServeRequest query = this.next();
				if (query == null) {
					break;
				}
				this.result.onDispatch(query);
			}
			synchronized (this) {
				this.notifyAll();
			}
			WebInterface.LOG.event("WEB-IFACE", "LOADER", "Target: " + this + ", done, " + count + " pending task(s) dispatched");
		}
		return null;
	}

	@Override
	public String getTargetId() {

		return this.target.baseToJavaString();
	}

	@Override
	public WebTarget getWaitRealTarget() {

		WebTarget result;

		result = this.result;
		if (result != null) {
			assert result != this : "Circullar reference, this=" + this;
			return result.getWaitRealTarget();
		}

		sync : {
			try {
				synchronized (this) {

					/** re-check after sync */
					result = this.result;
					if (result != null) {
						assert result != this : "Circullar reference, this=" + this;
						break sync;
					}
					/** I AM NOT PARANOID.
					 *
					 *
					 * Taken from Javadoc for 'wait' method:
					 *
					 * A thread can also wake up without being notified, interrupted, or timing out,
					 * a so-called spurious wakeup. While this will rarely occur in practice,
					 * applications must guard against it by testing for the condition that should
					 * have caused the thread to be awakened, and continuing to wait if the
					 * condition is not satisfied. In other words, waits should always occur in
					 * loops, like this one:
					 *
					 * synchronized (obj) { while (<condition does not hold>) obj.wait(timeout); ...
					 * // Perform action appropriate to condition }
					 *
					 * (For more information on this topic, see Section 3.2.3 in Doug Lea's
					 * "Concurrent Programming in Java (Second Edition)" (Addison-Wesley, 2000), or
					 * Item 50 in Joshua Bloch's "Effective Java Programming Language Guide"
					 * (Addison-Wesley, 2001). */
					for (//
							long left = 60_000L, expires = Engine.fastTime() + left; //
							left > 0; //
							left = expires - Engine.fastTime()) {
						//
						this.wait(left);
						result = this.result;
						if (result != null) {
							assert result != this : "Circullar reference, this=" + this;
							break sync;
						}
					}
				}
			} catch (final InterruptedException e) {
				return null;
			}
			throw new WaitTimeoutException("Wait timeout (hash=" + System.identityHashCode(this) + ")!");
		}

		assert result != null : "shouldn't be NULL";
		return result.getWaitRealTarget();
	}

	@Override
	public void onDispatch(final ServeRequest query) {

		if (this.result == null) {
			synchronized (this) {
				if (this.result == null) {
					this.enqueue(query);
					return;
				}
			}
		}
		this.result.onDispatch(query);
	}

	@Override
	public ReplyAnswer onHandle(final WebContext<?> context) {

		return null;
	}

	@Override
	public String toString() {

		return this.getClass().getSimpleName() + "{ targetId : " + Format.Ecma.string(this.target) + ", done : " + (this.result != null) + " }";
	}
}
