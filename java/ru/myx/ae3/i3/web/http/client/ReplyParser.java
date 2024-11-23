package ru.myx.ae3.i3.web.http.client;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;

import ru.myx.ae3.Engine;
import ru.myx.ae3.answer.ReplyAnswer;
import ru.myx.ae3.base.Base;
import ru.myx.ae3.base.BaseFunction;
import ru.myx.ae3.base.BaseMap;
import ru.myx.ae3.base.BaseNativeObject;
import ru.myx.ae3.base.BaseNativeObjectCaseInsencetive;
import ru.myx.ae3.base.BaseObject;
import ru.myx.ae3.base.BaseObjectNoOwnProperties;
import ru.myx.ae3.base.BaseProperty;
import ru.myx.ae3.binary.Transfer;
import ru.myx.ae3.binary.TransferBuffer;
import ru.myx.ae3.binary.TransferCollector;
import ru.myx.ae3.binary.TransferCopier;
import ru.myx.ae3.binary.TransferTarget;
import ru.myx.ae3.exec.Exec;
import ru.myx.ae3.exec.ExecProcess;
import ru.myx.ae3.exec.ResultHandler;
import ru.myx.ae3.help.Format;
import ru.myx.ae3.reflect.Reflect;
import ru.myx.ae3.reflect.ReflectionExplicit;
import ru.myx.ae3.reflect.ReflectionHidden;
import ru.myx.ae3.reflect.ReflectionManual;
import ru.myx.sapi.FormatSAPI;

/** @author myx */
@ReflectionManual
public class ReplyParser implements TransferTarget, BaseObjectNoOwnProperties {
	
	private static BaseObject PROTOTYPE = Reflect.classToBasePrototype(ReplyParser.class);
	
	private final static String OWNER = "ae3.http.client";
	
	static final ExecProcess CTX = Exec.getRootProcess();
	
	private static final int MAX_CHUNK_HEADER_SIZE = 128;
	
	private static final int MAX_CHUNK_EXTENSION_SIZE = 4096;
	private static final int MAX_CHUNK_SIZE = 8 * 65536;
	
	private static final int MAX_HEADER_LENGTH = 256;
	
	private static final int MAX_HEADERS = 96;
	
	private static final int MAX_PROTO_VERSION_LENGTH = 10;
	
	private static final int MAX_RESP_CODE_LENGTH = 4;
	
	private static final int MAX_RESP_MSG_LENGTH = 256;
	
	private static final int MAX_VALUE_LENGTH = 4096;
	
	private static final int MD_CHUNKED_BLOCK = 3;
	
	private static final int MD_CHUNKED_EXTENSION = 5;
	
	private static final int MD_CHUNKED_HEADER = 4;
	
	private static final int MD_FIRSTBYTE = 10;
	
	private static final int MD_HEADER = 0;
	
	private static final int MD_LOAD_BODY = 6;
	
	private static final int MD_PROTO_VERSION = 9;
	
	private static final int MD_RESP_CODE = 8;
	
	private static final int MD_RESP_MSG = 7;
	
	private static final int MD_VALUE = 1;
	
	private static final int MD_ABORTED = 11;
	
	static final int QBUFF_INIT = Engine.MODE_SPEED
		? 4096
		: 1024;
	
	static final int QBUFF_RSET = Engine.MODE_SIZE
		? 2048
		: 4096;
	
	static final int QBUFF_STEP = Engine.MODE_SIZE
		? 1024
		: 2048;
	
	static int stBadRequests = 0;
	
	static int stGzipped = 0;
	
	static int stChunked = 0;
	
	static int stExpands = 0;
	
	static int stRequests = 0;
	
	static int stRequestsFinished = 0;
	
	private static final int[] UPCASE_XLAT = ReplyParser.createUpperCaseXlatTable();
	
	private static final int[] createUpperCaseXlatTable() {
		
		final int[] result = new int[128];
		for (int i = 127; i >= 0; --i) {
			result[i] = Character.toUpperCase((char) i);
		}
		return result;
	}
	
	private ExecProcess callbackCtx = null;
	
	private BaseFunction callback = null;
	
	private boolean callbackOnHead = false;
	
	/** replyBody */
	TransferCollector collector;
	
