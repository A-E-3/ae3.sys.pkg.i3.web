/*
 * Created on 28.04.2006
 */
package ru.myx.ae3.i3.web.telnet;

import ru.myx.ae3.binary.TransferSocket;

final class TelnetHandlerQueue {
	private static final int					LEAF_SIZE					= 32;
	
	private static final int					QUEUE_COUNT					= 32;
	
	private static final int					QUEUE_MASK					= TelnetHandlerQueue.QUEUE_COUNT - 1;
	
	private final int							queueIndex;
	
	private final TelnetSocketHandler[]			parsers;
	
	private int									count;
	
	static int									stsInlineParserCreations	= 0;
	
	private static final TelnetHandlerQueue[]	QUEUES						= TelnetHandlerQueue.createQueues();
	
	private static int							counter						= 0;
	
	private static final TelnetHandlerQueue[] createQueues() {
		final TelnetHandlerQueue[] queues = new TelnetHandlerQueue[TelnetHandlerQueue.QUEUE_COUNT];
		for (int i = TelnetHandlerQueue.QUEUE_MASK; i >= 0; --i) {
			queues[i] = new TelnetHandlerQueue( i );
		}
		return queues;
	}
	
	static final TelnetSocketHandler getParser(final TransferSocket socket, final FlowConfiguration configuration) {
		final TelnetHandlerQueue queue = TelnetHandlerQueue.QUEUES[--TelnetHandlerQueue.counter
				& TelnetHandlerQueue.QUEUE_MASK];
		return queue.getParserImpl( socket, configuration );
	}
	
	static final void reuseParser(final TelnetSocketHandler parser, final int queueIndex) {
		TelnetHandlerQueue.QUEUES[queueIndex].reuseParser( parser );
	}
	
	private TelnetHandlerQueue(final int queueIndex) {
		this.queueIndex = queueIndex;
		this.parsers = new TelnetSocketHandler[TelnetHandlerQueue.LEAF_SIZE];
		for (int i = TelnetHandlerQueue.LEAF_SIZE - 1; i >= 0; --i) {
			this.parsers[i] = new TelnetSocketHandler( queueIndex );
			this.count++;
		}
	}
	
	private final TelnetSocketHandler getParserImpl(final TransferSocket socket, final FlowConfiguration configuration) {
		TelnetSocketHandler parser;
		synchronized (this) {
			if (this.count > 0) {
				parser = this.parsers[--this.count];
				this.parsers[this.count] = null;
			} else {
				parser = null;
			}
		}
		if (parser == null) {
			TelnetHandlerQueue.stsInlineParserCreations++;
			parser = new TelnetSocketHandler( this.queueIndex );
		}
		parser.prepare( socket, configuration );
		return parser;
	}
	
	private final void reuseParser(final TelnetSocketHandler parser) {
		synchronized (this) {
			if (this.count < TelnetHandlerQueue.LEAF_SIZE) {
				this.parsers[this.count++] = parser;
			}
		}
	}
}
