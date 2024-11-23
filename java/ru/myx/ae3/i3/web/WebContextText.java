package ru.myx.ae3.i3.web;

import ru.myx.ae3.answer.Reply;
import ru.myx.ae3.answer.ReplyAnswer;
import ru.myx.ae3.base.BaseObject;
import ru.myx.ae3.i3.TargetInterface;
import ru.myx.ae3.l2.text.TextTargetContext;
import ru.myx.ae3.serve.ServeRequest;
import ru.myx.sapi.FormatSAPI;

/**
 * @author myx
 * 
 */
public class WebContextText extends TextTargetContext implements WebContext<TextTargetContext> {
	
	ServeRequest query;

	/**
	 *
	 * @param target
	 * @param query
	 */
	public WebContextText(final TargetInterface target, final ServeRequest query) {
		super(target);
		this.query = query;
	}

	@Override
	public void doFinish() {

		this.document.close();
	}

	@Override
	public ServeRequest getQuery() {

		return this.query;
	}

	@Override
	public ReplyAnswer getResultReply() {

		final BaseObject resultLayout = this.getResultLayout();
		if (resultLayout instanceof ReplyAnswer) {
			return (ReplyAnswer) resultLayout;
		}
		assert resultLayout == null : "ResultLayout is: " + FormatSAPI.jsDescribe(this.getResultLayout());
		
		/**
		 * collector is supposed to be closed
		 */
		return Reply
				.binary(
						this.getClass().getSimpleName(), //
						this.query,
						this.document.toCloneFactory()) //
				.setAttribute("Content-Type", "text/plain")//
				.setFinal();
	}
}
