package ru.myx.ae3.i3.web.http.client;

import java.security.MessageDigest;

import ru.myx.ae3.answer.BinaryReplyAnswer;
import ru.myx.ae3.answer.Reply;
import ru.myx.ae3.answer.UniversalReplyAnswer;
import ru.myx.ae3.base.BaseMessage;
import ru.myx.ae3.base.BaseNativeObject;
import ru.myx.ae3.base.BaseObject;
import ru.myx.ae3.binary.TransferCopier;
import ru.myx.ae3.common.Value;
import ru.myx.ae3.reflect.ReflectionIgnore;

@ReflectionIgnore
final class HttpReplyBinary extends HttpReply<HttpReplyBinary> implements BinaryReplyAnswer<HttpReplyBinary> {

	private final TransferCopier copier;

	protected HttpReplyBinary(
			final String owner,
			final BaseMessage query,
			final int code,
			final String headMessage,
			final String protoVersion,
			final BaseObject headers,
			final TransferCopier copier) {

		super(owner, query, headMessage, protoVersion, code, headers);
		this.copier = copier;
	}

	@Override
	public TransferCopier getBinary() {

		return this.copier;
	}

	@Override
	public Value<TransferCopier> getBinaryContent() {

		return this.copier;
	}

	@Override
	public long getBinaryContentLength() {

		return this.copier.length();
	}

	@Override
	public MessageDigest getBinaryMessageDigest() {

		return this.copier.getMessageDigest();
	}

	@Override
	public boolean isEmpty() {

		return false;
	}

	@Override
	public HttpReplyBinary nextClone(final BaseMessage query) {

		return new HttpReplyBinary(//
				this.eventTypeId,
				query,
				this.code,
				this.headMessage,
				this.protoVersion,
				this.attributes == null
					? null
					: new BaseNativeObject(this.attributes),
				this.copier);
	}

	@SuppressWarnings("deprecation")
	@Override
	public HttpReplyBinary toBinary() {

		return this;
	}

	@Override
	public UniversalReplyAnswer<?> toCharacter() {

		return Reply.characterWrapBinary(this);
	}
}
