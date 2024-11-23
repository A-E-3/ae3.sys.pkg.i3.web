/**
 *
 */
package ru.myx.ae3.i3.web.http;

import java.nio.charset.Charset;
import java.security.MessageDigest;

import ru.myx.ae3.base.BaseMap;
import ru.myx.ae3.base.BaseObject;
import ru.myx.ae3.base.BaseString;
import ru.myx.ae3.binary.TransferCopier;
import ru.myx.ae3.common.Value;
import ru.myx.ae3.reflect.ReflectionIgnore;
import ru.myx.ae3.serve.UniversalServeRequest;

@ReflectionIgnore
final class QueryHttpsEmpty extends QueryHttps<QueryHttpsEmpty> implements UniversalServeRequest<QueryHttpsEmpty> {
	
	
	QueryHttpsEmpty(
			final SocketHandler parser,
			final String peerIdentity,
			final String sourceAddressExact,
			final String sourceAddress,
			final String targetAddress,
			final Charset overrideInputCharset,
			final String command,
			final String host,
			final boolean ignoreTargetPort,
			final String path,
			final String queryString,
			final String protocol,
			final BaseMap headers,
			final BaseObject cookies,
			final String[] arguments,
			final BaseMap parameters) {
		
		super(
				parser,
				peerIdentity,
				sourceAddressExact,
				sourceAddress,
				targetAddress,
				overrideInputCharset,
				command,
				host,
				ignoreTargetPort,
				path,
				queryString,
				protocol,
				headers,
				cookies,
				arguments,
				parameters);
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
	public boolean isEmpty() {
		
		
		return true;
	}
	
	@Deprecated
	@Override
	public QueryHttpsEmpty toBinary() {
		
		
		return this;
	}
	
	@Deprecated
	@Override
	public QueryHttpsEmpty toCharacter() {
		
		
		return this;
	}
}
