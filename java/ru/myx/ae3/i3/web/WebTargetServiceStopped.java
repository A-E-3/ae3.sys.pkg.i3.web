package ru.myx.ae3.i3.web;

import ru.myx.ae3.answer.Reply;
import ru.myx.ae3.answer.ReplyAnswer;
import ru.myx.ae3.l2.skin.Skin;

final class WebTargetServiceStopped extends WebTargetAbstract {

	static final WebTarget INSTANCE = new WebTargetServiceStopped();

	private WebTargetServiceStopped() {

		super(WebInterface.WEB_SITES);
	}

	@Override
	public String getTargetId() {

		return null;
	}

	@Override
	public ReplyAnswer onHandle(final WebContext<?> context) {

		context.doSetSkin(Skin.SKIN_STANDARD);
		return Reply
				.string(
						"WSM-QD", //
						context.getQuery(),
						"System is being restarted, please try again later.")//
				.setCode(Reply.CD_BUSY) //
				.setNoCaching();
	}
	
}
