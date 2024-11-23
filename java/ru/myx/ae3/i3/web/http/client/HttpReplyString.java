package ru.myx.ae3.i3.web.http.client;

import ru.myx.ae3.answer.CharacterReplyAnswer;
import ru.myx.ae3.answer.Reply;
import ru.myx.ae3.answer.ReplyAnswer;
import ru.myx.ae3.answer.UniversalReplyAnswer;
import ru.myx.ae3.base.Base;
import ru.myx.ae3.base.BaseMessage;
import ru.myx.ae3.base.BaseNativeObject;
import ru.myx.ae3.base.BaseObject;
import ru.myx.ae3.common.Value;
import ru.myx.ae3.reflect.ReflectionIgnore;

@ReflectionIgnore
final class HttpReplyString extends HttpReply<HttpReplyString> implements CharacterReplyAnswer<HttpReplyString> {

	private final CharSequence string;

	protected HttpReplyString(
			final String owner,
			final BaseMessage query,
			final int code,
			final String headMessage,
			final String protoVersion,
			final BaseObject headers,
			final CharSequence string) {

		super(owner, query, headMessage, protoVersion, code, headers);
		this.string = string;
	}

	@Override
	public long getCharacterContentLength() {

		return this.string.length();
	}

	@Override
	public CharSequence getText() {

		return this.string;
	}

	@Override
	public Value<? extends CharSequence> getTextContent() {

		return Base.forString(this.string);
	}

	@Override
	public boolean isEmpty() {

		return false;
	}

	@Override
	public ReplyAnswer nextClone(final BaseMessage query) {

		return new HttpReplyString(this.eventTypeId, query, this.code, this.headMessage, this.protoVersion, this.attributes == null
			? null
			: new BaseNativeObject(this.attributes), this.string);
	}

	@Override
	public UniversalReplyAnswer<?> toBinary() {

		return Reply.binaryWrapCharacter(this);
	}

	@SuppressWarnings("deprecation")
	@Override
	public HttpReplyString toCharacter() {

		return this;
	}
}
