/**
 * Created on 17.09.2002
 *
 *
 * myx - barachta */
package ru.myx.ae3.i3.web.http;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;
import java.util.function.Function;
import java.util.zip.GZIPOutputStream;

import ru.myx.ae3.Engine;
import ru.myx.ae3.act.Act;
import ru.myx.ae3.answer.Reply;
import ru.myx.ae3.answer.ReplyAnswer;
import ru.myx.ae3.base.Base;
import ru.myx.ae3.base.BaseArray;
import ru.myx.ae3.base.BaseMap;
import ru.myx.ae3.base.BaseMessage;
import ru.myx.ae3.base.BaseNativeObject;
import ru.myx.ae3.base.BaseNativeObjectCaseInsencetive;
import ru.myx.ae3.base.BaseObject;
import ru.myx.ae3.base.BaseProperty;
import ru.myx.ae3.binary.Transfer;
import ru.myx.ae3.binary.TransferBuffer;
import ru.myx.ae3.binary.TransferCollector;
import ru.myx.ae3.binary.TransferCopier;
import ru.myx.ae3.binary.TransferDescription;
import ru.myx.ae3.binary.TransferSocket;
import ru.myx.ae3.binary.TransferSource;
import ru.myx.ae3.binary.TransferTarget;
import ru.myx.ae3.common.Value;
import ru.myx.ae3.exec.Exec;
import ru.myx.ae3.exec.ExecProcess;
import ru.myx.ae3.extra.External;
import ru.myx.ae3.help.Format;
import ru.myx.ae3.mime.MimeType;
import ru.myx.ae3.produce.Produce;
import ru.myx.ae3.report.Report;
import ru.myx.ae3.serve.ServeRequest;
import ru.myx.ae3.transform.SerializationRequest;
import ru.myx.ae3.transform.Transform;
import ru.myx.io.DataInputByteArrayFast;
import ru.myx.util.WeakFinalizer;

/** @author myx
 *
 *         myx - barachta Window>Preferences>Java>Templates. To enable and disable the creation of
 *         type comments go to Window>Preferences>Java>Code Generation.
 *         http://cloud.datashed.net/gems/starlady/TrillianPro.zip */

final class SocketHandler implements TransferTarget, Function<ReplyAnswer, Boolean>, SerializationRequest, TransferBuffer {

	private static final String A_SERVER = "AE3 " + Engine.VERSION_STRING + " HTTP Interface, Pure JAVA";

	private static final String A_SERVER_SERIALIZATION = "AE3 " + Engine.VERSION_STRING + " HTTP Interface, Pure JAVA (serialization)";

	static int stRequests = 0;

	static int stRequestsHttp = 0;

	static int stRequestsHttps = 0;

	static int stBadRequests = 0;

	static int stGzipped = 0;

	static int stChunked = 0;

	static int stHttp10 = 0;

	static int stHttp11 = 0;

	static int stUnexpectedFinalizations = 0;

	static final int RBUFF_INIT = Engine.MODE_SPEED
		? 16384
		: 4096;

	static final int RBUFF_RSET = Engine.MODE_SIZE
		? 4096
		: 32768;

	static final int QBUFF_INIT = Engine.MODE_SPEED
		? 16384
		: 4096;

	static final int QBUFF_STEP = Engine.MODE_SIZE
		? 2048
		: 4096;

	static final int QBUFF_RSET = Engine.MODE_SIZE
		? 4096
		: 32768;

	private static final byte[] BYTES_UNKNOWN_TITLE_PREFFIX = " ".getBytes();

	private static final byte[] BYTES_UNKNOWN_TITLE_SUFFIX = ("\r\nServer: " + SocketHandler.A_SERVER + "\r\nAccept-Ranges: bytes\r\n").getBytes();

	/** private static final byte[] BYTES_UNKNOWN_TITLE_SUFFIX = ("]\r\nServer: " +
	 * SocketHandler.A_SERVER + "\r\nAccept-Ranges: bytes\r\n") .getBytes(); */

	private static final String UNKNOWN_TYPE = "application/octet-stream";

	private static final byte[] DIGIT_ONES = {
			'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '0', '1', '2',
			'3', '4', '5', '6', '7', '8', '9', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '0', '1', '2', '3', '4', '5',
			'6', '7', '8', '9', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '0', '1', '2', '3', '4', '5', '6', '7', '8',
			'9',
	};

	private static final byte[] DIGIT_TENS = {
			'0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '2', '2', '2', '2', '2', '2', '2', '2', '2', '2', '3', '3', '3',
			'3', '3', '3', '3', '3', '3', '3', '4', '4', '4', '4', '4', '4', '4', '4', '4', '4', '5', '5', '5', '5', '5', '5', '5', '5', '5', '5', '6', '6', '6', '6', '6', '6',
			'6', '6', '6', '6', '7', '7', '7', '7', '7', '7', '7', '7', '7', '7', '8', '8', '8', '8', '8', '8', '8', '8', '8', '8', '9', '9', '9', '9', '9', '9', '9', '9', '9',
			'9',
	};

	private static final byte[] DIGITS = {
			'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w',
			'x', 'y', 'z'
	};

	private static final byte[] HD_UNAUTHORIZED = (Reply.CD_UNAUTHORIZED + " User Authentication Required\r\nWWW-Authenticate: Basic realm=\"sign in\"\r\nServer: "
			+ SocketHandler.A_SERVER + "\r\nAccept-Ranges: bytes\r\n").getBytes();

	private static final byte[] HD_BADMETHOD = (Reply.CD_BADMETHOD + " Bad method\r\nServer: " + SocketHandler.A_SERVER + "\r\nAccept-Ranges: bytes\r\nContent-Length: 0\r\n")
			.getBytes();

	private static final byte[] HD_BADRANGE = (Reply.CD_BADRANGE + " Bad range\r\nServer: " + SocketHandler.A_SERVER + "\r\nAccept-Ranges: bytes\r\nContent-Length: 0\r\n")
			.getBytes();

	private static final byte[] HD_BADQUERY = (Reply.CD_BADQUERY + " Bad query\r\nServer: " + SocketHandler.A_SERVER + "\r\nAccept-Ranges: bytes\r\n").getBytes();

	private static final byte[] HD_BUSY = (Reply.CD_BUSY + " Busy\r\nRetry-After: 60\r\nServer: " + SocketHandler.A_SERVER + "\r\nAccept-Ranges: bytes\r\n").getBytes();

	private static final byte[] HD_CONFLICT = (Reply.CD_CONFLICT + " Conflict\r\nServer: " + SocketHandler.A_SERVER + "\r\nAccept-Ranges: bytes\r\n").getBytes();

	private static final byte[] HD_CREATED = (Reply.CD_CREATED + " Created\r\nServer: " + SocketHandler.A_SERVER + "\r\nAccept-Ranges: bytes\r\n").getBytes();

	private static final byte[] HD_DENIED = (Reply.CD_DENIED + " Forbidden\r\nServer: " + SocketHandler.A_SERVER + "\r\nAccept-Ranges: bytes\r\n").getBytes();

	private static final byte[] HD_EXCEPTION = (Reply.CD_EXCEPTION + " Server error\r\nServer: " + SocketHandler.A_SERVER + "\r\nAccept-Ranges: bytes\r\n").getBytes();

	private static final byte[] HD_FAILED = (Reply.CD_FAILED_PRECONDITION + " Failed\r\nServer: " + SocketHandler.A_SERVER + "\r\nAccept-Ranges: bytes\r\n").getBytes();

	private static final byte[] HD_LOCKED = (Reply.CD_LOCKED + " Locked\r\nServer: " + SocketHandler.A_SERVER + "\r\nAccept-Ranges: bytes\r\n").getBytes();

	private static final byte[] HD_LOOKAT = (Reply.CD_LOOKAT + " Found (Redirection)\r\nServer: " + SocketHandler.A_SERVER + "\r\nAccept-Ranges: bytes\r\n").getBytes();

	private static final byte[] HD_MODIFIED = (Reply.CD_FAILED_PRECONDITION + " Modified\r\nServer: " + SocketHandler.A_SERVER
			+ "\r\nAccept-Ranges: bytes\r\nContent-Length: 0\r\n").getBytes();

	private static final byte[] HD_MOVED = (Reply.CD_MOVED + " Moved\r\nServer: " + SocketHandler.A_SERVER + "\r\nAccept-Ranges: bytes\r\n").getBytes();

	private static final byte[] HD_MULTISTATUS = (Reply.CD_MULTISTATUS + " Multistatus\r\nServer: " + SocketHandler.A_SERVER + "\r\nAccept-Ranges: bytes\r\n").getBytes();

	private static final byte[] HD_NOTFOUND = (Reply.CD_UNKNOWN + " Not found\r\nServer: " + SocketHandler.A_SERVER + "\r\nAccept-Ranges: bytes\r\n").getBytes();

	private static final byte[] HD_OK = (Reply.CD_OK + " OK\r\nServer: " + SocketHandler.A_SERVER + "\r\nAccept-Ranges: bytes\r\n").getBytes();

	private static final byte[] HD_OK_DYNAMIC = (Reply.CD_OK + " OK (dynamic)\r\nServer: " + SocketHandler.A_SERVER + "\r\nAccept-Ranges: bytes\r\n").getBytes();

	private static final byte[] HD_OK_EMPTY = (Reply.CD_EMPTY + " Empty\r\nServer: " + SocketHandler.A_SERVER + "\r\nAccept-Ranges: bytes\r\nContent-Length: 0\r\n").getBytes();

	private static final byte[] HD_OK_EMPTY_WITH_BODY = (Reply.CD_OK + " Empty document follows\r\nServer: " + SocketHandler.A_SERVER + "\r\nAccept-Ranges: bytes\r\n").getBytes();

	private static final byte[] HD_OK_MODIFIED = (Reply.CD_OK + " OK (modified)\r\nServer: " + SocketHandler.A_SERVER + "\r\nAccept-Ranges: bytes\r\n").getBytes();

	private static final byte[] HD_PARTIAL = (Reply.CD_PARTIAL + " Partial\r\nServer: " + SocketHandler.A_SERVER + "\r\nAccept-Ranges: bytes\r\n").getBytes();

	private static final byte[] HD_UNIMPLEMENTED = (Reply.CD_UNIMPLEMENTED + " Unimplemented\r\nServer: " + SocketHandler.A_SERVER + "\r\nAccept-Ranges: bytes\r\n").getBytes();

	private static final byte[] HD_UNMODIFIED = (Reply.CD_UNMODIFIED + " Not modified\r\nServer: " + SocketHandler.A_SERVER + "\r\nAccept-Ranges: bytes\r\nContent-Length: 0\r\n")
			.getBytes();

	private static final byte[] HD_UNSUPPORTED = (Reply.CD_UNSUPPORTED_FORMAT + " Unsupported\r\nServer: " + SocketHandler.A_SERVER + "\r\nAccept-Ranges: bytes\r\n").getBytes();

	private static final byte[] HEADER_TE_CHUNKED_ALIVE = "Transfer-Encoding: chunked\r\nConnection: Keep-Alive\r\nKeep-Alive: max=256, timeout=60\r\n".getBytes();

	private static final byte[] HEADER_TE_CHUNKED_CLOSE = "Transfer-Encoding: chunked\r\nConnection: close\r\n".getBytes();

	private static final byte[] HEADER_TE_GZIP_CHUNKED_ALIVE = "Transfer-Encoding: gzip, chunked\r\nConnection: Keep-Alive\r\nKeep-Alive: max=256, timeout=60\r\n".getBytes();

	private static final byte[] HEADER_TE_GZIP_CHUNKED_CLOSE = "Transfer-Encoding: gzip, chunked\r\nConnection: close\r\n".getBytes();

	private static final byte[] HEADER_CE_GZIP_CHUNKED_ALIVE = "Content-Encoding: gzip\r\nTransfer-Encoding: chunked\r\nConnection: Keep-Alive\r\nKeep-Alive: max=256, timeout=60\r\n"
			.getBytes();

	private static final byte[] HEADER_CE_GZIP_CHUNKED_CLOSE = "Content-Encoding: gzip\r\nTransfer-Encoding: chunked\r\nConnection: close\r\n".getBytes();

	private static final byte[] HEADER_TE_GZIP_ALIVE = "Transfer-Encoding: gzip\r\nConnection: close\r\nConnection: Keep-Alive\r\nKeep-Alive: max=256, timeout=60\r\n".getBytes();

	private static final byte[] HEADER_CE_GZIP_ALIVE = "Content-Encoding: gzip\r\nConnection: close\r\nConnection: Keep-Alive\r\nKeep-Alive: max=256, timeout=60\r\n".getBytes();

	private static final byte[] HEADER_CE_GZIP_CLOSE = "Content-Encoding: gzip\r\nConnection: close\r\n".getBytes();

