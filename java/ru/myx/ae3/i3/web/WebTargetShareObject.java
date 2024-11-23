package ru.myx.ae3.i3.web;

import java.io.File;
import java.util.function.Function;

import ru.myx.ae3.Engine;
import ru.myx.ae3.act.Act;
import ru.myx.ae3.answer.ReplyAnswer;
import ru.myx.ae3.base.Base;
import ru.myx.ae3.base.BaseFunction;
import ru.myx.ae3.base.BaseNativeObject;
import ru.myx.ae3.base.BaseObject;
import ru.myx.ae3.base.BasePrimitiveString;
import ru.myx.ae3.base.BaseProperty;
import ru.myx.ae3.base.BaseString;
import ru.myx.ae3.exec.Exec;
import ru.myx.ae3.exec.ExecProcess;
import ru.myx.ae3.help.Format;
import ru.myx.ae3.l2.skin.Skin;
import ru.myx.ae3.l2.skin.SkinImpl;

class WebTargetShareObject extends WebTargetAbstract {
	
	private static final Function<WebTargetShareObject, Void> SHARE_RELOAD = new Function<>() {
		
		@Override
		public Void apply(final WebTargetShareObject arg) {
			
			arg.onUpdateShare(Exec.currentProcess());
			return null;
		}
	};
	
	private static final BasePrimitiveString STR_ON_DRILL = Base.forString("onDrill");
	
	private static final BasePrimitiveString STR_ON_EXCEPTION = Base.forString("onException");
	
	private static final BasePrimitiveString STR_ON_HANDLE = Base.forString("onHandle");
	
	private static final BasePrimitiveString STR_ON_UNKNOWN = Base.forString("onUnknown");
	
	private static final BasePrimitiveString STR_SHARE_CLS = Base.forString("Share");
	
	private static final BasePrimitiveString STR_SHARE_VAR = Base.forString("share");
	
	private static final Skin SKIN_STANDARD_XML;
	
	static {
		SKIN_STANDARD_XML = new SkinImpl(
				WebTargetAbstract.SKIN_WEB_ABSTRACT, //
				new File(
						Engine.PATH_PUBLIC, //
						"resources/skin/skin-standard-xml"));
	}
	
	public static BaseObject probeHandler(final BaseObject handler) {
		
		tryConstructor : {
			final BaseFunction constructor = handler.baseConstruct();
			if (constructor == null) {
				break tryConstructor;
			}
			final BaseObject prototype = constructor.baseConstructPrototype();
			if (prototype == null) {
				break tryConstructor;
			}
			if (prototype.baseGet(WebTargetShareObject.STR_SHARE_CLS, BaseObject.UNDEFINED).baseCall() == null) {
				if (prototype.baseGet(WebTargetShareObject.STR_ON_HANDLE, BaseObject.UNDEFINED).baseCall() == null) {
					break tryConstructor;
				}
			}
			final BaseObject result = BaseObject.createObject(prototype);
			result.baseDefine(BaseString.STR_CONSTRUCTOR, constructor, BaseProperty.ATTRS_MASK_NNN);
			final BaseObject instance = constructor.callNJ0(result);
			return instance == BaseObject.UNDEFINED
				? result
				: instance;
		}
		
		if (handler.baseGet(WebTargetShareObject.STR_SHARE_CLS, BaseObject.UNDEFINED).baseCall() != null) {
			return handler;
		}
		
		if (handler.baseGet(WebTargetShareObject.STR_ON_HANDLE, BaseObject.UNDEFINED).baseCall() != null) {
			return handler;
		}
		
		if (handler.baseCall() != null) {
			return new BaseNativeObject(WebTargetShareObject.STR_ON_HANDLE, handler);
		}
		
		return null;
	}
	
	public static WebTarget probeTarget(final BasePrimitiveString target, final BaseObject handler) {
		
		final BaseObject instance = WebTargetShareObject.probeHandler(handler);
		if (instance == null) {
			WebInterface.LOG.event("WEB-IFACE", "TGT_SHARE", "Probe target, failed: " + target);
		}
		
		WebInterface.LOG.event("WEB-IFACE", "TGT_SHARE", "Probe target, resolved: " + target);
		return new WebTargetShareObject(target, instance);
	}
	
	final BasePrimitiveString target;
	
	private BaseObject share;
	
	private BaseFunction onDrill;
	
	private BaseFunction onException;
	
	private BaseFunction onHandle;
	
	private BaseFunction onUnknown;
	
	private long shareDate;
	
	WebTargetShareObject(final BasePrimitiveString target, final BaseObject share) {
		
		super(WebInterface.WEB_SITES);
		this.target = target;
		this.replaceShare(share);
		this.shareDate = this.share == null
			? 0
			: Engine.fastTime();
	}
	
