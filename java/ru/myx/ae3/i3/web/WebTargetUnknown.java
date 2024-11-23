package ru.myx.ae3.i3.web;

import java.io.File;

import ru.myx.ae3.Engine;
import ru.myx.ae3.answer.Reply;
import ru.myx.ae3.answer.ReplyAnswer;
import ru.myx.ae3.base.Base;
import ru.myx.ae3.base.BaseFunction;
import ru.myx.ae3.l2.skin.Skin;
import ru.myx.ae3.l2.skin.SkinImpl;

final class WebTargetUnknown extends WebTargetAbstract {
	
	static final WebTarget INSTANCE;

	private static final Skin SKIN_UNKNOWN;

	private static final BaseFunction RETURN_REPLY_OR_UNDEFINED;

	static {
		INSTANCE = new WebTargetUnknown();

		SKIN_UNKNOWN = new SkinImpl(
				WebTargetAbstract.SKIN_WEB_ABSTRACT, //
				new File(
						Engine.PATH_PUBLIC, //
						"resources/skin/skin-web-unknown"));

		try {

			// 'this' is UndefinedHandler
			RETURN_REPLY_OR_UNDEFINED = Base.createFunction(
					"context", //
					"return this.onHandle(context);");
		} catch (final Exception e) {
			throw e instanceof final RuntimeException runtimeException
				? runtimeException
				: new RuntimeException(e);
		}
	}

	private WebTargetUnknown() {

		super(WebInterface.WEB_SITES);
	}

	@Override
	public String getTargetId() {
		
		return null;
	}

	@Override
	public WebHandler onDrill(final WebContext<?> context) throws Exception {
		
		// context.doSetSkin( Skin.SKIN_STANDARD );
		context.doSetSkin(WebTargetUnknown.SKIN_UNKNOWN);
		// if (context.getSkin() == null) {
		// context.doSetSkin(WebTargetUnknown.SKIN_UNKNOWN);
		// }

		return super.onDrill(context);
	}

	@Override
	public ReplyAnswer onHandle(final WebContext<?> context) throws Exception {
		
		final Object result = WebTargetUnknown.RETURN_REPLY_OR_UNDEFINED.callNJ1(WebTargetLoader.UNDEFINED_HANDLER, context).baseValue();
		return result == null
			? Reply.string(
					"WSM-QD", //
					context.getQuery(),
					"Server '" + context.getQuery().getTarget() + "' is not known! ") //
					.setCode(Reply.CD_UNKNOWN)//
					.setNoCaching()
			: WebInterface.replyFromObject(this.getClass().getSimpleName(), context, result);
	}

}
