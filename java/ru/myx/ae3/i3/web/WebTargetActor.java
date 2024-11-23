package ru.myx.ae3.i3.web;

import java.util.function.Function;

import ru.myx.ae3.act.Act;
import ru.myx.ae3.answer.AbstractReplyException;
import ru.myx.ae3.answer.Reply;
import ru.myx.ae3.answer.ReplyAnswer;
import ru.myx.ae3.base.BaseNativeObject;
import ru.myx.ae3.exec.Exec;
import ru.myx.ae3.exec.ExecProcess;
import ru.myx.ae3.help.Format;
import ru.myx.ae3.i3.TargetInterface;
import ru.myx.ae3.serve.ServeRequest;
import ru.myx.ae3.skinner.Skinner;

/** @author myx */
public interface WebTargetActor extends TargetInterface, WebTarget, Function<ServeRequest, Void> {

	@Override
	default Void apply(final ServeRequest query) {

		final WebContext<?> context;
		ReplyAnswer answer;
		try {
			/** check target */
			{
				answer = this.onQuery(query);
				if (answer != null) {
					WebInterface.sendReply(query, answer);
					return null;
				}
			}
			/** create context */
			context = WebContextType.createMatchingContext(this, query);
			// 'check target' supposed to be here, but it is not using context
			// at
			// all yet.
		} catch (final AbstractReplyException e) {
			WebInterface.sendReply(query, e.getReply());
			return null;
		} catch (final Throwable e) {
			WebInterface.sendReply(
					query, //
					Reply.string(
							this.getClass().getSimpleName() + ":ERROR", //
							query,
							Format.Throwable.toText(e))//
							.setCode(Reply.CD_EXCEPTION)//
			);
			return null;
		}

		try {
			/** do drill */
			WebHandler handler = this;

			drill : for (;;) {
				final WebHandler replacement = handler.onDrill(context);
				if (replacement == null || replacement == handler) {
					break drill;
				}
				handler = replacement;
			}
			
			answer = handler.onHandle(context);
			if (answer != null) {
				WebInterface.sendReply(query, answer);
				return null;
			}

			answer = handler.onUnknown(context);
			if (answer != null) {
				WebInterface.sendReply(query, answer);
				return null;
			}

			if (this != handler) {
				answer = this.onUnknown(context);
				if (answer != null) {
					WebInterface.sendReply(query, answer);
					return null;
				}
			}
		} catch (final AbstractReplyException e) {
			WebInterface.sendReply(query, this.onLeave(context, e.getReply()));
			return null;
		} catch (final Throwable t) {
			try {
				answer = this.onException(context, t);
				if (answer != null) {
					WebInterface.sendReply(query, answer);
					return null;
				}
			} catch (final AbstractReplyException e) {
				WebInterface.sendReply(query, e.getReply());
				return null;
			} catch (final Exception e) {
				WebInterface.sendReply(
						query, //
						Reply.string(
								this.getClass().getSimpleName() + ":ERROR", //
								query,
								Format.Throwable.toText(e))//
								.setCode(Reply.CD_EXCEPTION)//
				);
				return null;
			}
			WebInterface.sendReply(
					query, //
					Reply.string(
							this.getClass().getSimpleName() + ":ERROR", //
							query,
							Format.Throwable.toText(t))//
							.setCode(Reply.CD_EXCEPTION)//
			);
			return null;
		}
		/** fail-over response */
		{
			answer = Reply.string(this.getClass().getSimpleName() + ":NO_RESPONSE", query, "No response.")//
					.setAttribute("X-Reply-Origin", "execute, " + this.getTargetId() + ", " + this.getClass().getSimpleName())//
					.setCode(Reply.CD_EMPTY)//
					.setFinal();
			WebInterface.sendReply(query, answer);
			return null;
		}
	}
	@Override
	default void onDispatch(final ServeRequest query) {

		final ExecProcess ctx = Exec.createProcess(WebInterface.CTX, this.getClass().getSimpleName() + ", query: " + query.getUrl());
		ctx.vmScopeDeriveContext(WebInterface.CTX);
		ctx.contextCreateMutableBinding(
				"context", //
				new BaseNativeObject()//
						.putAppend("query", query),
				false);
		Act.launch(ctx, this, query);
	}

	@Override
	default ReplyAnswer onHandle(final WebContext<?> context) throws Exception {

		final Skinner skinner = context.getSkinner();
		if (skinner != null) {
			final ReplyAnswer reply = skinner.onQuery(context.getQuery());
			if (reply != null && reply != context.getQuery()) {
				return reply;
			}
		}
		/** <code>
		final Skin skin = context.getSkin();
		if(skin != null){
			skin.
		}
		</code> */
		return null;
	}

}