	private boolean http11;
	
	private char[] rBuffer;
	
	private int rBufferCapacity;
	
	private int rBufSize;
	
	private boolean rGzipped;
	
	private boolean rChunked;
	private long rContentLength;
	
	private String rContentType;
	
	private String rCurrentHeader;
	
	ReplyAnswer result = null;
	
	/** replyHeaders */
	BaseMap rHeaders;
	
	private int rHeadersSize;
	
	String rHeadProtocolVersion;
	
	int rHeadResponseCode;
	
	private String rHeadResponseMessage;
	
	private Charset rInputCharset;
	
	private int rLengthRemaining;
	
	private int rMode;
	
	private Closeable closeable;
	
	/** constructor without closeable will not close whatever it is connected to (fed from) */
	@ReflectionExplicit
	public ReplyParser() {
		
		this.rBuffer = new char[this.rBufferCapacity = ReplyParser.QBUFF_INIT];
		this.resetQueryParser();
	}
	
	/** Will close closeable on 'cancel', 'abort', 'reset', 'onDoneRead', 'onError'
	 *
	 * @param closeable */
	@ReflectionExplicit
	public ReplyParser(final Closeable closeable) {
		
		this.rBuffer = new char[this.rBufferCapacity = ReplyParser.QBUFF_INIT];
		this.resetQueryParser();
		this.closeable = closeable;
	}
	
	@Override
	@ReflectionExplicit
	public void abort(final String reason) {
		
		final BaseFunction callback = this.callback;
		if (callback != null) {
			final ExecProcess callbackCtx = this.callbackCtx;
			this.callback = null;
			this.callbackCtx = null;
			
			final String text = reason + ": " + this;
			
			final HttpReplyString reply = new HttpReplyString(//
					ReplyParser.OWNER,
					null,
					-1,
					reason == null
						? "Unknown Error"
						: reason,
					"HTTP/1.1",
					null,
					text//
			);
			
			Exec.callAsyncForkUnrelated(//
					callbackCtx,
					"HttpClient.Reply: aborted, query: " + reply.getQuery(),
					callback,
					BaseObject.UNDEFINED,
					ResultHandler.FU_BNN_NXT,
					reply);
			
		}
		this.resetQueryParser();
	}
	