	WebTargetShareObject(final String target, final BaseObject share) {
		
		super(WebInterface.WEB_SITES);
		this.target = Base.forString(target);
		this.replaceShare(share);
		this.shareDate = this.share == null
			? 0
			: Engine.fastTime();
	}
	
	private void replaceShare(final BaseObject share) {
		
		if (share == null) {
			this.share = null;
			this.onDrill = null;
			this.onException = null;
			this.onHandle = null;
			this.onUnknown = null;
			return;
		}
		
		this.share = share;
		this.onDrill = share.baseGet(WebTargetShareObject.STR_ON_DRILL, BaseObject.UNDEFINED).baseCall();
		this.onException = share.baseGet(WebTargetShareObject.STR_ON_EXCEPTION, BaseObject.UNDEFINED).baseCall();
		this.onHandle = share.baseGet(WebTargetShareObject.STR_ON_HANDLE, BaseObject.UNDEFINED).baseCall();
		this.onUnknown = share.baseGet(WebTargetShareObject.STR_ON_UNKNOWN, BaseObject.UNDEFINED).baseCall();
	}
	
	@Override
	public String getTargetId() {
		
		return this.target.baseToJavaString();
	}
	
	@Override
	public WebHandler onDrill(final WebContext<?> context) throws Exception {
		
		// context.doSetSkin( Skin.SKIN_STANDARD );
		if (context.getSkin() == null) {
			context.doSetSkin(WebTargetShareObject.SKIN_STANDARD_XML);
		}
		
		final long fastTime = Engine.fastTime();
		if (this.shareDate + 5_000L < fastTime) {
			synchronized (this) {
				if (this.shareDate + 5_000L < fastTime) {
					this.shareDate = fastTime;
					final ExecProcess ctx = Exec.createProcess(WebInterface.CTX, "Target: " + this.target + ", update, " + this.getClass().getSimpleName());
					Act.launch(ctx, WebTargetShareObject.SHARE_RELOAD, this);
				}
			}
		}
		
		if (this.share == null) {
			// actually, not unknown but invalid
			return WebTargetUnknown.INSTANCE;
		}
		
		context.baseDefine(WebTargetShareObject.STR_SHARE_VAR, this.share, BaseProperty.ATTRS_MASK_WND);
		
		final BaseFunction function = this.onDrill;
		if (function == null) {
			return super.onDrill(context);
		}
		final BaseObject result = function.callNJ1(this.share, context);
		if (result == BaseObject.NULL || result == BaseObject.UNDEFINED) {
			return super.onDrill(context);
		}
		return result instanceof BaseFunction
			? new WebHandlerFunction((BaseFunction) result)
			: new WebHandlerSimple(result);
	}
	
	@Override
	public ReplyAnswer onException(final WebContext<?> context, final Throwable e) throws Exception {
		
		final BaseFunction function = this.onException;
		if (function == null) {
			return super.onException(context, e);
		}
		final BaseObject result = function.callNJ2(this.share, context, Base.forThrowable(e));
		return result == BaseObject.NULL || result == BaseObject.UNDEFINED
			? super.onException(context, e)
			: WebInterface.replyFromObject(this.getClass().getSimpleName(), context, result);
	}
	
	@Override
	public ReplyAnswer onHandle(final WebContext<?> context) throws Exception {
		
		final BaseFunction function = this.onHandle;
		if (function == null) {
			return super.onHandle(context);
		}
		final BaseObject result = function.callNJ1(this.share, context);
		return result == BaseObject.NULL || result == BaseObject.UNDEFINED
			? super.onHandle(context)
			: WebInterface.replyFromObject(this.getClass().getSimpleName(), context, result);
	}
	
	@Override
	public ReplyAnswer onUnknown(final WebContext<?> context) throws Exception {
		
		final BaseFunction function = this.onUnknown;
		if (function == null) {
			return super.onUnknown(context);
		}
		final Object result = function.callNJ1(this.share, context).baseValue();
		return result == null
			? super.onUnknown(context)
			: WebInterface.replyFromObject(this.getClass().getSimpleName(), context, result);
	}
	
	@Override
	public String toString() {
		
		return '[' + this.getClass().getSimpleName() + ' ' + Format.Ecma.string(this.target) + ']';
	}
	
	void onUpdateShare(final ExecProcess ctx) {
		
		final BaseObject share = WebTargetLoader.resolveHostRouterTarget(ctx, this.target);
		
		if (share.baseValue() == null) {
			this.replaceShare(null);
			return;
			
		}
		
		if (this.share == share) {
			return;
		}
		if (this.onHandle == share) {
			return;
		}

		final BaseObject handler = WebTargetShareObject.probeHandler(share);
		this.replaceShare(handler);
		return;
	}
}
