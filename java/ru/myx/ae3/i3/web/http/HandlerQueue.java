/*
 * Created on 28.04.2006
 */
package ru.myx.ae3.i3.web.http;

import ru.myx.ae3.binary.TransferSocket;

final class HandlerQueue {
	
	private static int counter = 0;
	
	private static final int LEAF_SIZE = 32;
	
	private static final int QUEUE_COUNT = 32;
	
	private static final int QUEUE_MASK = HandlerQueue.QUEUE_COUNT - 1;
	
	private static final HandlerQueue[] QUEUES = HandlerQueue.createQueues();
	
	static int stsInlineParserCreations = 0;
	
	private static final HandlerQueue[] createQueues() {
		
		final HandlerQueue[] queues = new HandlerQueue[HandlerQueue.QUEUE_COUNT];
		for (int i = HandlerQueue.QUEUE_MASK; i >= 0; --i) {
			queues[i] = new HandlerQueue(i);
		}
		return queues;
	}
	
	static final SocketHandler getParser(final TransferSocket socket, final FlowConfiguration configuration) {
		
		return HandlerQueue.QUEUES[--HandlerQueue.counter & HandlerQueue.QUEUE_MASK].getParserImpl(socket, configuration);
	}
	
	static final void reuseParser(final SocketHandler parser, final int queueIndex) {
		
		HandlerQueue.QUEUES[queueIndex].reuseParser(parser);
	}
	
	private int count;
	
	private final SocketHandler[] parsers;
	
	private final int queueIndex;
	
	private HandlerQueue(final int queueIndex) {
		this.queueIndex = queueIndex;
		this.parsers = new SocketHandler[HandlerQueue.LEAF_SIZE];
		for (int i = HandlerQueue.LEAF_SIZE - 1; i >= 0; --i) {
			this.parsers[i] = new SocketHandler(queueIndex);
			this.count++;
		}
	}
	
	private final SocketHandler getParserImpl(final TransferSocket socket, final FlowConfiguration configuration) {
		
		SocketHandler parser;
		synchronized (this) {
			if (this.count > 0) {
				parser = this.parsers[--this.count];
				this.parsers[this.count] = null;
			} else {
				parser = null;
			}
		}
		if (parser == null) {
			HandlerQueue.stsInlineParserCreations++;
			parser = new SocketHandler(this.queueIndex);
		}
		parser.prepare(socket, configuration);
		return parser;
	}
	
	private final void reuseParser(final SocketHandler parser) {
		
		synchronized (this) {
			if (this.count < HandlerQueue.LEAF_SIZE) {
				this.parsers[this.count++] = parser;
			}
		}
	}
}