	@Override
	@ReflectionExplicit
	public final boolean absorb(final int i) {
		
		switch (this.rMode) {
			case MD_HEADER :
				if (i == '\r') {
					return true;
				}
				if (i == '\n') {
					if (this.rBufSize == 0) {
						ReplyParser.stRequests++;
						if (this.rChunked) {
							ReplyParser.stChunked++;
							this.collector = this.prepareCollector();
							if (!this.onDoneHead()) {
								return false;
							}
							this.setMode(ReplyParser.MD_CHUNKED_HEADER, ReplyParser.MAX_CHUNK_HEADER_SIZE);
							return true;
						}
						if (this.rContentLength > 0) {
							if (this.rContentLength == Long.MAX_VALUE) {
								switch (this.rHeadResponseCode) {
									case 100 :
									case 101 :
									case 301 :
									case 302 :
									case 401 :
										this.rContentLength = 0;
										return this.onDoneRead();
									default :
								}
							}
							this.collector = this.prepareCollector();
							if (!this.onDoneHead()) {
								return false;
							}
							this.setMode(ReplyParser.MD_LOAD_BODY, (int) Math.min(this.rContentLength, Integer.MAX_VALUE));
							return true;
						}
						return this.onDoneRead();
					}
					return this.onError("Too many headers");
				}
				if (i == ':') {
					this.rCurrentHeader = this.setMode(ReplyParser.MD_VALUE, ReplyParser.MAX_VALUE_LENGTH);
					return true;
				}
				return this.append(i);
			case MD_VALUE :
				if (i == '\r') {
					return true;
				}
				if (i == ' ' && this.rBufSize == 0) {
					return true;
				}
				if (i == '\n') {
					if (this.rHeadersSize == ReplyParser.MAX_HEADERS) {
						return this.onError("Too many headers, limit is " + ReplyParser.MAX_HEADERS);
					}
					this.onHeader(this.rCurrentHeader, this.setMode(ReplyParser.MD_HEADER, ReplyParser.MAX_HEADER_LENGTH));
					this.rCurrentHeader = "";
					return true;
				}
				return this.append(i);
			case MD_PROTO_VERSION :
				if (i == ' ') {
					this.rHeadProtocolVersion = this.setMode(ReplyParser.MD_RESP_CODE, ReplyParser.MAX_RESP_CODE_LENGTH);
					this.http11 = this.rHeadProtocolVersion.endsWith("/1.1");
					return true;
				}
				if (i >= 0 && i < ' ' || i > 127) {
					return this.onError("Invalid protocol version character: " + Format.Ecma.string(Character.toString((char) i)));
				}
				return this.append(ReplyParser.UPCASE_XLAT[i]);
			case MD_RESP_CODE :
				switch (i) {
					case '0' :
					case '1' :
					case '2' :
					case '3' :
					case '4' :
					case '5' :
					case '6' :
					case '7' :
					case '8' :
					case '9' :
						return this.append(i);
					case ' ' :
						this.rHeadResponseCode = Integer.parseInt(this.setMode(ReplyParser.MD_RESP_MSG, ReplyParser.MAX_RESP_MSG_LENGTH));
						return true;
					default :
						this.onError("Invalid response code character: " + Format.Ecma.string(Character.toString((char) i)));
						return false;
				}
			case MD_RESP_MSG :
				if (i == '\r') {
					return true;
				}
				if (i == '\n') {
					this.rHeadResponseMessage = this.setMode(ReplyParser.MD_HEADER, ReplyParser.MAX_HEADER_LENGTH);
					return true;
				}
				if (i >= 0 && i < ' ') {
					return this.onError("Invalid response message character: " + Format.Ecma.string(Character.toString((char) i)));
				}
				return this.append(i);
			case MD_LOAD_BODY :
				this.collector.getTarget().absorb(i);
				if (--this.rLengthRemaining == 0) {
					return this.onDoneRead();
				}
				return true;
			case MD_CHUNKED_EXTENSION : {
				if (i == '\r') {
					return true;
				}
				if (i != '\n') {
					if (i < ' ') {
						return this.onError(
								"Invalid chunk extension character: " + Format.Ecma.string(Character.toString((char) i)) + ", collected: "
										+ new String(this.rBuffer, 0, this.rBufSize) + ", this=" + this);
					}
					if (--this.rLengthRemaining == 0) {
						return this.onError("Сhunk extension is too long!");
					}
					return true;
				}
			}
			//$FALL-THROUGH$
			case MD_CHUNKED_HEADER :
				if (i == '\r') {
					return true;
				}
				if (i == '\n') {
					if (this.rBufSize == 0) {
						if (--this.rLengthRemaining == 0) {
							return this.onError("No chunk header seen");
						}
						return true;
					}
					final String chHeader = new String(this.rBuffer, 0, this.rBufSize);
					this.rBufSize = 0;
					final int length;
					try {
						length = Integer.parseInt(chHeader, 16);
					} catch (final Throwable t) {
						return this.onError(Format.Throwable.toText("ReplyParser:" + this, t));
					}
					if (length == 0) {
						return this.onDoneRead();
					}
					if (length > ReplyParser.MAX_CHUNK_SIZE) {
						return this.onError("Chunk is too long (" + length + ")");
					}
					this.setMode(ReplyParser.MD_CHUNKED_BLOCK, length);
					return true;
				}
				/** chunk extension */
				if (i == ';' || i == ':' || i == ' ') {
					this.rMode = ReplyParser.MD_CHUNKED_EXTENSION;
					this.rLengthRemaining = ReplyParser.MAX_CHUNK_EXTENSION_SIZE;
					/** No setMode, buffer should stay intact! */
					// this.setMode( ReplyParser.MD_CHUNKED_EXTENSION,
					// ReplyParser.MAX_CHUNK_EXTENSION_SIZE );
					return true;
				}
				/** only hexadecimal */
				if (!(i >= '0' && i <= '9' || i >= 'a' && i <= 'f' || i >= 'A' && i <= 'F')) {
					return this.onError(
							"Invalid chunk header character: " + Format.Ecma.string(Character.toString((char) i)) + ", collected: " + new String(this.rBuffer, 0, this.rBufSize)
									+ ", this=" + this);
				}
				return this.append(i);
			case MD_CHUNKED_BLOCK : {
				this.collector.getTarget().absorb(i);
				if (--this.rLengthRemaining == 0) {
					this.setMode(ReplyParser.MD_CHUNKED_HEADER, ReplyParser.MAX_CHUNK_HEADER_SIZE);
				}
				return true;
			}
			case MD_FIRSTBYTE :
				if (i == '\r' || i == '\n' || i == ' ') {
					return --this.rLengthRemaining > 0;
				}
				this.rHeaders = new BaseNativeObjectCaseInsencetive();
				this.rMode = ReplyParser.MD_PROTO_VERSION;
				if (i <= ' ') {
					this.onError("Invalid first character: " + Format.Ecma.string(Character.toString((char) i)));
					return false;
				}
				return this.append(
						i >= 128
							? i
							: ReplyParser.UPCASE_XLAT[i]);
			default :
				return this.onError("Invalid Parser State: " + this.rMode);
		}
	}
	