	private static final byte[] HEADER_TE_GZIP_CLOSE = "Transfer-Encoding: gzip\r\nConnection: close\r\n".getBytes();

	private static final byte[] HEADER_CN_IDENTITY_ALIVE = "Connection: Keep-Alive\r\nKeep-Alive: max=256, timeout=60\r\n".getBytes();

	private static final byte[] HEADER_CN_IDENTITY_CLOSE = "Connection: close\r\n".getBytes();

	private static final KeepAliveReadConnector KEEP_ALIVE_CONNECTOR = new KeepAliveReadConnector();

	private static final int MAX_CHUNK_HEADER_SIZE = 128;

	private static final int MAX_CHUNK_SIZE = 32768;

	private static final int MAX_COMMAND_LENGTH = 128;

	private static final int MAX_HEADER_LENGTH = 256;

	private static final int MAX_HEADERS = 96;

	private static final int MAX_PATH_LENGTH = 2048;

	private static final int MAX_PROTOCOL_LENGTH = 64;

	private static final int MAX_QUERY_LENGTH = 8192;

	private static final int MAX_VALUE_LENGTH = 3072;

	private static final int MD_CHUNKED_BLOCK = 8;

	private static final int MD_CHUNKED_HEADER = 7;

	private static final int MD_COMMAND = 2;

	private static final int MD_FIRSTBYTE = 9;

	private static final int MD_HEADER = 0;

	private static final int MD_LOAD_BODY = 6;

	private static final int MD_PATH = 3;

	private static final int MD_PROTOCOL = 5;

	private static final int MD_QUERY = 4;

	private static final int MD_VALUE = 1;

	private static final byte RU_00_RSET = 0b00000000; // fresh parser
	private static final byte RU_01_QDRT = 0b00000001; // query read
	private static final byte RU_02_RDRT = 0b00000010; // reply ready
	private static final byte RU_04_BDRT = 0b00000100; // bytes sent
	private static final byte RU_08_OPEN = 0b00001000; // want close
	private static final byte RU_10_INIT = 0b00010000; // in the reuse queue
	private static final byte RU_20_UREF = 0b00100000; // not to be reused

	private static final byte[] MIN_INTEGER = "-2147483648".getBytes();

	private static final byte[] MIN_LONG = "-9223372036854775808".getBytes();

	private static final byte[] PREFFIX_HTTP10 = "HTTP/1.0 ".getBytes();

	private static final byte[] PREFFIX_HTTP11 = "HTTP/1.1 ".getBytes();

	private static final byte[] RESPONSE_100 = (//
	"HTTP/1.1 100 CONTINUE\r\n" //
			+ "Server: " + SocketHandler.A_SERVER + "\r\n" //
			+ "Content-Length: 0\r\n"//
			+ "\r\n").getBytes();

	private static final byte[] RESPONSE_400 = (//
	"HTTP/1.1 400 BAD REQUEST\r\n"//
			+ "Server: " + SocketHandler.A_SERVER + "\r\n"//
			+ "Connection: close\r\n"//
			+ "Content-Type: text/plain\r\n"//
			+ "Content-Length: 0\r\n"//
			+ "\r\n").getBytes();

	private static final byte[] RESPONSE_414 = (//
	"HTTP/1.1 414 LONG URI\r\n"//
			+ "Server: " + SocketHandler.A_SERVER + "\r\n"//
			+ "Connection: close\r\n"//
			+ "Content-Type: text/plain\r\n"//
			+ "Content-Length: 0\r\n"//
			+ "\r\n").getBytes();

	private static final byte[] RESPONSE_431 = (//
	"HTTP/1.1 431 Request Header Fields Too Large\r\n"//
			+ "Server: " + SocketHandler.A_SERVER + "\r\n"//
			+ "Connection: close\r\n"//
			+ "Content-Type: text/plain\r\n"//
			+ "Content-Length: 0\r\n"//
			+ "\r\n").getBytes();

	private static final int[] SIZE_TABLE = {
			9, 99, 999, 9999, 99999, 999999, 9999999, 99999999, 999999999, Integer.MAX_VALUE
	};

	static int stExpands = 0;

	private static final long UID_EXPIRATION = 60_000L * 60L * 24L * 365L * 5L;

	private static final int[] UPCASE_XLAT = SocketHandler.createUpperCaseXlatTable();

	static {
		HttpProtocol.LOG.event("INFO", "#LEGEND#", "CODE\tPROTO\tCMD\tMEAN-ADDR(,PEER-ADDR)\t\tFLAGS\tTX\tSERVED\tSENT\tPEER-ID\tURL");
	}

	private static final int[] createUpperCaseXlatTable() {

		final int[] result = new int[128];
		for (int i = 127; i >= 0; --i) {
			result[i] = Character.toUpperCase((char) i);
		}
		return result;
	}

	private static final void finalizeStatic(final SocketHandler x) {

		if (x != null && x.socket != null) {
			if (x.socket.isOpen()) {
				try {
					x.socket.abort("Finalized");
				} catch (final Throwable t) {
					// ignore
				}
				SocketHandler.stUnexpectedFinalizations++;
				HttpProtocol.LOG.event(x.protocolName, "FINALIZE", "Unexpected http request finalization - non closed socket! remote=" + x.sourcePeerIdentity);
			} else {
				SocketHandler.stUnexpectedFinalizations++;
				HttpProtocol.LOG.event(x.protocolName, "FINALIZE", "Unexpected http request finalization - non null socket! remote=" + x.sourcePeerIdentity);
			}
			x.socket = null;
		}
	}

	private static final String fixUrl(final ServeRequest query, final String original) {

		if (original.length() > 0 && original.charAt(0) == '/') {
			// return query.getUrlBase() + original.substring(1);
			return original;
		}
		final String url = query.getUrl();
		final int pos = url.indexOf('?');
		return url.substring(
				0,
				pos == -1
					? url.lastIndexOf('/') + 1
					: url.lastIndexOf('/', pos) + 1)
				+ original;
	}

	private static final boolean isValidLocalIp(final String ip) {

		final int len = ip.length();
		IPv4 : {
			if (len <= 6 || len >= 16) { // 1.3.5.7 255.255.255.255
				break IPv4;
			}
			for (int i = len - 1; i >= 0; --i) {
				final char c = ip.charAt(i);
				if (c != '.' && !(c >= '0' && c <= '9')) {
					break IPv4;
				}
			}
			if (ip.charAt(0) != '1') {
				return false;
			}
			return //
			ip.startsWith("10.") || ip.startsWith("127.") || ip.startsWith("192.168.") || ip.startsWith("172.16.")//
			;
		}
		IPv6 : {
			if (len <= 6 || len >= 40) {
				break IPv6;
			}
			for (int i = len - 1; i >= 0; --i) {
				final char c = ip.charAt(i);
				if (c != ':' && !(c >= '0' && c <= '9' || c >= 'A' && c <= 'F' || c >= 'a' && c <= 'f')) {
					break IPv6;
				}
			}
			if (ip.charAt(0) != 'f') {
				return false;
			}
			// TODO FIXME add IPv6 check.
			return //
			ip.startsWith("fd") || ip.startsWith("fc") || ip.startsWith("fe8") //
			;
		}
		return false;
	}

	private static final boolean isValidNonLocalIp(final String ip) {

		final int len = ip.length();
		IPv4 : {
			if (len <= 6 || len >= 16) { // 1.3.5.7 255.255.255.255
				break IPv4;
			}
			for (int i = len - 1; i >= 0; --i) {
				final char c = ip.charAt(i);
				if (c != '.' && !(c >= '0' && c <= '9')) {
					break IPv4;
				}
			}
			if (ip.charAt(0) != '1') {
				return true;
			}
			return !(//
			ip.startsWith("10.") || ip.startsWith("127.") || ip.startsWith("192.168.") || ip.startsWith("172.16.")//
			);
		}
		IPv6 : {
			if (len <= 6 || len >= 40) {
				break IPv6;
			}
			for (int i = len - 1; i >= 0; --i) {
				final char c = ip.charAt(i);
				if (c != ':' && !(c >= '0' && c <= '9' || c >= 'A' && c <= 'F' || c >= 'a' && c <= 'f')) {
					break IPv6;
				}
			}
			if (ip.charAt(0) != 'f') {
				return true;
			}
			// TODO FIXME add IPv6 check.
			return !(//
			ip.startsWith("fd") || ip.startsWith("fc") || ip.startsWith("fe8")//
			);
		}
		return false;
	}

	// Requires positive x
	private static final int stringSizeOfInt(final int x) {

		for (int i = 0;; ++i) {
			if (x <= SocketHandler.SIZE_TABLE[i]) {
				return i + 1;
			}
		}
	}

	// Requires positive x
	private static final int stringSizeOfLong(final long x) {

		long p = 10;
		for (int i = 1; i < 19; ++i) {
			if (x < p) {
				return i;
			}
			p *= 10;
		}
		return 19;
	}

	private TransferCollector collector;

	private FlowConfiguration configuration;

	private String qCurrentHeader;

	private final Date date;

	private final SimpleDateFormat dateFormatHeader;

	private final SimpleDateFormat dateFormatCookie;

	private String sourcePeerAddress;

	private String targetPeerAddress;

	private byte reuse;

	private byte[] rBuffer;

	private BaseMap qHeaders;

	private BaseObject qCookies;

	private int rBufferLength;

	private int rBufferPosition;

	private boolean http11;

	private boolean doChunked;

	private boolean doCompress;

	private boolean doKeepAlive;

	private boolean teGzip;

	private int qPoints;

	private String protocolName;

	private int qArgumentPosition;

	private List<String> qArguments;

	private char[] qBuffer;

	private int qBufferCapacity;

	private int qBufSize;

	private boolean qChunked;

	private String qVerb;

	private int qContentLength;

	private String qContentType;

	private int qHeadersSize;

	private String qHost;

	private int qLengthRemaining;

	private int qMode;

	private Charset qInputCharset;

	private BaseMap qParameters;

	private String qPath;

	private String qProtocol;

	private int qQueryPosition;

	private String qQueryString;

	private ServeRequest query;

	private final int queueIndex;

	private ReplyAnswer reply;

	private TransferSocket socket;

	private String sourceMeanAddress;

	private String sourcePeerIdentity;

	{
		WeakFinalizer.register(this, SocketHandler::finalizeStatic);
	}

	SocketHandler(final int queueIndex) {

		this.queueIndex = queueIndex;
		this.dateFormatHeader = new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss 'GMT'", Locale.ENGLISH);
		this.dateFormatHeader.setTimeZone(Engine.TIMEZONE_GMT);
		this.dateFormatCookie = new SimpleDateFormat("E, dd-MMM-yyyy HH:mm:ss 'GMT'", Locale.ENGLISH);
		this.dateFormatCookie.setTimeZone(Engine.TIMEZONE_GMT);
		this.date = new Date(0L);
		this.qInputCharset = StandardCharsets.UTF_8;
		this.qBuffer = new char[SocketHandler.QBUFF_INIT];
		this.qBufferCapacity = SocketHandler.QBUFF_INIT;
		this.rBuffer = new byte[SocketHandler.RBUFF_INIT];
	}

	private final void addHeader(final String key, final BaseObject value) {

		assert value != null : "NULL java value";
		if (value.baseIsPrimitive()) {
			if (value == BaseObject.UNDEFINED || value == BaseObject.NULL) {
				return;
			}
			this.headAppend(key);
			this.headAppendHDDIV();
			this.headAppend(value.toString());
			this.headAppendCRLF();
			return;
		}
		{
			final BaseArray array = value.baseArray();
			if (array != null) {
				final int length = array.length();
				for (int i = 0; i < length; ++i) {
					this.addHeader(key, array.baseGet(i, BaseObject.UNDEFINED));
				}
				return;
			}
		}
		if (value instanceof Date) {
			this.headAppend(key);
			this.headAppendHDDIV();
			this.headAppend(this.dateFormatHeader.format((Date) value));
			this.headAppendCRLF();
			return;
		}
		{
			final Object baseValue = value.baseValue();
			if (baseValue != value) {
				if (value instanceof Collection<?>) {
					for (final Object item : (Collection<?>) baseValue) {
						this.addHeader(key, Base.forUnknown(item));
					}
					return;
				}
				if (baseValue instanceof byte[]) {
					this.headAppend(key);
					this.headAppendHDDIV();
					this.headAppend((byte[]) baseValue);
					this.headAppendCRLF();
					return;
				}
			}
		}
		this.headAppend(key);
		this.headAppendHDDIV();
		this.headAppend(value.toString());
		this.headAppendCRLF();
	}

