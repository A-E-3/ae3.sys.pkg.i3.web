package ru.myx.ae3.i3.web.http.client;

import java.security.MessageDigest;

import ru.myx.ae3.answer.ReplyAnswer;
import ru.myx.ae3.answer.UniversalReplyAnswer;
import ru.myx.ae3.base.BaseMessage;
import ru.myx.ae3.base.BaseNativeObject;
import ru.myx.ae3.base.BaseObject;
import ru.myx.ae3.base.BaseString;
import ru.myx.ae3.binary.TransferCopier;
import ru.myx.ae3.common.Value;
import ru.myx.ae3.reflect.ReflectionIgnore;

@ReflectionIgnore
final class HttpReplyEmpty extends HttpReply<HttpReplyEmpty> implements UniversalReplyAnswer<HttpReplyEmpty> {
	
	
	protected HttpReplyEmpty(final String owner, final BaseMessage query, final int code, final String headMessage, final String protoVersion, final BaseObject headers) {
		
		super(owner, query, headMessage, protoVersion, code, headers);
	}

	@Override
	public TransferCopier getBinary() {
		
		
		return TransferCopier.NUL_COPIER;
	}

	@Override
	public Value<TransferCopier> getBinaryContent() {
		
		
		return TransferCopier.NUL_COPIER;
	}

	@Override
	public long getBinaryContentLength() {
		
		
		return 0;
	}

	@Override
	public MessageDigest getBinaryMessageDigest() {
		
		
		return TransferCopier.NUL_COPIER.getMessageDigest();
	}

	@Override
	public long getCharacterContentLength() {
		
		
		return 0;
	}

	@Override
	public CharSequence getText() {
		
		
		return BaseString.EMPTY;
	}

	@Override
	public Value<? extends CharSequence> getTextContent() {
		
		
		return BaseString.EMPTY;
	}

	@Override
	public boolean isBinary() {
		
		
		return false;
	}

	@Override
	public boolean isCharacter() {
		
		
		return false;
	}

	@Override
	public boolean isEmpty() {
		
		
		return true;
	}

	@Override
	public ReplyAnswer nextClone(final BaseMessage query) {
		
		
		return new HttpReplyEmpty(this.eventTypeId, query, this.code, this.headMessage, this.protoVersion, new BaseNativeObject(this.attributes));
	}

}
