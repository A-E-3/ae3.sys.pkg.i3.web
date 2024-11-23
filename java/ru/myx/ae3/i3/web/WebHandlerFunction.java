package ru.myx.ae3.i3.web;


import ru.myx.ae3.answer.ReplyAnswer;
import ru.myx.ae3.base.BaseFunction;
import ru.myx.ae3.base.BaseObject;

final class WebHandlerFunction implements WebHandler {

	final BaseFunction function;

	WebHandlerFunction(final BaseFunction function) {

		this.function = function;
	}

	@Override
	public ReplyAnswer onHandle(final WebContext<?> context) throws Exception {

		final BaseObject result = this.function.callNJ1(this.function, context);
		return result == BaseObject.NULL || result == BaseObject.UNDEFINED
			? null
			: WebInterface.replyFromObject(this.getClass().getSimpleName(), context, result);
	}

}