	private final boolean append(final int b) {

		if (--this.qLengthRemaining <= 0) {
			switch (this.qMode) {
				case MD_HEADER :
					HttpProtocol.LOG.event(this.protocolName, "INFO", "BAD REQUEST/HEADER TOO LONG: " + "Header length exceeded, remote=" + this.sourcePeerIdentity);
					return this.onError4xxBadRequest(SocketHandler.RESPONSE_431);
				case MD_VALUE :
					HttpProtocol.LOG.event(this.protocolName, "INFO", "BAD REQUEST/HEADER VALUE TOO LONG: " + "Header value length exceeded, remote=" + this.sourcePeerIdentity);
					return this.onError4xxBadRequest(SocketHandler.RESPONSE_431);
				case MD_PATH :
					HttpProtocol.LOG.event(this.protocolName, "INFO", "BAD REQUEST/URI TOO LONG: " + "URI length exceeded, remote=" + this.sourcePeerIdentity);
					return this.onError4xxBadRequest(SocketHandler.RESPONSE_414);
				default :
					HttpProtocol.LOG.event(this.protocolName, "INFO", "BAD REQUEST: " + "length exceeded, mode=" + this.qMode + ", remote=" + this.sourcePeerIdentity);
					return this.onError4xxBadRequest(SocketHandler.RESPONSE_400);
			}
		}
		if (this.qBufSize + 1 == this.qBufferCapacity) {
			final char[] newBuf = new char[this.qBufferCapacity += SocketHandler.QBUFF_STEP];
			System.arraycopy(this.qBuffer, 0, newBuf, 0, this.qBufSize);
			this.qBuffer = newBuf;
			SocketHandler.stExpands++;
		}
		this.qBuffer[this.qBufSize++] = (char) b;
		return true;
	}

	private final ServeRequest createQueryHttp(final TransferCopier copier) {

		SocketHandler.stRequests++;
		SocketHandler.stRequestsHttp++;
		final String[] qArguments = this.qArguments == null
			? null
			: (String[]) this.qArguments.toArray(new String[this.qArguments.size()]);
		return (copier != null
			? new QueryHttpBinary(
					this,
					this.sourcePeerIdentity,
					this.sourcePeerAddress,
					this.sourceMeanAddress,
					this.targetPeerAddress,
					this.qInputCharset,
					this.qVerb,
					this.qHost,
					this.configuration.ignoreTargetPort,
					this.qPath,
					this.qQueryString,
					this.qProtocol,
					this.qHeaders,
					this.qCookies,
					qArguments,
					this.qParameters,
					copier)
			: new QueryHttpEmpty(
					this,
					this.sourcePeerIdentity,
					this.sourcePeerAddress,
					this.sourceMeanAddress,
					this.targetPeerAddress,
					this.qInputCharset,
					this.qVerb,
					this.qHost,
					this.configuration.ignoreTargetPort,
					this.qPath,
					this.qQueryString,
					this.qProtocol,
					this.qHeaders,
					this.qCookies,
					qArguments,
					this.qParameters)).setResponseTarget(this);
	}

	private final ServeRequest createQueryHttps(final TransferCopier copier) {

		SocketHandler.stRequests++;
		SocketHandler.stRequestsHttps++;
		final String[] qArguments = this.qArguments == null
			? null
			: (String[]) this.qArguments.toArray(new String[this.qArguments.size()]);
		return (copier != null
			? new QueryHttpsBinary(
					this,
					this.sourcePeerIdentity,
					this.sourcePeerAddress,
					this.sourceMeanAddress,
					this.targetPeerAddress,
					this.qInputCharset,
					this.qVerb,
					this.qHost,
					this.configuration.ignoreTargetPort,
					this.qPath,
					this.qQueryString,
					this.qProtocol,
					this.qHeaders,
					this.qCookies,
					qArguments,
					this.qParameters,
					copier)
			: new QueryHttpsEmpty(
					this,
					this.sourcePeerIdentity,
					this.sourcePeerAddress,
					this.sourceMeanAddress,
					this.targetPeerAddress,
					this.qInputCharset,
					this.qVerb,
					this.qHost,
					this.configuration.ignoreTargetPort,
					this.qPath,
					this.qQueryString,
					this.qProtocol,
					this.qHeaders,
					this.qCookies,
					qArguments,
					this.qParameters)).setResponseTarget(this);
	}

	private final void headAppend(final byte str[]) {

		final int newCount = this.rBufferLength + str.length;
		if (newCount > this.rBuffer.length) {
			this.headExpand(newCount);
		}
		System.arraycopy(str, 0, this.rBuffer, this.rBufferLength, str.length);
		this.rBufferLength = newCount;
	}

	private final void headAppend(int i) {

		if (i == Integer.MIN_VALUE) {
			this.headAppend(SocketHandler.MIN_INTEGER);
			return;
		}
		final int appendedLength = i < 0
			? SocketHandler.stringSizeOfInt(-i) + 1
			: SocketHandler.stringSizeOfInt(i);
		final int spaceNeeded = this.rBufferLength + appendedLength;
		if (spaceNeeded > this.rBuffer.length) {
			this.headExpand(spaceNeeded);
		}
		int q, r;
		int charPos = spaceNeeded;
		byte sign = 0;
		if (i < 0) {
			sign = '-';
			i = -i;
		}
		// Generate two digits per iteration
		while (i >= 65536) {
			q = i / 100;
			// really: r = i - (q * 100);
			r = i - ((q << 6) + (q << 5) + (q << 2));
			i = q;
			this.rBuffer[--charPos] = SocketHandler.DIGIT_ONES[r];
			this.rBuffer[--charPos] = SocketHandler.DIGIT_TENS[r];
		}
		// Fall thru to fast mode for smaller numbers
		// assert(i <= 65536, i);
		for (;;) {
			q = i * 52429 >>> 16 + 3;
			r = i - ((q << 3) + (q << 1)); // r = i-(q*10) ...
			this.rBuffer[--charPos] = SocketHandler.DIGITS[r];
			i = q;
			if (i == 0) {
				break;
			}
		}
		if (sign != 0) {
			this.rBuffer[--charPos] = sign;
		}
		this.rBufferLength = spaceNeeded;
	}

	private final void headAppend(final String str) {

		if (str == null) {
			this.headAppend("null");
			return;
		}
		final int len = str.length();
		if (len == 0) {
			return;
		}
		if (len < 32) {
			final int newCount = this.rBufferLength + len;
			if (newCount > this.rBuffer.length) {
				this.headExpand(newCount);
			}
			for (int i = 0; i < len; ++i) {
				this.rBuffer[this.rBufferLength++] = (byte) str.charAt(i);
			}
		} else {
			this.headAppend(str.getBytes());
		}
	}

	private final void headAppendCRLF() {

		final int newCount = this.rBufferLength + 2;
		if (newCount > this.rBuffer.length) {
			this.headExpand(newCount);
		}
		this.rBuffer[this.rBufferLength++] = '\r';
		this.rBuffer[this.rBufferLength++] = '\n';
	}

	private final void headAppendHDDIV() {

		final int newCount = this.rBufferLength + 2;
		if (newCount > this.rBuffer.length) {
			this.headExpand(newCount);
		}
		this.rBuffer[this.rBufferLength++] = ':';
		this.rBuffer[this.rBufferLength++] = ' ';
	}

	private final void headExpand(final int minimumCapacity) {

		int newCapacity = (this.rBuffer.length + 1) * 2;
		if (newCapacity < 0) {
			newCapacity = Integer.MAX_VALUE;
		} else //
		if (minimumCapacity > newCapacity) {
			newCapacity = minimumCapacity;
		}
		final byte newValue[] = new byte[newCapacity];
		System.arraycopy(this.rBuffer, 0, newValue, 0, this.rBufferLength);
		this.rBuffer = newValue;
	}

	private final boolean onDoneRead(final TransferCopier copier) {

		final TransferSocket socket = this.socket;
		if (socket != null) {
			socket.getSource().connectTarget(null);
		}

		/** Host header is required for HTTP/1.1 **/
		if (this.http11 && this.qHost == null) {
			HttpProtocol.LOG.event(
					this.protocolName,
					"INFO",
					"BAD REQUEST: no host header with HTTP/1.1 protocol version, verb=" + this.qVerb + ", path=" + this.qPath + ", remote=" + this.sourcePeerIdentity);
			return this.onError4xxBadRequest(SocketHandler.RESPONSE_400);
		}

		/** Creating Query object **/
		final ServeRequest query = this.query = this.protocolName == HttpProtocol.PNAME_HTTP //
				&& !(this.configuration.reverseProxied && Base.getString(this.qHeaders, "Secure", "false").equals("true"))
					? this.createQueryHttp(copier)
					: this.createQueryHttps(copier);

		try {
			if (!this.configuration.target.absorb(query)) {
				if (Report.MODE_DEBUG) {
					HttpProtocol.LOG.event(this.protocolName, "DEBUG", "Flow: stop; Request: " + query);
				}
				return false;
			}
			if (Report.MODE_DEBUG) {
				HttpProtocol.LOG.event(this.protocolName, "DEBUG", "Flow: continue; Request: " + query);
			}
			return true;
		} catch (final RuntimeException e) {
			throw e;
		} catch (final Exception e) {
			throw new RuntimeException(e);
		} finally {
			this.resetQueryParser();
			this.reuse &= ~SocketHandler.RU_01_QDRT;
		}
	}

	/** always returns false */
	private final boolean onError4xxBadRequest(final byte[] fullResponse) {

		if (this.socket == null) {
			return false;
		}
		final TransferTarget target = this.socket.getTarget();
		if (target == null) {
			return false;
		}
		target.absorbArray(fullResponse, 0, fullResponse.length);
		SocketHandler.stBadRequests++;
		SocketHandler.stRequests++;
		if (this.protocolName == HttpProtocol.PNAME_HTTP) {
			SocketHandler.stRequestsHttp++;
		} else {
			SocketHandler.stRequestsHttps++;
		}
		this.close();
		return false;
	}

	private final void onHeader(final String header, final String value) {

		final String hdr = header.toLowerCase();
		final int length = hdr.length();
		switch (hdr.charAt(0)) {
			case 'h' :
				/** second and third conditions go in reverse because of 'cost' */
				if (length == 4 && this.qHost == null && hdr.equals("host")) {
					this.qHost = value.toLowerCase();
				}
				break;
			case 'x' :
				if (length == 6 && hdr.equals("x-host") || length == 16 && hdr.equals("x-forwarded-host") || length == 18 && hdr.equals("x-forwarded-server")) {
					this.qHost = value.toLowerCase();
					break;
				}
				if (length == 15 && hdr.equals("x-forwarded-for") || length == 9 && hdr.equals("x-real-ip")) {
					/** The general format of the header is:
					 *
					 * X-Forwarded-For: client1, proxy1, proxy2
					 *
					 * where the value is a comma+space separated list of IP addresses, the
					 * left-most being the farthest downstream client, and each successive proxy
					 * that passed the request adding the IP address where it received the request
					 * from. In this example, the request passed proxy1, proxy2 and proxy3 (proxy3
					 * appears as remote address of the request).
					 *
					 * http://en.wikipedia.org/wiki/X-Forwarded-For */
					final int pos = value.indexOf(',');
					if (pos == -1) {
						if (SocketHandler.isValidNonLocalIp(value)) {
							this.sourceMeanAddress = value;
						} else//
						// reverse proxied, accept only local
						if (this.sourcePeerAddress == null && SocketHandler.isValidLocalIp(value)) {
							this.sourcePeerAddress = value;
						}
					} else {
						for (final StringTokenizer st = new StringTokenizer(value, ","); st.hasMoreTokens();) {
							final String val = st.nextToken().trim();
							if (SocketHandler.isValidNonLocalIp(val)) {
								if (this.sourceMeanAddress == null) {
									/** For a Peer address we are interested in last address in a
									 * list. For a Mean address we are interested in first address
									 * in a list. */
									this.sourceMeanAddress = val;
								}
								this.sourcePeerAddress = val;
								/** going till the last one */
								// break;
							} else//
							// reverse proxied, accept only local
							if (this.sourcePeerAddress == null && SocketHandler.isValidLocalIp(value)) {
								this.sourcePeerAddress = value;
							}
						}
					}
				}
				break;
			case 'c' :
				switch (length) {
					case 12 : {
						if (hdr.equals("content-type")) {
							if (this.qInputCharset != StandardCharsets.UTF_8 && value.toLowerCase().indexOf("charset=") == -1) {
								this.qContentType = value + "; charset=" + this.qInputCharset.name();
							} else {
								this.qContentType = value;
							}
							this.qHeaders.baseDefine("Content-Type", this.qContentType, BaseProperty.ATTRS_MASK_WED);
							this.qHeadersSize++;
							return;
						}
						break;
					}
					case 14 : {
						if (hdr.equals("content-length")) {
							try {
								this.qContentLength = Integer.parseInt(value, 10);
							} catch (final Throwable t) {
								this.qContentLength = -1;
							}
						}
						break;
					}
					case 6 : {
						if (hdr.equals("cookie")) {
							this.qCookies = HttpProtocol.parseCookieString(value);
						}
						break;
					}
					default :
				}
				break;
			case 't' :
				if (length == 17 && hdr.equals("transfer-encoding")) {
					final String val = value.toLowerCase().trim();
					this.qChunked = val.equals("chunked") || val.endsWith(", chunked");
					if (val.equals("gzip") || val.startsWith("gzip, ")) {
						throw new Error("gzip is not yet supported for Transfer-Encoding");
					}
					/** supposed to be transparent */
					this.qHeadersSize++;
					return;
				}
				if (length == 2 && hdr.equals("te")) {
					for (final StringTokenizer st = new StringTokenizer(value.toLowerCase(), ","); st.hasMoreTokens();) {
						final String val = st.nextToken().trim();
						if ("gzip".equals(val)) {
							this.teGzip = true;
						}
					}
					/** supposed to be transparent */
					this.qHeadersSize++;
					return;
				}
				break;
			case 'e' :
				if (length == 6 && hdr.equals("expect")) {
					if (value.equalsIgnoreCase("100-continue")) {
						final TransferTarget target = this.socket.getTarget();
						target.absorbArray(SocketHandler.RESPONSE_100, 0, SocketHandler.RESPONSE_100.length);
						target.force();
						this.qHeadersSize++;
						return;
					}
				}
				break;
			default :
		}
		final String existing = Base.getString(this.qHeaders, header, null);
		this.qHeaders.baseDefine(
				header,
				existing != null
					? existing + ", " + value
					: value,
				BaseProperty.ATTRS_MASK_WED);
		this.qHeadersSize++;
	}

