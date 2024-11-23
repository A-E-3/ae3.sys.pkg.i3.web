package ru.myx.ae3.i3.web;

import java.io.OutputStream;

import ru.myx.ae3.answer.Reply;
import ru.myx.ae3.answer.ReplyAnswer;
import ru.myx.ae3.base.BaseObject;
import ru.myx.ae3.binary.Transfer;
import ru.myx.ae3.binary.TransferCollector;
import ru.myx.ae3.help.Format;
import ru.myx.ae3.i3.TargetInterface;
import ru.myx.ae3.l2.html.HtmlDomTargetContext;
import ru.myx.ae3.serve.ServeRequest;
import ru.myx.sapi.FormatSAPI;

/** @author myx */
public class WebContextRss extends HtmlDomTargetContext<WebContextRss> implements WebContext<WebContextRss> {

	ServeRequest query;
	
	/** @param target
	 * @param query */
	public WebContextRss(final TargetInterface target, final ServeRequest query) {

		super(target);
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
		assert resultLayout == null : "ResultLayout is: " + FormatSAPI.jsDescribe(this.getResultLayout());

		final TransferCollector collector = Transfer.createCollector();
		try (final OutputStream output = collector.getOutputStream()) {
			this.store(output);
		} catch (final Exception e) {
			return Reply.string(this.getClass().getSimpleName(), this.query, Format.Throwable.toText(e)) //
					.setAttribute("Content-Type", "text/plain");
		}
		/** collector is closed by try ^^^ */
		return Reply.binary(this.getClass().getSimpleName(), this.query, collector.toCloneFactory()) //
				.setAttribute("Content-Type", "text/html");
	}
}
