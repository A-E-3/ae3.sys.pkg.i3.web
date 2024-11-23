package ru.myx.ae3.i3.web;

import ru.myx.ae3.answer.Reply;
import ru.myx.ae3.answer.ReplyAnswer;
import ru.myx.ae3.base.Base;
import ru.myx.ae3.base.BaseObject;
import ru.myx.ae3.i3.TargetInterface;
import ru.myx.ae3.json.Json;
import ru.myx.ae3.l2.NativeTargetContext;
import ru.myx.ae3.serve.ServeRequest;

/** @author myx */
public class WebContextJson extends NativeTargetContext implements WebContext<NativeTargetContext> {

	ServeRequest query;

	/** @param target
	 * @param query */
	public WebContextJson(final TargetInterface target, final ServeRequest query) {

		super(target, NativeTargetContext.TargetMode.CLONE);
		this.query = query;
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
		
		final int code = Base.getInt(this.result, "code", 0);
		final ReplyAnswer reply = Reply.string(
				this.getClass().getSimpleName(),
				this.query,
				this.result == null
					? "null"
					: Json.toJsonSource(this.result, false, 0))//
				.setContentType("application/json")//
				.setCode(
						code < 100 || code >= 600
							? 200
							: code)
				.setNoCaching().setFinal();
		return reply;
	}
}