	private final long render(final ReplyAnswer reply) throws IOException {

		if (reply.isEmpty()) {
			reply.setAttribute("Content-Length", "0");
			this.startRender(reply);
			if (!this.socket.getTarget().absorbBuffer(this)) {
				this.close();
			}
			return 0;
		}
		if (reply.isFile()) {
			final File file = reply.getFile();
			if (Base.hasProperty(reply.getAttributes(), "Content-Name") && !Base.hasProperty(reply.getAttributes(), "Content-Disposition")) {
				// reply.setContentDisposition( "file" );
				reply.setContentDisposition("inline");
			}
			final long modified = file.lastModified();
			final long time = Engine.fastTime();
			final TransferBuffer buffer = Transfer.createBuffer(file);
			reply.setAttribute("Last-Modified", Base.forDateMillis(modified));
			reply.setAttribute("Expires", Base.forDateMillis((time - modified) / 2 + time));
			return this.renderBuffer(reply, buffer);
		}
		if (reply.isBinary()) {
			final TransferBuffer buffer = reply.toBinary().getBinary().nextCopy();
			return this.renderBuffer(reply, buffer);
		}
		if (reply.isCharacter()) {
			final int code = reply.getCode();
			if (code == Reply.CD_LOOKAT || code == Reply.CD_MOVED) {
				this.startRender(reply);
				if (!this.socket.getTarget().absorbBuffer(this)) {
					this.close();
				}
				return 0;
			}
			return this.render(this.reply = reply.toBinary());
		}
		if (reply.isObject()) {
			final Class<?> responseClass = reply.getObjectClass();
			final Object responseObject;
			if (External.class.isAssignableFrom(responseClass)) {
				responseObject = ((External) reply.getObject()).toBinary();
			} else //
			if (Value.class.isAssignableFrom(responseClass)) {
				responseObject = ((Value<?>) reply.getObject()).baseValue();
			} else {
				responseObject = reply.getObject();
			}
			if (responseObject instanceof BaseMessage) {
				final BaseMessage message = ((BaseMessage) responseObject).toBinary();
				final BaseObject mAttrs = message.getAttributes();
				if (mAttrs != null) {
					final BaseObject rAttrs = reply.getAttributes();
					{
						final String contentType = Base.getString(mAttrs, "Content-Type", "").trim();
						if (contentType.length() > 0) {
							reply.setAttribute("Content-Type", contentType);
						}
					}
					{
						final String contentDisposition = Base.getString(mAttrs, "Content-Disposition", "").trim();
						if (contentDisposition.length() > 0) {
							final String contentType = Base.getString(rAttrs, "Content-Type", "").trim();
							if (!contentType.startsWith("image/") && contentDisposition.startsWith("inline")) {
								reply.setAttribute(
										"Content-Disposition", //
										"attachment" + contentDisposition.substring(6));
							} else {
								reply.setAttribute(
										"Content-Disposition", //
										Base.getString(rAttrs, "Content-Disposition", contentDisposition).trim());
							}
						}
					}
					if (!Base.getBoolean(rAttrs, "Last-Modified", false) && Base.getBoolean(mAttrs, "Last-Modified", false)) {
						reply.setAttribute(
								"Last-Modified", //
								mAttrs.baseGet("Last-Modified", null));
					}
					if (!Base.getBoolean(rAttrs, "Content-Id", false) && Base.getBoolean(mAttrs, "Content-Id", false)) {
						reply.setAttribute(
								"Content-Id", //
								mAttrs.baseGet("Content-Id", null));
					}
					if (!Base.getBoolean(rAttrs, "Content-Name", false) && Base.getBoolean(mAttrs, "Content-Name", false)) {
						reply.setAttribute(
								"Content-Name", //
								mAttrs.baseGet("Content-Name", null));
					}
				}
				final TransferBuffer buffer = message.toBinary().getBinary().nextCopy();
				return this.renderBuffer(reply, buffer);
			}
			if (responseObject instanceof byte[]) {
				final TransferBuffer buffer = Transfer.wrapBuffer((byte[]) responseObject);
				return this.renderBuffer(reply, buffer);
			}
			if (responseObject instanceof CharSequence) {
				final TransferBuffer buffer = Transfer.wrapBuffer(responseObject.toString().getBytes());
				return this.renderBuffer(reply, buffer);
			}
			if (responseObject instanceof TransferBuffer) {
				final TransferBuffer buffer = (TransferBuffer) responseObject;
				return this.renderBuffer(reply, buffer);
			}
			if (responseObject instanceof TransferCopier) {
				final TransferBuffer buffer = ((TransferCopier) responseObject).nextCopy();
				return this.renderBuffer(reply, buffer);
			}
			if (responseObject instanceof TransferSource) {
				final boolean contentFollows = this.startRender(reply);
				final TransferTarget socketTarget = this.socket.getTarget();
				if (!socketTarget.absorbBuffer(this)) {
					this.close();
					return 0;
				}
				if (!contentFollows) {
					/** no close, no body */
					return 0;
				}
				((TransferSource) responseObject).connectTarget(//
						new SourceToSocketConnector(socketTarget, this)//
				);
				return -1;
			}
			if (responseObject instanceof FileInputStream) {
				try (final FileInputStream fis = (FileInputStream) responseObject) {
					reply.setAttribute("Content-Length", String.valueOf(fis.available()));
					final boolean contentFollows = this.startRender(reply);
					if (!this.socket.getTarget().absorbBuffer(this)) {
						this.close();
						return 0;
					}
					if (!contentFollows) {
						/** no close, no body */
						return 0;
					}
					final TransferCollector collector = this.prepareCollector();
					if (this.doChunked) {
						SocketHandler.stChunked++;
						collector.startChunking(2048, 65536);
					}
					assert !this.doCompress : "doCompress is expected to be false here!";
					Transfer.toStream(fis, collector.getOutputStream(), true);
					final TransferBuffer buffer = collector.toBuffer();
					final long bodyLength = buffer.remaining();
					if (!this.socket.getTarget().absorbBuffer(buffer)) {
						this.close();
						return 0;
					}
					return bodyLength;
				}
			}
			@SuppressWarnings("resource")
			final TransferCollector collector = this.prepareCollector();
			if (!Transform.serialize(this)) {
				final byte[] message = ("Cannot convert an object, class=" + responseObject.getClass().getName() + "!").getBytes();
				reply.setCode(Reply.CD_EXCEPTION);
				reply.setAttribute("Content-Length", String.valueOf(message.length));
				final boolean contentFollows = this.startRender(reply);
				if (contentFollows) {
					this.headAppend(message);
				}
				if (!this.socket.getTarget().absorbBuffer(this)) {
					this.close();
					return 0;
				}
				return contentFollows
					? message.length
					: 0;
			}
			collector.close();
			final TransferBuffer buffer = collector.toBuffer();
			try {
				return buffer.remaining();
			} finally {
				if (!this.socket.getTarget().absorbBuffer(buffer)) {
					this.close();
				}
			}
		}
		final byte[] message = ("Unknown response type, class=" + reply.getClass().getName() + "!").getBytes();
		reply.setCode(Reply.CD_EXCEPTION);
		reply.setAttribute("Content-Length", String.valueOf(message.length));
		final boolean contentFollows = this.startRender(reply);
		if (contentFollows) {
			this.headAppend(message);
		}
		if (!this.socket.getTarget().absorbBuffer(this)) {
			this.close();
			return 0;
		}
		return contentFollows
			? message.length
			: 0;
	}

	private final long renderBuffer(final ReplyAnswer reply, final TransferBuffer bufferOriginal) throws IOException {

		final TransferBuffer buffer;
		if (bufferOriginal == null) {
			buffer = TransferBuffer.NUL_BUFFER;
		} else //
		if (reply.getCode() == Reply.CD_OK) {
			final String header = Base.getString(this.qHeaders, "Range", null);
			if (header != null && header.toLowerCase().startsWith("bytes=")) {
				try {
					final String value = header.substring(6).trim();
					final int pos = value.indexOf('-');
					if (pos != -1) {
						final long length = bufferOriginal.remaining();
						final long rangeStart;
						final long rangeEnd;
						{
							final String leftPart = value.substring(0, pos).trim();
							final String rightPart = value.substring(pos + 1).trim();
							if (leftPart.length() == 0) {
								if (rightPart.length() > 0) {
									final long count = Long.parseLong(rightPart, 10);
									if (count > length) {
										rangeStart = length - count;
									} else {
										rangeStart = 0;
									}
									rangeEnd = length;
								} else {
									rangeStart = 0;
									rangeEnd = length;
								}
							} else {
								rangeStart = Long.parseLong(leftPart, 10);
								if (rightPart.length() > 0) {
									final long last = Long.parseLong(rightPart, 10) + 1;
									rangeEnd = last > length
										? length
										: last;
								} else {
									rangeEnd = length;
								}
							}
						}
						if (rangeEnd < rangeStart || rangeStart < 0 || rangeEnd < 0) {
							SocketHandler.stBadRequests++;
							HttpProtocol.LOG.event(this.protocolName, "INFO", "BAD REQUEST: invalid range: " + "start=" + rangeStart + ", end=" + rangeEnd + ", length=" + length);
							reply.setCode(Reply.CD_BADRANGE);
							reply.setAttribute("Content-Range", "*/" + bufferOriginal.remaining());
							this.startRender(reply);
							if (!this.socket.getTarget().absorbBuffer(this)) {
								this.close();
							}
							return 0;
						}
						buffer = bufferOriginal.toSubBuffer(rangeStart, rangeEnd);
						reply.setAttribute("Content-Range", "bytes " + rangeStart + '-' + (rangeEnd - 1) + '/' + length);
						reply.setAttribute("Content-Length", rangeEnd - rangeStart);
						reply.setCode(Reply.CD_PARTIAL);
					} else {
						buffer = bufferOriginal;
					}
				} catch (final Throwable t) {
					SocketHandler.stBadRequests++;
					HttpProtocol.LOG.event(this.protocolName, "INFO", "BAD REQUEST: broken range: header=" + header);
					reply.setCode(Reply.CD_BADQUERY);
					this.startRender(reply);
					if (!this.socket.getTarget().absorbBuffer(this)) {
						this.close();
					}
					return 0;
				}
			} else {
				buffer = bufferOriginal;
			}
		} else {
			buffer = bufferOriginal;
		}
		final long length = buffer.remaining();
		if (!this.configuration.ignoreGzip) {
			if (length > 1024 && length < 1024 * 1024 && Base.getString(reply.getAttributes(), "Content-Encoding", null) == null) {
				final String contentTypeObject = Base.getString(reply.getAttributes(), "Content-Type", null);
				final String contentType;
				if (contentTypeObject == null) {
					contentType = MimeType.forName(this.qPath, SocketHandler.UNKNOWN_TYPE);
					if (contentType != SocketHandler.UNKNOWN_TYPE) {
						reply.setAttribute("Content-Type", contentType);
					}
				} else {
					contentType = contentTypeObject;
				}
				if (contentType == SocketHandler.UNKNOWN_TYPE || MimeType.compressByContentTypeSure(contentType, false)) {
					this.doCompress = true;
				}
			}
		}
		final boolean contentFollows = this.startRender(reply);
		if (!this.socket.getTarget().absorbBuffer(this)) {
			this.close();
			return 0;
		}
		if (!contentFollows) {
			return 0;
		}
		final TransferBuffer bufferResult;
		if (this.doChunked) {
			final TransferCollector collector = this.prepareCollector();
			++SocketHandler.stChunked;
			collector.startChunking(2048, 65536);
			if (this.doCompress) {
				final GZIPOutputStream gos = new GZIPOutputStream(collector.getOutputStream());
				Transfer.toStream(buffer, gos, true);
			} else {
				collector.getTarget().absorbBuffer(buffer);
				collector.close();
			}
			bufferResult = collector.toBuffer();
		} else //
		if (this.doCompress) {
			final TransferCollector collector = this.prepareCollector();
			final GZIPOutputStream gos = new GZIPOutputStream(collector.getOutputStream());
			Transfer.toStream(buffer, gos, true);
			bufferResult = collector.toBuffer();
		} else {
			bufferResult = buffer;
		}
		final long bodySize = bufferResult.remaining();
		if (!this.socket.getTarget().absorbBuffer(bufferResult)) {
			this.close();
			return 0;
		}
		return bodySize;
	}

