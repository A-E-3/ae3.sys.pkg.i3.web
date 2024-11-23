package ru.myx.ae3.i3.web;

import ru.myx.ae3.answer.ReplyAnswer;
import ru.myx.ae3.base.BaseObject;
import ru.myx.ae3.l2.ContextHandler;
import ru.myx.ae3.l2.TargetContext;
import ru.myx.ae3.l2.skin.Skin;
import ru.myx.ae3.serve.ServeRequest;
import ru.myx.ae3.skinner.Skinner;

/**
 * @author myx
 * @param <T>
 *
 */
public interface WebContext<T extends TargetContext<?>> extends TargetContext<BaseObject>, ContextHandler<T, BaseObject>, BaseObject {
	
	
	/**
	 *
	 * @param skin
	 */
	void doSetSkin(Skin skin);

	/**
	 *
	 * @param skin
	 */
	void doSetSkinner(Skinner skin);

	/**
	 *
	 * @return
	 */
	ServeRequest getQuery();

	/**
	 *
	 * @return
	 */
	ReplyAnswer getResultReply();
}
