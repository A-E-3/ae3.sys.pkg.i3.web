package ru.myx.ae3.i3.web;

import ru.myx.ae3.answer.Reply;
import ru.myx.ae3.answer.ReplyAnswer;
import ru.myx.ae3.base.Base;
import ru.myx.ae3.base.BaseMap;
import ru.myx.ae3.base.BaseObject;
import ru.myx.ae3.help.Format;
import ru.myx.ae3.skinner.Skinner;

/** Generic for all accessible hierarchy object, from the point of view of the 'interface 3'
 * subsystem.
 *
 * @author myx */
public interface WebHandler {
	
	/** @param context
	 * @return
	 * @throws Exception */
	default WebHandler onDrill(final WebContext<?> context) throws Exception {
		
		return null;
	}
	
	/** @param context
	 * @return */
	default ReplyAnswer onEnter(final WebContext<?> context) {
		
		final Skinner skinner = context.getSkinner();
		if (skinner != null) {
			final ReplyAnswer reply = skinner.onQuery(context.getQuery());
			if (reply != null && reply != context.getQuery()) {
				return reply;
			}
		}
		
		return null;
	}
	
	/** @param context
	 * @param t
	 * @return
	 * @throws Exception */
	default ReplyAnswer onException(final WebContext<?> context, final Throwable t) throws Exception {
		
		return Reply.string(
				this.getClass().getSimpleName() + ":ERROR", //
				context.getQuery(),
				Format.Throwable.toText(t))//
				.setCode(Reply.CD_EXCEPTION)//
		;
	}
	
	/** @param context
	 * @return
	 * @throws Exception */
	ReplyAnswer onHandle(WebContext<?> context) throws Exception;
	
	/** @param context
	 * @param reply
	 * @return */
	default ReplyAnswer onLeave(final WebContext<?> context, final ReplyAnswer reply) {
		
		if (!reply.isFinal() && reply.isObject()) {
			final Object object = reply.getObject();
			if (object instanceof BaseMap) {
				final BaseObject base = (BaseObject) object;
				if (!Base.getString(base, "layout", "").isBlank()) {
					return WebInterface.replyFromObject(this.getClass().getSimpleName(), context, base);
				}
			}
		}
		return reply;
	}
	
	/** @param context
	 * @return
	 * @throws Exception */
	default ReplyAnswer onUnknown(final WebContext<?> context) throws Exception {
		
		return Reply.string(this.getClass().getSimpleName() + ":NO_RESPONSE", context.getQuery(), "No response!")//
				.setAttribute("X-Reply-Origin", "onUnknown, " + this.getClass().getSimpleName())//
				.setCode(Reply.CD_UNKNOWN)//
				.setFinal();
	}
}