	private final void resetQueryParser() {

		this.qQueryString = null;
		this.qHost = null;
		this.qContentType = null;
		this.qArguments = null;
		this.qParameters = null;
		this.qInputCharset = StandardCharsets.UTF_8;
		if (this.qBufferCapacity > SocketHandler.QBUFF_RSET) {
			this.qBuffer = new char[this.qBufferCapacity = SocketHandler.QBUFF_INIT];
		}
	}

	private final void resetResponseBuilder() {

		this.qPath = null;
		this.qVerb = null;
		this.qHeaders = null;
		this.qCookies = null;
		this.qProtocol = null;
		this.query = null;
		this.doCompress = false;
		this.doChunked = false;
		this.teGzip = false;
		this.socket = null;
		this.sourceMeanAddress = null;
		this.sourcePeerAddress = null;
		this.sourcePeerIdentity = null;
		this.reply = null;
	}

	private final String setMode(final int mode, final int maxLength) {

		try {
			return new String(this.qBuffer, 0, this.qBufSize);
		} finally {
			this.qBufSize = 0;
			this.qMode = mode;
			this.qLengthRemaining = maxLength;
		}
	}

	/** returns: true if content should follow */
	private final boolean startRender(final ReplyAnswer reply) throws java.io.IOException {

		final ServeRequest query = this.query;
		final BaseObject attributes = reply.getAttributes();
		boolean suppressContentLength = false;
		boolean bodylessResponse = false;
		boolean delayResponse = false;
		final int code;
		switch (reply.getCode()) {
			case Reply.CD_OK : {
				final Object lastModifiedObject = Base.get(attributes, "Last-Modified", null);
				if (lastModifiedObject instanceof Date) {
					final long lastModified = ((Date) lastModifiedObject).getTime();
					if (lastModified >= 0) {
						final long ifModifiedSince;
						{
							final String value = Base.getString(this.qHeaders, "If-Modified-Since", null);
							if (value != null) {
								long temp;
								try {
									temp = this.dateFormatHeader.parse(value).getTime();
								} catch (final ParseException e) {
									temp = -1L;
								}
								ifModifiedSince = temp;
							} else {
								ifModifiedSince = -1L;
							}
						}
						if (ifModifiedSince >= 0) {
							switch (this.configuration.ifModifiedSinceMode) {
								case FlowConfiguration.IMS_MODE_EXACT : {
									final long difference = 1000L + lastModified - ifModifiedSince & 0x7FFFFFFFFFFFFFFFL;
									if (difference < 2000L) {
										suppressContentLength = true;
										bodylessResponse = true;
										this.headAppend(SocketHandler.HD_UNMODIFIED);
										code = Reply.CD_UNMODIFIED;
									} else {
										this.headAppend(SocketHandler.HD_OK_MODIFIED);
										code = Reply.CD_OK;
									}
									break;
								}
								case FlowConfiguration.IMS_MODE_BEFORE : {
									if (lastModified < ifModifiedSince + 1000L) {
										suppressContentLength = true;
										bodylessResponse = true;
										this.headAppend(SocketHandler.HD_UNMODIFIED);
										code = Reply.CD_UNMODIFIED;
									} else {
										this.headAppend(SocketHandler.HD_OK_MODIFIED);
										code = Reply.CD_OK;
									}
									break;
								}
								default :
									this.headAppend(SocketHandler.HD_OK);
									code = Reply.CD_OK;
									break;
							}
						} else {
							final long ifNotModifiedSince;
							{
								final String value = Base.getString(this.qHeaders, "If-Unmodified-Since", null);
								if (value != null) {
									long temp;
									try {
										temp = this.dateFormatHeader.parse(value).getTime();
									} catch (final ParseException e) {
										temp = -1L;
									}
									ifNotModifiedSince = temp;
								} else {
									ifNotModifiedSince = -1L;
								}
							}
							if (ifNotModifiedSince >= 0) {
								final long difference = 1000L + lastModified - ifNotModifiedSince & 0x7FFFFFFFFFFFFFFFL;
								if (difference > 2000L) {
									suppressContentLength = true;
									bodylessResponse = true;
									this.headAppend(SocketHandler.HD_MODIFIED);
									code = Reply.CD_FAILED_PRECONDITION;
								} else {
									this.headAppend(SocketHandler.HD_OK_MODIFIED);
									code = Reply.CD_OK;
								}
							} else {
								this.headAppend(SocketHandler.HD_OK);
								code = Reply.CD_OK;
							}
						}
					} else {
						this.headAppend(SocketHandler.HD_OK_DYNAMIC);
						code = Reply.CD_OK;
					}
				} else {
					this.headAppend(SocketHandler.HD_OK);
					code = Reply.CD_OK;
				}
			}
				break;
			case Reply.CD_EMPTY : {
				if (reply.isEmpty()) {
					suppressContentLength = true;
					bodylessResponse = true;
					reply.setAttribute("Content-Type", BaseObject.UNDEFINED);
					this.headAppend(SocketHandler.HD_OK_EMPTY);
					code = Reply.CD_EMPTY;
				} else {
					this.headAppend(SocketHandler.HD_OK_EMPTY_WITH_BODY);
					code = Reply.CD_OK;
				}
			}
				break;
			case Reply.CD_PARTIAL : {
				this.headAppend(SocketHandler.HD_PARTIAL);
				code = Reply.CD_PARTIAL;
			}
				break;
			case Reply.CD_LOOKAT : {
				reply.setNoCaching();
				assert attributes != null : "NULL java object";
				if (Base.getBoolean(attributes, "Location", false)) {
					this.headAppend(SocketHandler.HD_LOOKAT);
					code = Reply.CD_LOOKAT;
				} else {
					final ReplyAnswer converted = reply.toCharacter();
					if (converted != reply) {
						return this.startRender(converted);
					}
					this.headAppend(SocketHandler.HD_LOOKAT);
					code = Reply.CD_LOOKAT;
					final String original = reply.toCharacter().getText().toString();
					reply.setAttribute(
							"Location",
							original != null
								? HttpProtocol.urlEncode(
										original.indexOf("://") == -1
											? SocketHandler.fixUrl(query, original)
											: original)
								: SocketHandler.fixUrl(query, "/"));
				}
				reply.setAttribute("Content-Length", "0");
				bodylessResponse = true;
				reply.setAttribute("Content-Type", BaseObject.UNDEFINED);
			}
				break;
			case Reply.CD_MOVED : {
				assert attributes != null : "NULL java object";
				if (Base.getBoolean(attributes, "Location", false)) {
					this.headAppend(SocketHandler.HD_MOVED);
					code = Reply.CD_MOVED;
				} else {
					final ReplyAnswer converted = reply.toCharacter();
					if (converted != reply) {
						return this.startRender(converted);
					}
					this.headAppend(SocketHandler.HD_MOVED);
					code = Reply.CD_MOVED;
					final String original = reply.toCharacter().getText().toString();
					reply.setAttribute(
							"Location",
							original != null
								? HttpProtocol.urlEncode(
										original.indexOf("://") == -1
											? SocketHandler.fixUrl(query, original)
											: original)
								: SocketHandler.fixUrl(query, "/"));
				}
				reply.setAttribute("Content-Length", "0");
				bodylessResponse = true;
				reply.setAttribute("Content-Type", BaseObject.UNDEFINED);
			}
				break;
			case Reply.CD_UNMODIFIED : {
				suppressContentLength = true;
				bodylessResponse = true;
				this.headAppend(SocketHandler.HD_UNMODIFIED);
				code = Reply.CD_UNMODIFIED;
			}
				break;
			case Reply.CD_UNAUTHORIZED : {
				this.headAppend(SocketHandler.HD_UNAUTHORIZED);
				code = Reply.CD_UNAUTHORIZED;
			}
				break;
			case Reply.CD_DENIED : {
				this.headAppend(SocketHandler.HD_DENIED);
				code = Reply.CD_DENIED;
				delayResponse = true;
			}
				break;
			case Reply.CD_UNKNOWN : {
				this.headAppend(SocketHandler.HD_NOTFOUND);
				code = Reply.CD_UNKNOWN;
				delayResponse = Base.get(attributes, "X-Delay", null) != BaseObject.FALSE;
			}
				break;
			case Reply.CD_EXCEPTION : {
				this.headAppend(SocketHandler.HD_EXCEPTION);
				code = Reply.CD_EXCEPTION;
			}
				break;
			case Reply.CD_BUSY : {
				this.headAppend(SocketHandler.HD_BUSY);
				code = Reply.CD_BUSY;
				this.doKeepAlive = false;
				delayResponse = true;
			}
				break;
			case Reply.CD_BADQUERY : {
				this.headAppend(SocketHandler.HD_BADQUERY);
				code = Reply.CD_BADQUERY;
				this.doKeepAlive = false;
				// reply.setAttribute("Content-Length", "0");
				// bodylessResponse = true;
				// // suppressContentLength = true;
				delayResponse = Base.get(attributes, "X-Delay", null) != BaseObject.FALSE;
			}
				break;
			case Reply.CD_BADMETHOD : {
				this.headAppend(SocketHandler.HD_BADMETHOD);
				code = Reply.CD_BADMETHOD;
				bodylessResponse = true;
				suppressContentLength = true;
				delayResponse = Base.get(attributes, "X-Delay", null) != BaseObject.FALSE;
			}
				break;
			case Reply.CD_BADRANGE : {
				this.headAppend(SocketHandler.HD_BADRANGE);
				code = Reply.CD_BADRANGE;
				bodylessResponse = true;
				suppressContentLength = true;
				delayResponse = true;
			}
				break;
			case Reply.CD_CONFLICT : {
				this.headAppend(SocketHandler.HD_CONFLICT);
				code = Reply.CD_CONFLICT;
			}
				break;
			case Reply.CD_UNSUPPORTED_FORMAT : {
				this.headAppend(SocketHandler.HD_UNSUPPORTED);
				code = Reply.CD_UNSUPPORTED_FORMAT;
			}
				break;
			case Reply.CD_UNIMPLEMENTED : {
				this.headAppend(SocketHandler.HD_UNIMPLEMENTED);
				code = Reply.CD_UNIMPLEMENTED;
				delayResponse = Base.get(attributes, "X-Delay", null) != BaseObject.FALSE;
			}
				break;
			case Reply.CD_FAILED_PRECONDITION : {
				this.headAppend(SocketHandler.HD_FAILED);
				code = Reply.CD_FAILED_PRECONDITION;
			}
				break;
			case Reply.CD_LOCKED : {
				this.headAppend(SocketHandler.HD_LOCKED);
				code = Reply.CD_LOCKED;
			}
				break;
			case Reply.CD_MULTISTATUS : {
				this.headAppend(SocketHandler.HD_MULTISTATUS);
				code = Reply.CD_MULTISTATUS;
			}
				break;
			case Reply.CD_CREATED : {
				this.headAppend(SocketHandler.HD_CREATED);
				code = Reply.CD_CREATED;
			}
				break;
			default : {
				code = reply.getCode();
				this.headAppend(code);
				this.headAppend(SocketHandler.BYTES_UNKNOWN_TITLE_PREFFIX);
				this.headAppend(reply.getTitle());
				this.headAppend(SocketHandler.BYTES_UNKNOWN_TITLE_SUFFIX);
				/** obsolete <code>
				this.headAppend( SocketHandler.BYTES_UNKNOWN_TITLE_PREFFIX );
				this.headAppend( code );
				this.headAppend( SocketHandler.BYTES_UNKNOWN_TITLE_SUFFIX );
				</code> */
			}
		}
		if (!Base.getBoolean(attributes, "Date", false)) {
			this.date.setTime(Engine.fastTime());
			reply.setAttribute("Date", this.dateFormatHeader.format(this.date));
		}
		if (reply.isPrivate()) {
			final String rSessionID = reply.getSessionID();
			final String rUserID = reply.getUserID();
			final String qUserID = query.getUserID();
			if (rSessionID != null) {
				this.headAppend("Set-Cookie: SID=");
				this.headAppend(rSessionID);
				this.headAppend("; path=/\r\n");
			}
			if (rUserID != null && (qUserID == null || !rUserID.equals(qUserID))) {
				this.headAppend("Set-Cookie: UID-s=");
				this.headAppend(rUserID);

				/** FIXME mmsource */
				if (false) {
					this.headAppend("; path=/\r\n");
				} else {
					final String append;
					{
						final String domain = reply.getSourceAddress();
						append = domain != null && query.getTarget().endsWith('.' + domain)
							? domain
							: null;
					}
					this.date.setTime(Engine.fastTime() + SocketHandler.UID_EXPIRATION);
					if (append == null) {
						this.headAppend("; path=/\r\nSet-Cookie: UID=");
						this.headAppend(rUserID);
						this.headAppend("; path=/; expires=");
					} else {
						this.headAppend("; path=/; domain=.");
						this.headAppend(append);
						this.headAppend("\r\nSet-Cookie: UID=");
						this.headAppend(rUserID);
						this.headAppend("; path=/; domain=.");
						this.headAppend(append);
						this.headAppend("; expires=");
					}
					this.headAppend(this.dateFormatCookie.format(this.date));
					this.headAppendCRLF();
				}
			}
		}

		if (delayResponse) {
			this.socket.setTransferDescription(TransferDescription.IDLE_UNLIMITED);
			this.doCompress = false;
		} else {
			final TransferDescription description = this.socket.getTransferDescription();
			if (description.isReplaceable(null)) {
				final Object transferClass = Base.getJava(attributes, "Transfer-Class", null);
				if (transferClass != null && transferClass instanceof TransferDescription) {
					final TransferDescription substitute = (TransferDescription) transferClass;
					if (description.isReplaceable(substitute)) {
						this.socket.setTransferDescription(substitute);
					} else {
						reply.setAttribute("Transfer-Class", Base.forUnknown(description));
					}
				}
			} else {
				reply.setAttribute("Transfer-Class", Base.forUnknown(description));
			}
			checkCompress : if (this.doCompress) {
				if (this.teGzip) {
					break checkCompress;
				}
				this.doCompress = false;
				final String acceptEncoding = Base.getString(this.qHeaders, "Accept-Encoding", null);
				if (acceptEncoding != null) {
					if (acceptEncoding.length() == 4 && acceptEncoding.equals("gzip")) {
						this.doCompress = true;
						break checkCompress;
					}
					for (final StringTokenizer tokenizer = new StringTokenizer(acceptEncoding, ","); tokenizer.hasMoreTokens();) {
						final String next = tokenizer.nextToken().trim();
						if (next.length() == 4 && next.equals("gzip")) {
							this.doCompress = true;
							break checkCompress;
						}
					}
				}
			}
		}

		final long contentLength = Base.getLong(attributes, "Content-Length", -1);

		bodylessResponse = bodylessResponse || contentLength == 0 || code == Reply.CD_EMPTY || code / 100 == ReplyAnswer.TP_RD || query.getVerbOriginal().equals("HEAD");

		if (bodylessResponse) {
			if (this.doCompress) {
				this.doCompress = false;
			}
			/** kept as it was */
			// this.doKeepAlive =
			/** false by default */
			// this.doChunked = false;
		} else //
		if (this.doCompress) {
			/** kept as it was */
			// this.doKeepAlive =
			this.doChunked = true;
			suppressContentLength = true;
		} else //
		if (this.http11) {
			/** kept as it was */
			// this.doKeepAlive =
			if (contentLength == -1) {
				this.doChunked = true;
				// no content length, but this will compare quicker later
				suppressContentLength = true;
			}
		} else {
			this.doKeepAlive = this.doKeepAlive && contentLength > 0;
			this.doChunked = false;
		}

		this.headAppend(
				this.doCompress
					? this.doChunked
						? this.doKeepAlive
							? this.teGzip
								// GZ CH KA 11
								? SocketHandler.HEADER_TE_GZIP_CHUNKED_ALIVE
								// GZ CH KA 10
								: SocketHandler.HEADER_CE_GZIP_CHUNKED_ALIVE
							: this.teGzip
								// GZ CH CL 11
								? SocketHandler.HEADER_TE_GZIP_CHUNKED_CLOSE
								// GZ CH CL 10
								: SocketHandler.HEADER_CE_GZIP_CHUNKED_CLOSE
						: this.doKeepAlive
							? this.teGzip
								// GZ RW KA 11
								? SocketHandler.HEADER_TE_GZIP_ALIVE
								// GZ RW KA 10
								: SocketHandler.HEADER_CE_GZIP_ALIVE
							: this.teGzip
								// GZ RW CL 11
								? SocketHandler.HEADER_TE_GZIP_CLOSE
								// GZ RW CL 10
								: SocketHandler.HEADER_CE_GZIP_CLOSE
					: this.doChunked
						? this.doKeepAlive
							// PL CH KA
							? SocketHandler.HEADER_TE_CHUNKED_ALIVE
							// PL CH CL
							: SocketHandler.HEADER_TE_CHUNKED_CLOSE
						: this.doKeepAlive
							// PL RW KA
							? SocketHandler.HEADER_CN_IDENTITY_ALIVE
							// PL RW CL
							: SocketHandler.HEADER_CN_IDENTITY_CLOSE);

		if (Report.MODE_DEBUG || Report.MODE_ASSERT) {
			reply.addAttribute("X-Debug-Origin", reply.getEventTypeId());
		}
		headerLoop : for (final Iterator<String> iterator = Base.keys(attributes); iterator.hasNext();) {
			final String key = iterator.next();
			final int len = key.length();
			if (len == 0) {
				continue headerLoop;
			}
			final BaseObject value = attributes.baseGet(key, null);
			/** value == null is unreal, cause we kind of iterating through existing keys... */
			if (value == null || value == BaseObject.UNDEFINED || value == BaseObject.NULL) {
				continue headerLoop;
			}
			switch (key.charAt(0)) {
				case 'T' : {
					if (5 == len && "Title".equals(key)) {
						continue headerLoop;
					}
					if (17 == len && "Transfer-Encoding".equals(key)) {
						continue headerLoop;
					}
					break;
				}
				case 'S' : {
					if (6 == len && "Server".equals(key)) {
						continue headerLoop;
					}
					if (7 == len && "Subject".equals(key)) {
						continue headerLoop;
					}
					/** <code>
					if (10 == len && "Set-Cookie".equals( key )) {
						continue headerLoop;
					}
					</code> */
					break;
				}
				case 'C' : {
					if (10 == len && "Connection".equals(key)) {
						continue headerLoop;
					}
					if (suppressContentLength && 14 == len && "Content-Length".equals(key)) {
						continue headerLoop;
					}
					if (19 == len && "Content-Disposition".equals(key) && String.valueOf(value).startsWith("form-data")) {
						continue headerLoop;
					}
					break;
				}
				case 'K' : {
					if (10 == len && "Keep-Alive".equals(key)) {
						continue headerLoop;
					}
					break;
				}
				case 'A' : {
					if (13 == len && "Accept-Ranges".equals(key)) {
						continue headerLoop;
					}
					break;
				}
				default :
			}
			this.addHeader(key, value);
		}
		this.headAppendCRLF();
		this.reuse |= SocketHandler.RU_04_BDRT;
		return !bodylessResponse;
	}

