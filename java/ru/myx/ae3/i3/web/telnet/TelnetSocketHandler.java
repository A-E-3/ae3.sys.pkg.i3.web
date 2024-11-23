package ru.myx.ae3.i3.web.telnet;

import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.function.Function;

import ru.myx.ae3.Engine;
import ru.myx.ae3.act.Act;
import ru.myx.ae3.binary.Transfer;
import ru.myx.ae3.binary.TransferBuffer;
import ru.myx.ae3.binary.TransferCopier;
import ru.myx.ae3.binary.TransferSocket;
import ru.myx.ae3.binary.TransferTarget;
import ru.myx.ae3.console.tty.ConsoleTty;
import ru.myx.ae3.exec.ExecProcess;
import ru.myx.io.DataInputByteArrayFast;
import ru.myx.util.WeakFinalizer;

final class TelnetSocketHandler extends ConsoleTty implements TransferTarget, TransferBuffer, Function<TransferSocket, Void> {

	static final int QBUFF_INIT = 1024;
	
	static final int QBUFF_STEP = 1024;
	
	static int stBadRequests = 0;
	
	static int stExpands = 0;
	
	static int stRequests = 0;
	
	static int stUnexpectedFinalizations = 0;
	
	private final static int MDR_DEFAULT = 0;
	
	private final static int MDR_TELNET_ATTENTION = 1;
	
	private final static int MDR_TELNET_WILL = 2;
	
	private final static int MDR_TELNET_WONT = 3;
	
	private final static int MDR_TELNET_DO = 4;
	
	private final static int MDR_TELNET_DONT = 5;
	
	private final static int MDR_TELNET_SB = 6;
	
	private final static int MDR_TELNET_SB_DIMENSIONS_W1 = 7;
	
	private final static int MDR_TELNET_SB_DIMENSIONS_W2 = 8;
	
	private final static int MDR_TELNET_SB_DIMENSIONS_H1 = 9;
	
	private final static int MDR_TELNET_SB_DIMENSIONS_H2 = 10;
	
	private final static int MDR_TELNET_SB_TERM_TYPE = 11;
	
	private final static int MDD_TERMINAL_DEFAULT = 0;
	
	private final static int MDD_TERMINAL_HIGH_BIT = 1;
	
	private final static int MDD_TERMINAL_ATTENTION = 2;
	
	private final static int MDD_TERMINAL_EXT1_KEY = 3;
	
	private final static int MDD_TERMINAL_EXT2_KEY = 4;
	
	static final int RBUFF_INIT = 4096;
	
	private static final void finalizeStatic(final TelnetSocketHandler x) {
		
		if (x.socket != null) {
			if (x.socket.isOpen()) {
				try {
					x.socket.abort("Finalized");
				} catch (final Throwable t) {
					// ignore
				}
				TelnetSocketHandler.stUnexpectedFinalizations++;
				Telnet.LOG.event(Telnet.PNAME_TELNET, "FINALIZE", "Unexpected http request finalization - non closed socket!" + x.sourcePeerIdentity);
			} else {
				TelnetSocketHandler.stUnexpectedFinalizations++;
				Telnet.LOG.event(Telnet.PNAME_TELNET, "FINALIZE", "Unexpected http request finalization - non null socket! remote=" + x.sourcePeerIdentity);
			}
			x.socket = null;
		}
	}
	
	private final int readMode = TelnetSocketHandler.MDR_DEFAULT;
	
	private final int readModeTerminal = TelnetSocketHandler.MDD_TERMINAL_DEFAULT;
	
	private TransferSocket socket;
	
	private final ByteArrayOutputStream debug = new ByteArrayOutputStream();
	
	private final int queueIndex;
	
	private String sourcePeerAddress;
	
	private String sourcePeerIdentity;
	
	private boolean active;
	
	private final byte[] rBuffer;
	
	private int rBufferLength;
	
	private int rBufferPosition;
	
	{
		WeakFinalizer.register(this, TelnetSocketHandler::finalizeStatic);
	}
	
	TelnetSocketHandler(final int queueIndex) {
		
		this.queueIndex = queueIndex;
		this.rBuffer = new byte[TelnetSocketHandler.RBUFF_INIT];
	}
	
