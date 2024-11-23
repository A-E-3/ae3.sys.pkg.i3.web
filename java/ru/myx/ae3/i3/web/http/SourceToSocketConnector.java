package ru.myx.ae3.i3.web.http;

import java.nio.ByteBuffer;
import java.util.function.Function;

import ru.myx.ae3.binary.TransferBuffer;
import ru.myx.ae3.binary.TransferTarget;
import ru.myx.ae3.exec.ExecProcess;

class SourceToSocketConnector implements TransferTarget {

	private long written;

	/** socket output target */
	private TransferTarget target;

	/** HTTP session parser */
	private final SocketHandler parser;

	SourceToSocketConnector(final TransferTarget target, final SocketHandler parser) {
		assert target != null : "NULL target";
		this.target = target;
		this.parser = parser;
	}

	@Override
	public void abort(final String reason) {

		final TransferTarget target = this.target;
		if (target != null) {
			this.target = null;
			this.parser.abort(reason);
		}
	}

	@Override
	public boolean absorb(final int i) {

		this.written += i;
		return this.target.absorb(i);
	}

	@Override
	public boolean absorbArray(final byte[] array, final int off, final int len) {

		this.written += len;
		return this.target.absorbArray(array, off, len);
	}

	@Override
	public boolean absorbBuffer(final TransferBuffer buffer) {

		this.written += buffer.remaining();
		return this.target.absorbBuffer(buffer);
	}

	@Override
	public boolean absorbNio(final ByteBuffer buffer) {

		this.written += buffer.remaining();
		return this.target.absorbNio(buffer);
	}

	@Override
	public void close() {

		final TransferTarget target = this.target;
		if (target != null) {
			this.target = null;
			target.force();
			/** returns TRUE when connection should be kept for new replies<br/>
			 * returns FALSE when connection is not to be held */
			if (Boolean.TRUE != this.parser.executeDone(this.written)) {
				target.close();
			}
		}
	}

	@Override
	public <A, R> boolean enqueueAction(final ExecProcess ctx, final Function<A, R> function, final A argument) {

		return this.target.enqueueAction(ctx, function, argument);
	}

	@Override
	public void force() {

		this.target.force();
	}
}