	@Override
	public final void abort(final String reason) {

		final TransferSocket socket = this.socket;
		if (socket != null) {
			this.socket = null;
			socket.abort(reason);
		}
		if ((this.reuse & SocketHandler.RU_01_QDRT) != 0) {
			this.reuse &= ~SocketHandler.RU_01_QDRT;
			this.resetQueryParser();
		}
		if ((this.reuse & SocketHandler.RU_02_RDRT) != 0) {
			this.reuse &= ~SocketHandler.RU_02_RDRT;
			this.resetResponseBuilder();
		}
		switch (this.reuse) {
			case RU_08_OPEN :
				if (this.rBuffer.length > SocketHandler.RBUFF_RSET) {
					this.reuse = SocketHandler.RU_20_UREF;
					this.rBuffer = new byte[SocketHandler.RBUFF_INIT];
				}
				this.reuse = SocketHandler.RU_10_INIT;
				HandlerQueue.reuseParser(this, this.queueIndex);
				return;

			case RU_10_INIT :
				// throw new IllegalStateException("Parser state is INIT, parser: " + this);
				HandlerQueue.reuseParser(this, this.queueIndex);
				return;

			default :
				if ((this.reuse & SocketHandler.RU_08_OPEN) != 0) {
					this.reuse &= ~SocketHandler.RU_08_OPEN;
				}
				return;
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

		switch (this.qMode) {
			case MD_HEADER :
				if (i == '\r') {
					return true;
				}
				if (i == '\n') {
					if (this.qBufSize == 0) {
						if (this.qContentLength > 0) {
							this.collector = this.prepareCollector();
							this.setMode(SocketHandler.MD_LOAD_BODY, this.qContentLength);
							return true;
						}
						if (this.qChunked) {
							this.collector = this.prepareCollector();
							this.setMode(SocketHandler.MD_CHUNKED_HEADER, SocketHandler.MAX_CHUNK_HEADER_SIZE);
							return true;
						}
						return this.onDoneRead(null);
					}
					HttpProtocol.LOG.event(
							this.protocolName,
							"INFO",
							"BAD REQUEST: " + "buffer is not empty on last header, bufferSize=" + this.qBufSize + ", remote=" + this.sourcePeerIdentity);
					return this.onError4xxBadRequest(SocketHandler.RESPONSE_400);
				}
				if (i == ':') {
					this.qCurrentHeader = this.setMode(SocketHandler.MD_VALUE, SocketHandler.MAX_VALUE_LENGTH);
					return true;
				}
				return this.append(i);
			case MD_VALUE :
				if (i == '\r') {
					return true;
				}
				if (i == ' ' && this.qBufSize == 0) {
					return true;
				}
				if (i == '\n') {
					if (this.qHeadersSize == SocketHandler.MAX_HEADERS) {
						HttpProtocol.LOG.event(
								this.protocolName,
								"INFO",
								"BAD REQUEST: " + "maximum headers reached, max=" + SocketHandler.MAX_HEADERS + ", code=" + i + ", collected=" + this.setMode(0, 0) + ", remote="
										+ this.sourcePeerIdentity);
						return this.onError4xxBadRequest(SocketHandler.RESPONSE_431);
					}
					this.onHeader(this.qCurrentHeader, this.setMode(SocketHandler.MD_HEADER, SocketHandler.MAX_HEADER_LENGTH));
					this.qCurrentHeader = "";
					return true;
				}
				return this.append(i);
			case MD_COMMAND :
				if (i == ' ') {
					this.qVerb = this.setMode(SocketHandler.MD_PATH, SocketHandler.MAX_PATH_LENGTH);
					this.qPoints = -1;
					return true;
				}
				if (i >= 0 && i < ' ' || i > 127) {
					HttpProtocol.LOG.event(
							this.protocolName,
							"INFO",
							"BAD REQUEST: " + "illegal character in command, code=" + i + ", collected=" + this.setMode(0, 0) + ", remote=" + this.sourcePeerIdentity);
					return this.onError4xxBadRequest(SocketHandler.RESPONSE_400);
				}
				return this.append(SocketHandler.UPCASE_XLAT[i]);
			case MD_PATH :
				switch (i) {
					case '?' :
						if (this.qVerb == null) {
							HttpProtocol.LOG.event(
									this.protocolName,
									"INFO",
									"BAD REQUEST: " + "illegal path encountered, collected=" + this.setMode(0, 0) + ", remote=" + this.sourcePeerIdentity);
							return this.onError4xxBadRequest(SocketHandler.RESPONSE_400);
						}
						this.qPath = this.setMode(SocketHandler.MD_QUERY, SocketHandler.MAX_QUERY_LENGTH);
						return true;
					case ' ' :
						if (this.qVerb == null) {
							HttpProtocol.LOG.event(
									this.protocolName,
									"INFO",
									"BAD REQUEST: " + "illegal path encountered, collected=" + this.setMode(0, 0) + ", remote=" + this.sourcePeerIdentity);
							return this.onError4xxBadRequest(SocketHandler.RESPONSE_400);
						}
						this.qPath = this.setMode(SocketHandler.MD_PROTOCOL, SocketHandler.MAX_PROTOCOL_LENGTH);
						return true;
					case '/' :
						if (this.qPoints == 0) {
							this.qVerb = null;
						} else {
							this.qPoints = 2;
						}
						break;
					case '\\' :
						this.qVerb = null;
						break;
					case '.' :
						if (this.qPoints > 0) {
							--this.qPoints;
						}
						break;
					default :
						this.qPoints = -1;
				}
				if (i >= 0 && i < ' ') {
					HttpProtocol.LOG.event(
							this.protocolName,
							"INFO",
							"BAD REQUEST: " + "Illegal characters in path, code=" + i + ", collected=" + this.setMode(0, 0) + ", remote=" + this.sourcePeerIdentity);
					return this.onError4xxBadRequest(SocketHandler.RESPONSE_400);
				}
				return this.append(i);
			case MD_QUERY :
				if (i == '&' || i == ' ') {
					if (this.qQueryPosition > this.qArgumentPosition) {
						final String argument = new String(this.qBuffer, this.qArgumentPosition, this.qQueryPosition - this.qArgumentPosition);
						if (this.qArguments == null) {
							this.qArguments = new ArrayList<>();
						}
						this.qArguments.add(argument);
						final int pos = argument.indexOf('=');
						if (pos != -1) {
							final String key;
							final String val;
							key = HttpProtocol.urlDecode(argument.substring(0, pos), this.qInputCharset);
							val = HttpProtocol.urlDecode(argument.substring(pos + 1), this.qInputCharset);
							if (key.length() == 4 && key.charAt(0) == '_') {
								if (key.equals("_ic_")) {
									this.qInputCharset = Charset.forName(val);
									this.qHeaders.baseDefine("Content-Charset", val, BaseProperty.ATTRS_MASK_WED);
								} else //
								if (key.equals("_cd_")) {
									this.qVerb = val;
								} else //
								if (key.equals("_ht_")) {
									this.qHost = val;
								} else //
								if (key.equals("_vb_")) {
									this.qVerb = val;
								} else //
								if (key.equals("_ri_")) {
									this.qPath = this.qPath + val;
								}
							}
							final BaseObject o;
							if (this.qParameters == null) {
								o = BaseObject.UNDEFINED;
								this.qParameters = new BaseNativeObject();
							} else {
								o = this.qParameters.baseGet(key, BaseObject.UNDEFINED);
								assert o != null : "NULL java value";
							}
							if (o == BaseObject.UNDEFINED) {
								this.qParameters.baseDefine(key, val, BaseProperty.ATTRS_MASK_WED);
							} else {
								if (o instanceof MultipleList) {
									((MultipleList) o).add(Base.forString(val));
								} else {
									final MultipleList list = new MultipleList();
									list.add(o);
									list.add(Base.forString(val));
									this.qParameters.baseDefine(key, list, BaseProperty.ATTRS_MASK_WED);
								}
							}
						}
					}
					if (i == ' ') {
						this.qQueryString = this.setMode(SocketHandler.MD_PROTOCOL, SocketHandler.MAX_PROTOCOL_LENGTH);
						return true;
					}
					this.qArgumentPosition = this.qQueryPosition + 1;
				}
				if (i >= 0 && i < ' ') {
					HttpProtocol.LOG.event(
							this.protocolName,
							"INFO",
							"BAD REQUEST: " + "Illegal characters in query string, code=" + i + ", collected=" + this.setMode(0, 0) + ", remote=" + this.sourcePeerIdentity);
					return this.onError4xxBadRequest(SocketHandler.RESPONSE_400);
				}
				this.qQueryPosition++;
				return this.append(i);
			case MD_PROTOCOL :
				if (i == '\r') {
					return true;
				}
				if (i == '\n') {
					this.qProtocol = this.setMode(SocketHandler.MD_HEADER, SocketHandler.MAX_HEADER_LENGTH);
					this.http11 = this.qProtocol.endsWith("/1.1");
					return true;
				}
				if (i >= 0 && i < ' ') {
					HttpProtocol.LOG.event(
							this.protocolName,
							"INFO",
							"BAD REQUEST: " + "Illegal characters in protocol name, code=" + i + ", collected=" + this.setMode(0, 0) + ", remote=" + this.sourcePeerIdentity);
					return this.onError4xxBadRequest(SocketHandler.RESPONSE_400);
				}
				return this.append(SocketHandler.UPCASE_XLAT[i]);
			case MD_LOAD_BODY :
				this.collector.getTarget().absorb(i);
				if (--this.qLengthRemaining == 0) {
					return this.onDoneRead(this.collector.toBinary());
				}
				return true;
			case MD_CHUNKED_HEADER :
				if (i == '\r') {
					return true;
				}
				if (i == '\n') {
					final String chHeader = new String(this.qBuffer, 0, this.qBufSize);
					this.qBufSize = 0;
					final int length;
					try {
						length = Integer.parseInt(chHeader, 10);
					} catch (final Throwable t) {
						HttpProtocol.LOG.event(this.protocolName, "INFO", "BAD REQUEST: " + "unparseable chunk size" + ", remote=" + this.sourcePeerIdentity);
						return this.onError4xxBadRequest(SocketHandler.RESPONSE_400);
					}
					if (length == 0) {
						return this.onDoneRead(this.collector.toBinary());
					}
					if (length > SocketHandler.MAX_CHUNK_SIZE) {
						HttpProtocol.LOG.event(
								this.protocolName,
								"INFO",
								"BAD REQUEST: " + "chunk is larger than allowed, max=" + SocketHandler.MAX_CHUNK_SIZE + ", length=" + length + ", remote="
										+ this.sourcePeerIdentity);
						return this.onError4xxBadRequest(SocketHandler.RESPONSE_400);
					}
					this.setMode(SocketHandler.MD_CHUNKED_BLOCK, length);
					return true;
				}
				if (i <= ' ') {
					HttpProtocol.LOG.event(this.protocolName, "INFO", "BAD REQUEST: " + "illegal character in chunk header, code=" + i + ", remote=" + this.sourcePeerIdentity);
					return this.onError4xxBadRequest(SocketHandler.RESPONSE_400);
				}
				return this.append(i);
			case MD_CHUNKED_BLOCK : {
				this.collector.getTarget().absorb((char) i);
				if (--this.qLengthRemaining == 0) {
					this.setMode(SocketHandler.MD_CHUNKED_HEADER, SocketHandler.MAX_CHUNK_HEADER_SIZE);
				}
				return true;
			}
			case MD_FIRSTBYTE :
				if (i == '\r' || i == '\n' || i == ' ') {
					return --this.qLengthRemaining > 0;
				}
				this.reuse |= SocketHandler.RU_01_QDRT;
				this.qHeaders = new BaseNativeObjectCaseInsencetive();
				this.qMode = SocketHandler.MD_COMMAND;
				if (i <= ' ') {
					HttpProtocol.LOG.event(this.protocolName, "INFO", "BAD REQUEST: " + "illegal character in first byte, code=" + i + ", remote=" + this.sourcePeerIdentity);
					return this.onError4xxBadRequest(SocketHandler.RESPONSE_400);
				}
				return this.append(
						i >= 128
							? i
							: SocketHandler.UPCASE_XLAT[i]);
			default :
				this.abort("Invalid parser state: " + this.qMode);
				return false;
		}
	}

	@Override
	public final boolean absorbArray(final byte[] bytes, final int off, final int length) {

		if (this.qMode == SocketHandler.MD_LOAD_BODY && this.qLengthRemaining >= length) {
			this.qLengthRemaining -= length;
			this.collector.getTarget().absorbArray(bytes, off, length);
			if (this.qLengthRemaining == 0) {
				return this.onDoneRead(this.collector.toBinary());
			}
			return true;
		}
		if (this.qMode == SocketHandler.MD_CHUNKED_BLOCK && this.qLengthRemaining >= length) {
			this.qLengthRemaining -= length;
			this.collector.getTarget().absorbArray(bytes, off, length);
			if (this.qLengthRemaining == 0) {
				this.setMode(SocketHandler.MD_CHUNKED_HEADER, SocketHandler.MAX_CHUNK_HEADER_SIZE);
			}
			return true;
		}
		for (int i = 0; i < length; ++i) {
			if (!this.absorb(bytes[off + i] & 0xFF)) {
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
					if (this.qMode == SocketHandler.MD_LOAD_BODY && this.qLengthRemaining >= length) {
						this.qLengthRemaining -= length;
						this.collector.getTarget().absorbBuffer(buffer);
						if (this.qLengthRemaining == 0) {
							return this.onDoneRead(this.collector.toBinary());
						}
						return true;
					}
					if (this.qMode == SocketHandler.MD_CHUNKED_BLOCK && this.qLengthRemaining >= length) {
						this.qLengthRemaining -= length;
						this.collector.getTarget().absorbBuffer(buffer);
						if (this.qLengthRemaining == 0) {
							this.setMode(SocketHandler.MD_CHUNKED_HEADER, SocketHandler.MAX_CHUNK_HEADER_SIZE);
						}
						return true;
					}
					if (!this.absorb(buffer.next())) {
						return false;
					}
				}
			}
		}
		return true;
	}

