package ru.myx.ae3.i3.web;

import ru.myx.ae3.answer.Reply;
import ru.myx.ae3.answer.ReplyAnswer;
import ru.myx.ae3.base.BaseObject;
import ru.myx.ae3.i3.TargetInterface;
import ru.myx.ae3.l2.LayoutDefinition;
import ru.myx.ae3.l2.TargetContextAbstract;
import ru.myx.ae3.serve.ServeRequest;
import ru.myx.sapi.FormatSAPI;

/** @author myx */
class WebContextSimple extends TargetContextAbstract<WebContextSimple> implements WebContext<WebContextSimple> {

	private final ServeRequest query;

	/** @param target
	 * @param query */
	protected WebContextSimple(final TargetInterface target, final ServeRequest query) {

		super(target);
		this.query = query;
	}

	@Override
	protected LayoutDefinition<WebContextSimple> getLayoutForContext(final String name) {

		/** really */
		return null;
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

		if (resultLayout instanceof CharSequence) {
			return Reply.string("WebContextSimple:RR", this.query, resultLayout.toString())//
					.setAttribute("Content-Type", "text/plain")//
					.setFinal()//
			;
		}

		assert resultLayout == null //
		: "ResultLayout is: " + FormatSAPI.jsDescribe(resultLayout) //
				+ ", class is: " + resultLayout.getClass().getSimpleName() //
				+ ", ctx class is: " + this.getClass().getSimpleName()//
		;

		/** really */
		return null;
	}

}
