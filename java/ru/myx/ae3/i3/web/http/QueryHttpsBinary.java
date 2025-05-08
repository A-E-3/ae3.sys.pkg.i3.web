/**
 *
 */
package ru.myx.ae3.i3.web.http;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.security.MessageDigest;

import ru.myx.ae3.base.BaseMap;
import ru.myx.ae3.base.BaseObject;
import ru.myx.ae3.binary.TransferCopier;
import ru.myx.ae3.common.Value;
import ru.myx.ae3.flow.FlowOperationException;
import ru.myx.ae3.reflect.ReflectionIgnore;
import ru.myx.ae3.serve.BinaryServeRequest;
import ru.myx.ae3.serve.Request;
import ru.myx.ae3.serve.UniversalServeRequest;

@ReflectionIgnore
final class QueryHttpsBinary extends QueryHttps<QueryHttpsBinary> implements BinaryServeRequest<QueryHttpsBinary> {
	
	private final TransferCopier copier;

	QueryHttpsBinary(
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
			final BaseMap parameters,
			final TransferCopier copier) {

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
	public boolean isBinary() {
		
		return true;
	}

	@Override
	public boolean isEmpty() {
		
		return false;
	}

	@Deprecated
	@Override
	public BinaryServeRequest<?> toBinary() {
		
		return this;
	}

	@Override
	public UniversalServeRequest<?> toCharacter() {
		
		try {
			return Request.characterWrapBinary(this);
		} catch (final UnsupportedEncodingException e) {
			throw new FlowOperationException("Error converting from binary to character", e);
		}
	}
}