	@Override
	public final boolean absorbNio(final ByteBuffer buffer) {

		for (;;) {
			final int remaining = buffer.remaining();
			if (remaining == 0) {
				return true;
			}
			if (this.qMode == SocketHandler.MD_LOAD_BODY) {
				if (this.qLengthRemaining >= remaining) {
					this.qLengthRemaining -= remaining;
					this.collector.getTarget().absorbNio(buffer);
					if (this.qLengthRemaining == 0) {
						return this.onDoneRead(this.collector.toBinary());
					}
					return true;
				}
				final int limit = remaining - this.qLengthRemaining;
				buffer.limit(buffer.position() + limit);
				this.qLengthRemaining -= limit;
				this.collector.getTarget().absorbNio(buffer);
				buffer.limit(buffer.position() + remaining - limit);
				if (this.qLengthRemaining == 0) {
					return this.onDoneRead(this.collector.toBinary());
				}
				return true;
			}
			if (this.qMode == SocketHandler.MD_CHUNKED_BLOCK) {
				if (this.qLengthRemaining >= remaining) {
					this.qLengthRemaining -= remaining;
					this.collector.getTarget().absorbNio(buffer);
					if (this.qLengthRemaining == 0) {
						this.setMode(SocketHandler.MD_CHUNKED_HEADER, SocketHandler.MAX_CHUNK_HEADER_SIZE);
					}
					continue;
				}
				final int limit = remaining - this.qLengthRemaining;
				buffer.limit(buffer.position() + limit);
				this.qLengthRemaining -= limit;
				this.collector.getTarget().absorbNio(buffer);
				buffer.limit(buffer.position() + remaining - limit);
				if (this.qLengthRemaining == 0) {
					this.setMode(SocketHandler.MD_CHUNKED_HEADER, SocketHandler.MAX_CHUNK_HEADER_SIZE);
				}
				continue;
			}
			{
				if (!this.absorb(buffer.get() & 0xFF)) {
					return false;
				}
			}
		}
	}

	@Override
	public final Boolean apply(final ReplyAnswer reply) {

		final int responseCode = reply.getCode();
		if (responseCode == Reply.CD_PROCESSING) {
			/** parser is still to be reused! */
			return Boolean.TRUE;
		}
		if (responseCode == Reply.CD_RAW_PROTOCOL) {

			final TransferSocket proxy;
			proxy : {
				final Object object = reply.getObject();
				if (object instanceof TransferSocket) {
					proxy = (TransferSocket) object;
					break proxy;
				}
				if (object instanceof Value<?>) {
					final Object replacement = ((Value<?>) object).baseValue();
					if (replacement != null && replacement != object && replacement instanceof TransferSocket) {
						proxy = (TransferSocket) replacement;
						break proxy;
					}
				}
				if (object instanceof BaseObject) {
					final Object replacement = ((BaseObject) object).baseValue();
					if (replacement != null && replacement != object && replacement instanceof TransferSocket) {
						proxy = (TransferSocket) replacement;
						break proxy;
					}
				}
				proxy = Produce.object(TransferSocket.class, null, null, object);
				if (proxy == null) {
					throw new IllegalArgumentException("Socket is expected");
				}
			}
			final TransferSocket socket = this.socket;
			this.socket = null;

			proxy.getSource().connectTarget(socket.getTarget());
			socket.getSource().connectTarget(proxy.getTarget());

			final ServeRequest query = this.query;
			final String sourceAddress = query.getSourceAddress();
			final String sourceAddressExact = query.getSourceAddressExact();
			HttpProtocol.LOG.event(//
					this.protocolName,
					"UPGRADE-RAW",
					reply.getCode() + "\t" //
							+ this.qProtocol + '\t' //
							+ this.qVerb + '\t' //
							+ (sourceAddress != null && sourceAddress.equals(sourceAddressExact)
								? sourceAddress
								: sourceAddress + ',' + sourceAddressExact)
							+ '\t'//
							+ (reply.getDate() - query.getDate()) + '\t'//
							+ (System.currentTimeMillis() - query.getDate()) + '\t' //
							+ this.sourcePeerIdentity + '\t'//
							+ query.getUrl()//
			);
			this.close();
			return Boolean.TRUE;
		}

		this.reuse |= SocketHandler.RU_02_RDRT;
		this.rBufferLength = 0;
		this.rBufferPosition = 0;
		// final ServeRequest query = this.query;
		this.reply = reply;
		if (this.http11) {
			SocketHandler.stHttp11++;
		} else {
			SocketHandler.stHttp10++;
		}
		if (this.http11) {
			this.headAppend(SocketHandler.PREFFIX_HTTP11);
		} else {
			this.headAppend(SocketHandler.PREFFIX_HTTP10);
		}
		if (this.configuration.ignoreKeepAlive) {
			this.doKeepAlive = false;
		} else {
			this.doKeepAlive = this.http11
				? !Base.getString(this.qHeaders, "Connection", "").regionMatches(true, 0, "close", 0, 5)
				: Base.getString(this.qHeaders, "Connection", "").regionMatches(true, 0, "keep-alive", 0, 10);
		}
		final long written;
		try {
			written = this.render(reply);
		} catch (final IOException e) {
			HttpProtocol.LOG.event(this.protocolName, "RENDER-IOEXCEPTION", Format.Throwable.toText(e));
			this.abort("IOException");
			return Boolean.FALSE;
		} catch (final NullPointerException e) {
			HttpProtocol.LOG.event(this.protocolName, "RENDER-NULLEXCEPTION", Format.Throwable.toText(e));
			this.abort("NullException");
			return Boolean.FALSE;
		}
		if (written == -1) {
			return Boolean.TRUE;
		}
		return this.executeDone(written);
	}

