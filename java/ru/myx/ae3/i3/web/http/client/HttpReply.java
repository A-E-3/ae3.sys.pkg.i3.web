package ru.myx.ae3.i3.web.http.client;

import ru.myx.ae3.answer.AbstractReplyAnswerMutable;
import ru.myx.ae3.base.BaseMessage;
import ru.myx.ae3.base.BaseObject;
import ru.myx.ae3.reflect.ReflectionIgnore;

@ReflectionIgnore
abstract class HttpReply<T extends HttpReply<T>> extends AbstractReplyAnswerMutable<T> {
	
	protected final String headMessage;
	
	protected final String protoVersion;
	
	protected HttpReply(final String owner, final BaseMessage query, final String headMessage, final String protoVersion, final int code, final BaseObject headers) {
		
		super(owner, query, code, headers);
		this.headMessage = headMessage;
		this.protoVersion = protoVersion;
	}
	
	@Override
	public String getProtocolName() {
		
		return "HTTP";
	}
	
	@Override
	public String getProtocolVariant() {
		
		return this.protoVersion;
	}
	
	@Override
	public String getTitle() {
		
		return this.headMessage;
	}
	
}
