/*
 * Created on 28.04.2006
 */
package ru.myx.ae3.i3.web.http;

import java.nio.ByteBuffer;

import ru.myx.ae3.act.Act;
import java.util.function.Function;
import ru.myx.ae3.binary.TransferBuffer;
import ru.myx.ae3.binary.TransferSocket;
import ru.myx.ae3.binary.TransferTarget;
import ru.myx.ae3.exec.ExecProcess;

final class KeepAliveParserConnector implements TransferTarget {
	private TransferSocket			socket;
	
	private final FlowConfiguration	configuration;
	
	KeepAliveParserConnector(final TransferSocket socket, final FlowConfiguration configuration) {
		this.socket = socket;
		this.configuration = configuration;
	}
	
	@Override
	public final void abort(final String reason) {
		final TransferSocket socket = this.socket;
		if (socket != null) {
			this.socket = null;
			socket.abort( reason );
		}
	}
	
	@Override
	public final boolean absorb(final int i) {
		final TransferTarget parser = HandlerQueue.getParser( this.socket, this.configuration );
		this.socket.getSource().connectTarget( parser );
		return parser.absorb( i );
	}
	
	@Override
	public final boolean absorbArray(final byte[] array, final int off, final int len) {
		final TransferTarget parser = HandlerQueue.getParser( this.socket, this.configuration );
		this.socket.getSource().connectTarget( parser );
		return parser.absorbArray( array, off, len );
	}
	
	@Override
	public final boolean absorbBuffer(final TransferBuffer buffer) {
		final TransferTarget parser = HandlerQueue.getParser( this.socket, this.configuration );
		this.socket.getSource().connectTarget( parser );
		return parser.absorbBuffer( buffer );
	}
	
	@Override
	public final boolean absorbNio(final ByteBuffer buffer) {
		final TransferTarget parser = HandlerQueue.getParser( this.socket, this.configuration );
		this.socket.getSource().connectTarget( parser );
		return parser.absorbNio( buffer );
	}
	
	@Override
	public final void close() {
		final TransferSocket socket = this.socket;
		if (socket != null) {
			this.socket = null;
			socket.close();
		}
	}
	
	@Override
	public final <A, R> boolean enqueueAction(final ExecProcess ctx, final Function<A, R> function, final A argument) {
		Act.launch( ctx, function, argument );
		return true;
	}
	
	@Override
	public final void force() {
		// ignore
	}
	
	final void reconnect() {
		if (this.socket.isOpen()) {
			this.socket.getSource().connectTarget( this );
		}
	}
}