	@Override
	public final void close() {

		final TransferSocket socket = this.socket;
		if (socket != null) {
			this.socket = null;
			socket.close();
		}
		if ((this.reuse & SocketHandler.RU_01_QDRT) != 0) {
			this.reuse &= ~SocketHandler.RU_01_QDRT;
			this.resetQueryParser();
		}
		if ((this.reuse & SocketHandler.RU_02_RDRT) != 0) {
			this.reuse &= ~SocketHandler.RU_02_RDRT;
			this.resetResponseBuilder();
		}
		switch (this.reuse) {
			case RU_08_OPEN :
				if (this.rBuffer.length > SocketHandler.RBUFF_RSET) {
					this.reuse = SocketHandler.RU_20_UREF;
					this.rBuffer = new byte[SocketHandler.RBUFF_INIT];
				}
				this.reuse = SocketHandler.RU_10_INIT;
				HandlerQueue.reuseParser(this, this.queueIndex);
				return;

			case RU_10_INIT :
				// throw new IllegalStateException("Parser state is INIT, parser: " + this);
				HandlerQueue.reuseParser(this, this.queueIndex);
				return;

			default :
				if ((this.reuse & SocketHandler.RU_08_OPEN) != 0) {
					this.reuse &= ~SocketHandler.RU_08_OPEN;
				}
				return;
		}
	}

	@Override
	public final void destroy() {

		switch (this.reuse) {
			case RU_04_BDRT :
				if (this.rBuffer.length > SocketHandler.RBUFF_RSET) {
					this.rBuffer = new byte[SocketHandler.RBUFF_INIT];
				}
				this.reuse = SocketHandler.RU_10_INIT;
				HandlerQueue.reuseParser(this, this.queueIndex);
				return;

			case RU_10_INIT :
				// throw new IllegalStateException("Parser state is INIT, parser: " + this);
				HandlerQueue.reuseParser(this, this.queueIndex);
				return;

			default :
				if ((this.reuse & SocketHandler.RU_04_BDRT) != 0) {
					this.reuse ^= SocketHandler.RU_04_BDRT;
				}
				return;
		}
	}

	@Override
	public <A, R> boolean enqueueAction(final ExecProcess ctx, final Function<A, R> function, final A argument) {

		Act.launch(ctx, function, argument);
		return true;
	}

	@Override
	public final void force() {

		// ignore
	}

	@Override
	public final String[] getAcceptTypes() {

		return null;
	}

	@Override
	public MessageDigest getMessageDigest() {

		final MessageDigest digest = Engine.getMessageDigestInstance();
		digest.update(this.rBuffer, this.rBufferPosition, this.rBufferLength);
		return digest;
	}

	@Override
	public final Object getObject() {

		return this.reply.getObject();
	}

	@Override
	public final Class<?> getObjectClass() {

		return this.reply.getObjectClass();
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
	public final TransferCollector setResultType(final String contentType) throws IOException {

		assert contentType != null : "NULL contentType";
		assert contentType.indexOf('/') != -1 : "Invalid content type: " + contentType;
		this.reply.setAttribute("Server", SocketHandler.A_SERVER_SERIALIZATION);
		this.reply.setAttribute("Content-Type", contentType);
		final boolean contentFollows = this.startRender(this.reply);
		if (!this.socket.getTarget().absorbBuffer(this)) {
			this.close();
			return TransferCollector.NUL_COLLECTOR;
		}
		if (!contentFollows) {
			return TransferCollector.NUL_COLLECTOR;
		}
		if (this.doChunked) {
			this.collector.startChunking(2048, 65536);
		}
		return this.collector;
	}

	@Override
	public final TransferCopier toBinary() {

		final int position = this.rBufferPosition;
		final int remaining = this.rBufferLength - position;

		if ((this.reuse & SocketHandler.RU_20_UREF) != 0) {
			/** Wrap is used cause it supposed to be called internally and sequentially */
			return Transfer.wrapCopier(this.rBuffer, position, remaining);
		}

		try {
			return Transfer.createCopier(this.rBuffer, position, remaining);
		} finally {
			this.destroy();
		}
	}

	@Override
	public final byte[] toDirectArray() {

		if (this.rBufferPosition == 0 && this.rBufferLength == this.rBuffer.length) {
			this.rBufferPosition = this.rBufferLength;
			/** FIXME: reallocate buffer? **/
			this.reuse |= SocketHandler.RU_20_UREF;
			return this.rBuffer;
		}
		final int remaining = this.rBufferLength - this.rBufferPosition;
		final byte[] result = new byte[remaining];
		System.arraycopy(this.rBuffer, this.rBufferPosition, result, 0, remaining);
		this.rBufferPosition = this.rBufferLength;
		this.destroy();
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

		return "[HttpServerParser(" + System.identityHashCode(this) + ", bufRemaining=" + this.remaining() + ")]";
	}

	@Override
	public final String toString(final Charset charset) {

		final int remaining = this.rBufferLength - this.rBufferPosition;
		if (remaining > 0) {
			return new String(this.rBuffer, this.rBufferPosition, remaining, charset);
		}
		return "";
	}

	@Override
	public final String toString(final String charset) throws UnsupportedEncodingException {

		final int remaining = this.rBufferLength - this.rBufferPosition;
		if (remaining > 0) {
			return new String(this.rBuffer, this.rBufferPosition, remaining, charset);
		}
		return "";
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

	/** returns TRUE when connection should be kept for new replies<br/>
	 * returns FALSE when connection is not to be held */
	@SuppressWarnings("resource")
	final Boolean executeDone(final long written) {

		final ServeRequest query = this.query;
		final long length = written + this.rBufferLength;
		final String logLine;
		{
			final ReplyAnswer reply = this.reply;
			final String sourceAddress = query.getSourceAddress();
			final String sourceAddressExact = query.getSourceAddressExact();
			logLine = reply.getCode() //
					+ "\t" + this.qProtocol //
					+ '\t' + this.qVerb //
					+ '\t' + (sourceAddress != null && sourceAddress.equals(sourceAddressExact)
						? sourceAddress
						: sourceAddress + ',' + sourceAddressExact) //
					+ '\t' + (this.doKeepAlive
						? "K"
						: "k")
					+ (this.doCompress
						? this.doChunked
							? "GC"
							: "Gc"
						: this.doChunked
							? "gC"
							: "gc") //
					+ '\t' + length //
					+ '\t' + (reply.getDate() - query.getDate()) //
					+ '\t' + (System.currentTimeMillis() - query.getDate()) //
					+ '\t' + this.sourcePeerIdentity //
					+ '\t' + query.getUrl()//
			;
		}

		try {
			final Object execute = Base.getJava(this.qHeaders, "execute", null);
			final TransferSocket socket = this.socket;
			if (socket == null || !socket.isOpen()) {
				if (execute != null) {
					@SuppressWarnings("unchecked")
					final Function<ServeRequest, Boolean> function = (Function<ServeRequest, Boolean>) execute;
					final ExecProcess process = Exec.currentProcess();
					Act.launch(process, function, query);
				}
				this.socket = null;
				this.abort("Underlying Socket is Closed");
				return Boolean.FALSE;
			}
			final TransferTarget target = socket.getTarget();
			if (execute != null) {
				@SuppressWarnings("unchecked")
				final Function<ServeRequest, Boolean> function = (Function<ServeRequest, Boolean>) execute;
				final ExecProcess process = Exec.currentProcess();
				if (!target.enqueueAction(process, function, query)) {
					Act.launch(process, function, query);
					this.abort("Target is refusing to enqueue an action");
					return Boolean.FALSE;
				}
			}
			if (this.doKeepAlive) {
				target.force();
				if (target.enqueueAction(
						HttpProtocol.CTX, //
						SocketHandler.KEEP_ALIVE_CONNECTOR,
						new KeepAliveParserConnector(socket, this.configuration))) {
					/** socket disconnected */
					this.socket = null;
					/** parser may be reused! */
					this.close();
					return Boolean.TRUE;
				}
				this.abort("Target is refusing to enqueue an action");
				return Boolean.FALSE;
			}
			this.close();
			return Boolean.FALSE;
		} catch (final Throwable t) {
			t.printStackTrace();
			this.abort("Throwable");
			return Boolean.FALSE;
		} finally {
			HttpProtocol.LOG.event(this.protocolName, "RESPONSE", logLine);
			this.resetResponseBuilder();
		}
	}

	final void headAppend(long l) {

		if (l == Long.MIN_VALUE) {
			this.headAppend(SocketHandler.MIN_LONG);
			return;
		}
		final int appendedLength = l < 0
			? SocketHandler.stringSizeOfLong(-l) + 1
			: SocketHandler.stringSizeOfLong(l);
		final int spaceNeeded = this.rBufferLength + appendedLength;
		if (spaceNeeded > this.rBuffer.length) {
			this.headExpand(spaceNeeded);
		}
		long q;
		int r;
		int charPos = spaceNeeded;
		byte sign = 0;
		if (l < 0) {
			sign = '-';
			l = -l;
		}
		// Get 2 digits/iteration using longs until quotient fits into an int
		while (l > Integer.MAX_VALUE) {
			q = l / 100;
			r = (int) (l - q * 100);
			l = q;
			this.rBuffer[--charPos] = SocketHandler.DIGIT_ONES[r];
			this.rBuffer[--charPos] = SocketHandler.DIGIT_TENS[r];
		}
		// Get 2 digits/iteration using ints
		int q2;
		int i2 = (int) l;
		while (i2 >= 65536) {
			q2 = i2 / 100;
			r = i2 - q2 * 100;
			i2 = q2;
			this.rBuffer[--charPos] = SocketHandler.DIGIT_ONES[r];
			this.rBuffer[--charPos] = SocketHandler.DIGIT_TENS[r];
		}
		// Fall thru to fast mode for smaller numbers
		// assert(i2 <= 65536, i2);
		for (;;) {
			q2 = i2 * 52429 >>> 16 + 3;
			r = i2 - q2 * 10;
			this.rBuffer[--charPos] = SocketHandler.DIGITS[r];
			i2 = q2;
			if (i2 == 0) {
				break;
			}
		}
		if (sign != 0) {
			this.rBuffer[--charPos] = sign;
		}
		this.rBufferLength = spaceNeeded;
	}

	final boolean isSocketPresentAndOpen() {

		final TransferSocket socket = this.socket;
		if (socket == null) {
			return false;
		}
		if (socket.isOpen()) {
			return true;
		}
		this.socket = null;
		socket.abort("Underlying Socket is Closed");
		return false;
	}

	final void prepare(final TransferSocket socket, final FlowConfiguration configuration) {

		this.socket = socket;
		this.configuration = configuration;
		this.sourcePeerIdentity = socket.getIdentity();
		if (!configuration.reverseProxied) {
			// mean address is NULL, query will provide peer instead
			// this.sourceMeanAddress =
			this.sourcePeerAddress = socket.getRemoteAddress();
			this.targetPeerAddress = socket.getLocalAddress();
		}
		this.reuse = SocketHandler.RU_00_RSET;
		this.protocolName = configuration.protocolName;
		this.qMode = SocketHandler.MD_FIRSTBYTE;
		this.qLengthRemaining = SocketHandler.MAX_COMMAND_LENGTH;
		this.qBufSize = 0;
		this.qHeadersSize = 0;
		this.qArgumentPosition = 0;
		this.qQueryPosition = 0;
		this.qContentLength = -1;
		this.qChunked = false;
	}

	final TransferCollector prepareCollector() {

		if (this.collector == null) {
			return this.collector = Transfer.createCollector();
		}
		this.collector.reset();
		return this.collector;
	}

}
