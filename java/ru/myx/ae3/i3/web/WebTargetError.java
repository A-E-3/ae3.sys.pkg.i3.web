package ru.myx.ae3.i3.web;

import ru.myx.ae3.answer.Reply;
import ru.myx.ae3.answer.ReplyAnswer;
import ru.myx.ae3.help.Format;
import ru.myx.ae3.l2.skin.Skin;

final class WebTargetError extends WebTargetAbstract {
	
	private final Throwable t;
	
	public WebTargetError(final Throwable t) {
		
		super(WebInterface.WEB_SITES);
		this.t = t;
	}
	
	@Override
	public String getTargetId() {

		return null;
	}
	
	@Override
	public ReplyAnswer onHandle(final WebContext<?> context) {

		context.doSetSkin(Skin.SKIN_STANDARD);
		return Reply.string("WEB-ERROR", context.getQuery(), Format.Throwable.toText(this.t)) //
				.setCode(Reply.CD_EXCEPTION) //
				.setNoCaching();
	}
}