	@Override
	@ReflectionExplicit
	public final boolean absorbArray(final byte[] bytes, final int off, final int length) {
		
		for (int i = 0, left = length; left > 0; ++i, --left) {
			if (this.rMode == ReplyParser.MD_LOAD_BODY && this.rLengthRemaining >= left) {
				if (!this.collector.getTarget().absorbArray(bytes, off + i, left)) {
					return this.onError("Error collecting reply body");
				}
				if ((this.rLengthRemaining -= left) == 0) {
					return this.onDoneRead();
				}
				return true;
			}
			if (this.rMode == ReplyParser.MD_CHUNKED_BLOCK && this.rLengthRemaining >= left) {
				if (!this.collector.getTarget().absorbArray(bytes, off + i, left)) {
					return this.onError("Error collecting reply chunk");
				}
				if ((this.rLengthRemaining -= left) == 0) {
					this.setMode(ReplyParser.MD_CHUNKED_HEADER, ReplyParser.MAX_CHUNK_HEADER_SIZE);
				}
				return true;
			}
			if (!this.absorb(bytes[off + i] & 0xFF)) {
				return false;
			}
		}
		return true;
	}
	
	@Override
	@ReflectionExplicit
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
				for (long inboundRemaining = buffer.remaining();;) {
					if (this.rMode == ReplyParser.MD_LOAD_BODY && this.rLengthRemaining >= inboundRemaining) {
						if (!this.collector.getTarget().absorbBuffer(buffer)) {
							return this.onError("Error collecting reply body");
						}
						if ((this.rLengthRemaining -= inboundRemaining) == 0) {
							return this.onDoneRead();
						}
						return true;
					}
					if (this.rMode == ReplyParser.MD_CHUNKED_BLOCK && this.rLengthRemaining >= inboundRemaining) {
						if (!this.collector.getTarget().absorbBuffer(buffer)) {
							return this.onError("Error collecting reply chunk");
						}
						if ((this.rLengthRemaining -= inboundRemaining) == 0) {
							this.setMode(ReplyParser.MD_CHUNKED_HEADER, ReplyParser.MAX_CHUNK_HEADER_SIZE);
						}
						return true;
					}
					if (!this.absorb(buffer.next())) {
						return false;
					}
					if (--inboundRemaining == 0) {
						return true;
					}
				}
			}
		}
		return true;
	}
	
	@Override
	@ReflectionExplicit
	public final boolean absorbNio(final ByteBuffer buffer) {
		
		for (int inboundRemaining = buffer.remaining();;) {
			if (inboundRemaining == 0) {
				return true;
			}
			if (this.rMode == ReplyParser.MD_LOAD_BODY) {
				final int limit = this.rLengthRemaining;
				if (limit >= inboundRemaining) {
					if (!this.collector.getTarget().absorbNio(buffer)) {
						return this.onError("Error collecting reply body");
					}
					if ((this.rLengthRemaining -= inboundRemaining) == 0) {
						return this.onDoneRead();
					}
					return true;
				}
				this.rLengthRemaining = 0;
				buffer.limit(buffer.position() + limit);
				if (!this.collector.getTarget().absorbNio(buffer)) {
					return this.onError("Error collecting reply body tail");
				}
				buffer.limit(buffer.position() + inboundRemaining - limit);
				// no continue - got reply and that's it
				return this.onDoneRead();
			}
			if (this.rMode == ReplyParser.MD_CHUNKED_BLOCK) {
				if (this.rLengthRemaining >= inboundRemaining) {
					this.rLengthRemaining -= inboundRemaining;
					if (!this.collector.getTarget().absorbNio(buffer)) {
						return this.onError("Error collecting reply chunk");
					}
					if (this.rLengthRemaining == 0) {
						this.setMode(ReplyParser.MD_CHUNKED_HEADER, ReplyParser.MAX_CHUNK_HEADER_SIZE);
					}
					return true;
				}
				final int limit = this.rLengthRemaining;
				this.rLengthRemaining = 0;
				buffer.limit(buffer.position() + limit);
				if (!this.collector.getTarget().absorbNio(buffer)) {
					return this.onError("Error collecting reply chunk tail");
				}
				inboundRemaining -= limit;
				buffer.limit(buffer.position() + inboundRemaining);
				this.setMode(ReplyParser.MD_CHUNKED_HEADER, ReplyParser.MAX_CHUNK_HEADER_SIZE);
				// FALL-THROUGH
			}
			{
				if (!this.absorb(buffer.get() & 0xFF)) {
					return false;
				}
				--inboundRemaining;
			}
		}
	}
	
	private final boolean append(final int b) {
		
		if (this.rLengthRemaining == 0) {
			this.onError("token is too long");
			return false;
		}
		if (this.rBufSize + 1 == this.rBufferCapacity) {
			final char[] newBuf = new char[this.rBufferCapacity += ReplyParser.QBUFF_STEP];
			System.arraycopy(this.rBuffer, 0, newBuf, 0, this.rBufSize);
			this.rBuffer = newBuf;
			ReplyParser.stExpands++;
		}
		this.rBuffer[this.rBufSize++] = (char) b;
		return --this.rLengthRemaining > 0;
	}
	
	@Override
	public BaseObject basePrototype() {
		
		return ReplyParser.PROTOTYPE;
	}
	
	void cancel() {
		
		this.rMode = ReplyParser.MD_ABORTED;
		if (this.callback != null) {
			this.callback = null;
			this.callbackCtx = null;
			this.callbackOnHead = false;
			this.result = null;
		}
		final Closeable closeable = this.closeable;
		if (closeable != null) {
			this.closeable = null;
			try {
				closeable.close();
			} catch (final IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	@Override
	@ReflectionExplicit
	public void close() {
		
		/** No Content-Length header, use 'close'. */
		if (this.rMode == ReplyParser.MD_LOAD_BODY && this.rContentLength == Long.MAX_VALUE) {
			this.onDoneRead();
		} else {
			this.abort("Parser Closed");
		}
	}
	
	@Override
	@ReflectionExplicit
	public <A, R> boolean enqueueAction(final ExecProcess ctx, final Function<A, R> function, final A argument) {
		
		// TODO Auto-generated method stub
		return false;
	}
	
	@Override
	@ReflectionExplicit
	public void force() {
		
		// ignore
	}
	
	/** @return */
	@ReflectionExplicit
	public ReplyAnswer getReply() {
		
		try {
			return this.result;
		} finally {
			this.result = null;
		}
	}
	
	/** @return */
	@ReflectionExplicit
	public final String getReplyFirstLine() {
		
		return this.rHeadProtocolVersion + " " + this.rHeadResponseCode + " " + this.rHeadResponseMessage;
	}
	
	private final boolean onDoneHead() {
		
		if (!this.callbackOnHead) {
			return true;
		}
		
		final BaseFunction callback = this.callback;
		
		assert callback != null //
		: "'callback' parameter must be set when 'callbackOnHead' is not FALSE.";
		
		final ReplyAnswer reply = this.result = new HttpReplySource(
				ReplyParser.OWNER,
				this,
				null,
				this.rHeadResponseCode,
				this.rHeadResponseMessage,
				this.rHeadProtocolVersion,
				this.rHeaders,
				this.collector);
		
		final ExecProcess callbackCtx = this.callbackCtx;
		this.callback = null;
		this.callbackCtx = null;
		
		Exec.callAsyncForkUnrelated(//
				callbackCtx,
				"HttpClient.Reply: onDoneHead, query: " + reply.getQuery(),
				callback,
				BaseObject.UNDEFINED,
				ResultHandler.FU_BNN_NXT,
				reply);
		
		return true;
	}
	
	private final boolean onDoneRead() {
		
		ReplyParser.stRequestsFinished++;
		if (this.callbackOnHead && this.result != null) {
			
			this.collector = null;
			this.callbackOnHead = false;
			
			((HttpReplySource) this.result).onDoneRead();
			this.result = null;
			
		} else {
			
			final ReplyAnswer reply;
			if (this.collector == null) {
				if (this.rChunked || this.rContentLength == Long.MAX_VALUE) {
					this.rHeaders.baseDefine("Content-Length", 0, BaseProperty.ATTRS_MASK_WND);
				}
				reply = this.result = new HttpReplyEmpty(ReplyParser.OWNER, null, this.rHeadResponseCode, this.rHeadResponseMessage, this.rHeadProtocolVersion, this.rHeaders);
			} else {
				final TransferCopier copier = this.collector.toBinary();
				if (this.rChunked || this.rContentLength == Long.MAX_VALUE) {
					this.rHeaders.baseDefine("Content-Length", copier.length(), BaseProperty.ATTRS_MASK_WND);
				}
				reply = this.result = new HttpReplyBinary(
						ReplyParser.OWNER,
						null,
						this.rHeadResponseCode,
						this.rHeadResponseMessage,
						this.rHeadProtocolVersion,
						this.rHeaders,
						copier);
			}
			final BaseFunction callback = this.callback;
			if (callback != null) {
				final ExecProcess callbackCtx = this.callbackCtx;
				this.callback = null;
				this.callbackCtx = null;
				this.callbackOnHead = false;
				
				Exec.callAsyncForkUnrelated(//
						callbackCtx,
						"HttpClient.Reply: onDoneRead, query: " + reply.getQuery(),
						callback,
						BaseObject.UNDEFINED,
						ResultHandler.FU_BNN_NXT,
						reply);
			}
			
		}
		
		this.resetQueryParser();
		/** ???? a way to stop, then 'result' must be checked. */
		return false;
	}
	
	private boolean onError(final String error) {
		
		final ReplyAnswer reply = this.result = new HttpReplyString(//
				ReplyParser.OWNER,
				null,
				-1,
				"Reply Parser Error",
				"HTTP/1.1",
				new BaseNativeObject("X-Parser-Error", error),
				error//
		);
		
		// Reply.string( OWNER, null, error ).setCode( -1 );
		final BaseFunction callback = this.callback;
		if (callback != null) {
			final ExecProcess callbackCtx = this.callbackCtx;
			this.callback = null;
			this.callbackCtx = null;
			this.callbackOnHead = false;
			
			Exec.callAsyncForkUnrelated(//
					callbackCtx,
					"HttpClient.Reply: onError, query: " + reply.getQuery(),
					callback,
					BaseObject.UNDEFINED,
					ResultHandler.FU_BNN_NXT,
					reply);
			
			this.result = null;
		}
		this.resetQueryParser();
		ReplyParser.stBadRequests++;
		ReplyParser.stRequests++;
		return false;
	}
	
	private final void onHeader(final String header, final String value) {
		
		final String hdr = header.toLowerCase();
		final int length = hdr.length();
		switch (hdr.charAt(0)) {
			case 'c' :
				if (length == 12 && hdr.equals("content-type")) {
					final String contentType = this.rContentType //
							= this.rInputCharset != StandardCharsets.UTF_8 && value.toLowerCase().indexOf("charset=") == -1
								? value + "; charset=" + this.rInputCharset.name()
								: value;
					this.rHeaders.baseDefine("Content-Type", contentType, BaseProperty.ATTRS_MASK_WED);
					this.rHeadersSize++;
					return;
				}
				if (length == 14 && hdr.equals("content-length")) {
					try {
						this.rContentLength = Integer.parseInt(value, 10);
					} catch (final Throwable t) {
						this.rContentLength = -1;
					}
				}
				if (length == 16 && hdr.equals("content-encoding")) {
					final String val = value.toLowerCase().trim();
					this.rGzipped = val.equals("gzip") || val.startsWith("gzip, ");
					if (this.rGzipped) {
						throw new Error("gzip is not yet supported for Content-Encoding");
					}
					this.rHeadersSize++;
					// transparent
					return;
				}
				break;
			case 't' :
				if (length == 17 && hdr.equals("transfer-encoding")) {
					final String val = value.toLowerCase().trim();
					this.rChunked = val.equals("chunked") || val.endsWith(", chunked");
					this.rGzipped = val.equals("gzip") || val.startsWith("gzip, ");
					if (this.rGzipped) {
						throw new Error("gzip is not yet supported for Transfer-Encoding");
					}
					this.rHeadersSize++;
					// transparent
					return;
				}
				break;
			default :
		}
		final String existing = Base.getString(this.rHeaders, header, null);
		this.rHeaders.baseDefine(
				header, //
				existing != null
					? existing + ", " + value
					: value,
				BaseProperty.ATTRS_MASK_WED);
		this.rHeadersSize++;
	}
	
	private final TransferCollector prepareCollector() {
		
		if (this.collector == null) {
			return this.collector = Transfer.createCollector();
		}
		this.collector.reset();
		return this.collector;
	}
	
	private final void resetQueryParser() {
		
		this.rMode = ReplyParser.MD_FIRSTBYTE;
		
		this.rContentLength = Long.MAX_VALUE;
		this.rChunked = false;
		this.rGzipped = false;
		
		this.rContentType = null;
		this.rLengthRemaining = ReplyParser.MAX_PROTO_VERSION_LENGTH;
		this.rBufSize = 0;
		this.rHeadersSize = 0;
		this.rInputCharset = StandardCharsets.UTF_8;
		
		if (this.rBufferCapacity > ReplyParser.QBUFF_RSET) {
			this.rBuffer = new char[this.rBufferCapacity = ReplyParser.QBUFF_INIT];
		}
		
		final Closeable closeable = this.closeable;
		if (closeable != null) {
			this.closeable = null;
			try {
				closeable.close();
			} catch (final IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	/** Used in require('http') for example.
	 *
	 * @param callback */
	@ReflectionHidden
	public void setCallback(final BaseFunction callback) {
		
		this.setCallback(Exec.currentProcess(), callback);
	}
	
	/** Scripting friendly version
	 *
	 * Used in require('http') for example.
	 *
	 * TODO: check if it is needed to launch callback straight away if response is already fully
	 * parsed and closed.
	 *
	 * @param ctx
	 * @param callback */
	@ReflectionExplicit
	public void setCallback(final ExecProcess ctx, final BaseFunction callback) {
		
		// System.err.println(">>> >>> parser setCallback: callback: " +
		// callback);
		
		if (this.callback != null) {
			throw new IllegalStateException("Callback is already set!");
		}
		this.callbackCtx = Exec.createProcess(ReplyParser.CTX, ctx, "HttpClient.ReplyParser-2-BaseCallback Context");
		this.callback = callback;
		this.callbackOnHead = false;
	}
	
	/** @param bool */
	@ReflectionExplicit
	public void setCallbackOnHead(final boolean bool) {
		
		if (this.callback == null) {
			throw new IllegalStateException("Callback must be set already!");
		}
		this.callbackOnHead = bool;
	}
	
	private final String setMode(final int mode, final int maxLength) {
		
		try {
			return new String(this.rBuffer, 0, this.rBufSize);
		} finally {
			this.rBufSize = 0;
			this.rMode = mode;
			this.rLengthRemaining = maxLength;
		}
	}
	
	@Override
	public String toString() {
		
		return "[HttpReplyParser(mode=" + this.rMode //
				+ ", code=" + this.rHeadResponseCode //
				+ ", message=" + this.rHeadResponseMessage //
				+ ", version=" + this.rHeadProtocolVersion //
				+ ", chunked=" + this.rChunked //
				+ ", gzipped=" + this.rGzipped //
				+ ", headers=" + FormatSAPI.jsObject(this.rHeaders) //
				+ ", collector=" + this.collector //
				+ " )]"//
		;
	}
}
