/**
 * Created on 16.09.2002
 *
 * myx - barachta */
package ru.myx.ae3.i3.web.http;

import java.nio.charset.Charset;

import ru.myx.ae3.answer.Reply;
import ru.myx.ae3.answer.ReplyAnswer;
import ru.myx.ae3.base.Base;
import ru.myx.ae3.base.BaseMap;
import ru.myx.ae3.base.BaseObject;
import ru.myx.ae3.help.Convert;
import ru.myx.ae3.help.Format;
import ru.myx.ae3.reflect.ReflectionIgnore;
import ru.myx.ae3.serve.AbstractServeRequestMutable;

/**
 * @author myx
 * 
 * myx - barachta 
 *         "typecomment": Window>Preferences>Java>Templates. To enable and
 *         disable the creation of type comments go to
 *         Window>Preferences>Java>Code Generation.
 */
@ReflectionIgnore
abstract class QueryHttp<T extends QueryHttp<T>> extends AbstractServeRequestMutable<T> {
	
	
	private BaseObject cookies;

	private String target;

	private final String peerId;

	private final String protocol;

	private final String queryString;

	private final SocketHandler parser;

	QueryHttp(
			final SocketHandler parser,
			final String peerIdentity,
			final String sourceAddressExact,
			final String sourceAddress,
			final String targetAddress,
			final Charset inputCharset,
			final String verb,
			final String host,
			final boolean ignoreTargetPort,
			final String path,
			final String queryString,
			final String protocol,
			final BaseMap headers,
			final BaseObject cookies,
			final String[] arguments,
			final BaseMap parameters) {
		super("PROTOCOL/HTTP", verb, headers);
		this.parser = parser;
		this.peerId = peerIdentity;
		this.setResourceIdentifier(HttpProtocol.urlDecode(path, inputCharset));
		this.cookies = cookies;
		this.useArguments(arguments);
		this.useParameters(parameters);
		if (ignoreTargetPort) {
			final int pos = host.indexOf(':');
			this.target = pos != -1
				? host.substring(0, pos)
				: host;
			this.setTargetExact(this.target);
			this.setUrlBase("http://" + this.target);
			this.setUrl(queryString == null
				? "http://" + this.target + path
				: "http://" + this.target + path + '?' + queryString);
		} else {
			this.setTargetExact(host);
			this.setUrlBase("http://" + host);
			this.setUrl(queryString == null
				? "http://" + host + path
				: "http://" + host + path + '?' + queryString);
		}
		this.sourceAddressExact = sourceAddressExact;
		this.sourceAddress = sourceAddress;
		this.targetAddress = targetAddress;
		this.setResponseClass(ReplyAnswer.class);
		this.queryString = HttpProtocol.urlDecode(queryString, inputCharset);
		this.protocol = protocol;
	}

	private final BaseObject getCookies() {
		
		
		return this.cookies == null
			? this.cookies = HttpProtocol.parseCookieString(Base.getString(this.getAttributes(), "Cookie", null))
			: this.cookies;
	}

	@Override
	public String getParameterString() {
		
		
		return this.queryString;
	}

	@Override
	public String getProtocolName() {
		
		
		return HttpProtocol.PNAME_HTTP;
	}

	@Override
	public String getProtocolVariant() {
		
		
		return this.protocol;
	}

	// private String sessionID;
	@Override
	public String getSessionID() {
		
		
		if (this.sessionID != null) {
			return this.sessionID;
		}
		return this.sessionID = Base.getString(this.getCookies(), "SID", null);
	}

	@Override
	public final boolean getStillActual() {
		
		
		return this.parser.isSocketPresentAndOpen();
	}

	@Override
	public String getTarget() {
		
		
		if (this.target == null) {
			final String host = this.getTargetExact();
			if (host == null) {
				return null;
			}
			final int pos = host.indexOf(':');
			if (pos != -1) {
				this.target = host.substring(0, pos);
			} else {
				this.target = host;
			}
		}
		return this.target;
	}

	// private String userID;
	@Override
	public String getUserID() {
		
		
		if (this.userID != null) {
			return this.userID;
		}
		final String uid = Base.getString(this.getCookies(), "UID-s", null);
		return this.userID = uid == null
			? Base.getString(this.getCookies(), "UID", null)
			: uid;
	}

	@SuppressWarnings("unchecked")
	@Override
	public T setTarget(final String target) {
		
		
		this.target = target;
		return (T) this;
	}

	@Override
	public ReplyAnswer toSecureChannel() {
		
		
		/**
		 * Not JavaScript rules
		 */
		final boolean secure = Convert.MapEntry.toBoolean(this.getAttributes(), "Secure", false);
		if (secure) {
			return null;
		}
		// final URL url = new URL( this.getUrl() );
		final StringBuilder url = new StringBuilder();
		url.append("https://");
		url.append(this.getTarget());
		url.append(this.resourcePrefix);
		url.append(this.resourceIdentifier);
		final String[] arguments = this.getArguments();
		if (arguments == null) {
			final String queryString = this.getParameterString();
			if (queryString != null) {
				url.append('?').append(queryString);
			}
		} else {
			boolean first = true;
			for (final String current : arguments) {
				url.append(first
					? '?'
					: '&');
				if (first) {
					first = false;
				}
				url.append(HttpProtocol.urlEncode(current));
			}
		}
		return Reply.redirect(this.getEventTypeId(), this, false, url.toString());
	}

	@Override
	public String toString() {
		
		
		return this.getClass().getSimpleName() + "(" + this.getVerb() + ", " + Format.Ecma.string(this.getUrl()) + ", " + this.peerId + ")";
	}

	@Override
	public ReplyAnswer toUnSecureChannel() {
		
		
		return null;
	}
}
