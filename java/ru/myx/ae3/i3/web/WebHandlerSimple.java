package ru.myx.ae3.i3.web;

import ru.myx.ae3.answer.ReplyAnswer;
import ru.myx.ae3.base.BaseFunction;
import ru.myx.ae3.base.BaseObject;

final class WebHandlerSimple implements WebHandler {

	final BaseObject handler;
	
	WebHandlerSimple(final BaseObject handler) {
		
		this.handler = handler;
	}
	
	@Override
	public WebHandler onDrill(final WebContext<?> context) throws Exception {

		final BaseObject result = this.handler.baseCall("onDrill", true, context);
		if (result == BaseObject.NULL || result == BaseObject.UNDEFINED) {
			return null;
		}
		return result instanceof BaseFunction
			? new WebHandlerFunction((BaseFunction) result)
			: new WebHandlerSimple(result);
	}
	
	@Override
	public ReplyAnswer onHandle(final WebContext<?> context) throws Exception {

		final BaseObject result = this.handler.baseCall("handle", true, context);
		return result == BaseObject.NULL || result == BaseObject.UNDEFINED
			? null
			: WebInterface.replyFromObject(this.getClass().getSimpleName(), context, result);
	}
	
}
