package ru.myx.ae3.i3.web;

import ru.myx.ae3.answer.Reply;
import ru.myx.ae3.answer.ReplyAnswer;
import ru.myx.ae3.base.BaseObject;
import ru.myx.ae3.binary.Transfer;
import ru.myx.ae3.binary.TransferCollector;
import ru.myx.ae3.i3.TargetInterface;
import ru.myx.ae3.l2.pdf.PdfTargetContext;
import ru.myx.ae3.serve.ServeRequest;
import ru.myx.sapi.FormatSAPI;

/**
 * @author myx
 * 
 */
public class WebContextPdf extends PdfTargetContext implements WebContext<PdfTargetContext> {
	
	ServeRequest query;

	TransferCollector output;

	/**
	 *
	 * @param target
	 * @param query
	 */
	public WebContextPdf(final TargetInterface target, final ServeRequest query) {
		super(target);
		this.query = query;
		this.output = Transfer.createCollector();
		this.doStart(PdfTargetContext.createA4(this.output.getOutputStream(), null, null));
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
		return Reply.binary(this.getClass().getSimpleName(), this.query, this.output.toCloneFactory()) //
				.setAttribute("Content-Type", "application/pdf");
	}
}
