package ru.myx.ae3.i3.web;

import ru.myx.ae3.serve.ServeRequest;

/**
 * @author myx
 *
 */
public interface WebTarget extends WebHandler {

	/**
	 * Host name.
	 *
	 * @return
	 */
	String getTargetId();

	/**
	 * for in-line execution returns this or replacement target.
	 *
	 * @return
	 */
	default WebTarget getWaitRealTarget() {
		
		return this;
	}

	/**
	 *
	 * @param query
	 */
	void onDispatch(final ServeRequest query);
}