	private final boolean consumeNext(final int b) {
		
		return this.consumeFromClient(b);
	}
	
	@Override
	public final void abort(final String reason) {
		
		this.socket = null;
		this.sourcePeerAddress = null;
		this.sourcePeerIdentity = null;
		final TransferSocket socket = this.socket;
		if (socket != null) {
			socket.abort(reason);
			this.socket = null;
			this.destroy();
		}
	}
	
	// //////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// //////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// //////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// //////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// //////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// //////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// //////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// //////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// //////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// //////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// //////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// //////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// //////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public final boolean absorb(final int i) {
		
		return this.consumeNext(i);
	}
	
	@Override
	public final boolean absorbArray(final byte[] bytes, final int off, final int length) {

		for (int i = 0; i < length; ++i) {
			if (!this.consumeNext(bytes[off + i] & 0xFF)) {
				return false;
			}
		}
		return true;
	}
	
	@Override
	public final boolean absorbBuffer(final TransferBuffer buffer) {
		
		if (buffer.isDirectAbsolutely()) {
			final byte[] bytes = buffer.toDirectArray();
			return this.absorbArray(bytes, 0, bytes.length);
		}
		while (buffer.hasRemaining()) {
			if (buffer.isSequence()) {
				final TransferBuffer next = buffer.nextSequenceBuffer();
				if (!this.absorbBuffer(next)) {
					return false;
				}
			} else {
				for (;;) {
					final long length = buffer.remaining();
					if (length == 0) {
						return true;
					}
					if (!this.consumeNext(buffer.next())) {
						return false;
					}
				}
			}
		}
		return true;
	}
	
	@Override
	public final boolean absorbNio(final ByteBuffer buffer) {
		
		for (int i = buffer.remaining(); i > 0; --i) {
			if (!this.consumeNext(buffer.get() & 0xFF)) {
				return false;
			}
		}
		return true;
	}
	
	/** ping socket with dummy request every 30 seconds */
	@Override
	public Void apply(final TransferSocket socket) {
		
		if (!socket.isOpen() || this.socket != socket) {
			return null;
		}
		this.tryPingClient();
		Act.later(null, this, socket, 27_000L);
		return null;
	}
	
	@Override
	public void close() {
		
		final TransferSocket socket = this.socket;
		if (socket != null) {
			this.flush();
			this.force();
			this.flush();
			socket.close();
			this.socket = null;
			this.destroy();
		}
	}
	
	@Override
	public final void destroy() {
		
		if (this.active && this.socket == null) {
			this.consoleDestroy();
			this.active = false;
			TelnetHandlerQueue.reuseParser(this, this.queueIndex);
		}
		this.rBufferPosition = 0;
	}
	
	@Override
	public <A, R> boolean enqueueAction(final ExecProcess ctx, final Function<A, R> function, final A argument) {

		// TODO Auto-generated method stub
		return false;
	}
	
	@Override
	public void force() {
		
		// TODO Auto-generated method stub
	}
	
	@Override
	public MessageDigest getMessageDigest() {
		
		final MessageDigest digest = Engine.getMessageDigestInstance();
		digest.update(this.rBuffer, this.rBufferPosition, this.rBufferLength);
		return digest;
	}
	
	@Override
	public final boolean hasRemaining() {
		
		return this.rBufferLength - this.rBufferPosition > 0;
	}
	
	@Override
	public final boolean isDirectAbsolutely() {
		
		return this.rBufferPosition == 0 && this.rBufferLength == this.rBuffer.length;
	}
	
	@Override
	public final boolean isSequence() {
		
		return false;
	}
	
	@Override
	public final int next() {
		
		return this.rBuffer[this.rBufferPosition++] & 0xFF;
	}
	
	@Override
	public final int next(final byte[] buffer, final int offset, final int length) {

		final int amount = Math.min(this.rBufferLength - this.rBufferPosition, length);
		if (amount > 0) {
			System.arraycopy(this.rBuffer, this.rBufferPosition, buffer, offset, amount);
			this.rBufferPosition += amount;
		}
		return amount;
	}
	
	@Override
	public final TransferBuffer nextSequenceBuffer() {
		
		throw new UnsupportedOperationException("Not a sequence!");
	}
	
	@Override
	public final long remaining() {
		
		return this.rBufferLength - this.rBufferPosition;
	}
	
	@Override
	public final TransferCopier toBinary() {
		
		return Transfer.createCopier(this.rBuffer, this.rBufferPosition, this.rBufferLength - this.rBufferPosition);
	}
	
	@Override
	public final byte[] toDirectArray() {
		
		if (this.rBufferPosition == 0 && this.rBufferLength == this.rBuffer.length) {
			this.rBufferPosition = this.rBufferLength;
			return this.rBuffer;
		}
		final int remaining = this.rBufferLength - this.rBufferPosition;
		final byte[] result = new byte[remaining];
		System.arraycopy(this.rBuffer, this.rBufferPosition, result, 0, remaining);
		this.rBufferPosition = this.rBufferLength;
		return result;
	}
	
	@Override
	public final DataInputByteArrayFast toInputStream() {
		
		return new DataInputByteArrayFast(this.toDirectArray());
	}
	
	@Override
	public final TransferBuffer toNioBuffer(final ByteBuffer target) {
		
		final int remaining = this.rBufferLength - this.rBufferPosition;
		if (remaining <= 0) {
			this.destroy();
			return null;
		}
		final int writable = target.remaining();
		if (writable <= 0) {
			return this;
		}
		if (writable >= remaining) {
			target.put(this.rBuffer, this.rBufferPosition, remaining);
			this.rBufferPosition = this.rBufferLength;
			this.destroy();
			return null;
		}
		target.put(this.rBuffer, this.rBufferPosition, writable);
		this.rBufferPosition += writable;
		return this;
	}
	
	@Override
	public final InputStreamReader toReaderUtf8() {
		
		return new InputStreamReader(this.toInputStream(), StandardCharsets.UTF_8);
	}
	
	@Override
	public final String toString() {
		
		return "TELNET PARSER TARGET(" + System.identityHashCode(this) + ")";
	}
	
	@Override
	public final String toString(final Charset charset) {
		
		return new String(this.rBuffer, 0, this.rBufferLength, charset);
	}
	
	@Override
	public final String toString(final String charset) throws UnsupportedEncodingException {

		return new String(this.rBuffer, 0, this.rBufferLength, charset);
	}
	
	@Override
	public final TransferBuffer toSubBuffer(final long start, final long end) {
		
		final int remaining = this.rBufferLength - this.rBufferPosition;
		if (start < 0 || start > end || end > remaining) {
			throw new IllegalArgumentException("Indexes are out of bounds: start=" + start + ", end=" + end + ", length=" + remaining);
		}
		this.rBufferLength = (int) (this.rBufferPosition + end);
		this.rBufferPosition += start;
		return this;
	}
	
	@Override
	public MessageDigest updateMessageDigest(final MessageDigest digest) {
		
		digest.update(this.rBuffer, this.rBufferPosition, this.rBufferLength);
		return digest;
	}
	
	@Override
	protected String consolePeerIdentity() {
		
		return this.sourcePeerIdentity;
	}
	
	@Override
	protected boolean consumeFromClientWanted(final boolean flag) {
		
		return this.socket.getSource().connectTarget(
				flag
					? this
					: null);
	}
	
	@Override
	protected boolean consumeFromServer(final TransferBuffer buffer) {
		
		return this.socket.getTarget().absorbBuffer(buffer);
	}
	
	@Override
	protected void onDetectAnsi() {
		
		if (this.isTelnetDetected()) {
			return;
		}
		Act.later(null, this, this.socket, 27_000L);
	}
	
	@Override
	protected void onDetectTelnet() {
		
		if (this.isAnsiDetected()) {
			return;
		}
		Act.later(null, this, this.socket, 27_000L);
	}
	
	final void prepare(final TransferSocket socket, final FlowConfiguration configuration) {

		this.socket = socket;
		this.sourcePeerAddress = socket.getRemoteAddress();
		this.sourcePeerIdentity = socket.getIdentity();
		Telnet.LOG.event(Telnet.PNAME_TELNET, "INFO", "connection: " + socket);
		this.consoleStart();
	}
	
	final void reconnect() {
		
		if (this.socket.isOpen()) {
			this.socket.getSource().connectTarget(this);
		}
	}
	
}
