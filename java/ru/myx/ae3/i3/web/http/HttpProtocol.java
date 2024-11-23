/*
 * Created on 02.06.2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package ru.myx.ae3.i3.web.http;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.StringTokenizer;

import ru.myx.ae3.Engine;
import ru.myx.ae3.base.BaseNativeObject;
import ru.myx.ae3.base.BaseObject;
import ru.myx.ae3.exec.Exec;
import ru.myx.ae3.exec.ExecProcess;
import ru.myx.ae3.i3.web.http.client.HttpClientStatusProvider;
import ru.myx.ae3.produce.Produce;
import ru.myx.ae3.report.Report;
import ru.myx.ae3.report.ReportReceiver;
import ru.myx.ae3.status.StatusRegistry;

/** @author myx
 * 
 *         To change the template for this generated type comment go to Window>Preferences>Java>Code
 *         Generation>Code and Comments */
public class HttpProtocol {

	static final long STARTED = Engine.fastTime();

	static final ReportReceiver LOG = Report.createReceiver("ae3.proto-http");

	static final ExecProcess CTX = Exec.currentProcess();

	static final String PNAME_HTTP = "HTTP";

	static final String PNAME_HTTPS = "HTTPS";

	private static final int BASE_N = -'0';

	private static final int BASE_A = -'A' + 10;

	private static final int HEX_PRE_0 = '0';

	private static final int HEX_PRE_A = 'A' - 10;

	private static final boolean[] IS_HEX;

	static {
		IS_HEX = new boolean[256];
		for (int i = 255; i >= 0; --i) {
			HttpProtocol.IS_HEX[i] = i >= '0' && i <= '9' || i >= 'a' && i <= 'f' || i >= 'A' && i <= 'F';
		}
	}

	static {
		StatusRegistry.ROOT_REGISTRY.register(new HttpStatusProvider());
		StatusRegistry.ROOT_REGISTRY.register(new HttpClientStatusProvider());
		
		Produce.registerFactory(new MaterializerXUrl("application/x-url-encoded"));
		Produce.registerFactory(new MaterializerXUrl("application/x-www-form-urlencoded"));
		Produce.registerFactory(new FactoryHttpParser());
		Produce.registerFactory(new FactoryHttpsParser());
		Produce.registerFactory(new FactoryQueryStringToProperties());
	}

	/** @param cookies
	 * @return non NULL value */
	static final BaseObject parseCookieString(final String cookies) {

		if (cookies == null) {
			return BaseObject.UNDEFINED;
		}
		final BaseObject result = new BaseNativeObject();
		for (final StringTokenizer st = new StringTokenizer(cookies, ";"); st.hasMoreTokens();) {
			final String current = st.nextToken().trim();
			final int pos = current.indexOf('=');
			if (pos != -1) {
				result.baseDefine(current.substring(0, pos), current.substring(pos + 1));
			}
		}
		return result;
	}

	private static final void toHex(final StringBuilder sb, final int i) {

		final int h1 = (i & 0x0F) >> 0;
		final int h2 = (i & 0xF0) >> 4;
		sb.append('%').append(
				(char) (h2 < 10
					? HttpProtocol.HEX_PRE_0 + h2
					: HttpProtocol.HEX_PRE_A + h2))
				.append(
						(char) (h1 < 10
							? HttpProtocol.HEX_PRE_0 + h1
							: HttpProtocol.HEX_PRE_A + h1));
	}

	static final String urlDecode(final String source, final Charset encoding) {

		if (source == null) {
			return null;
		}
		final byte[] bytes = HttpProtocol.urlDecodeBytes(source.replace('+', ' '));
		if (encoding == null) {
			boolean allASCII = true;
			{
				for (int j = bytes.length - 1; j >= 0; j--) {
					if (bytes[j] < 0) {
						allASCII = false;
						break;
					}
				}
			}
			return allASCII
				? new String(bytes, StandardCharsets.US_ASCII)
				: new String(bytes, StandardCharsets.UTF_8);
		}
		return new String(bytes, encoding);
	}

	/** Extracts URL encoded string
	 *
	 * @param source
	 * @return bytes */
	static final byte[] urlDecodeBytes(final String source) {

		if (source == null) {
			return null;
		}
		final int length = source.length();
		try (final ByteArrayOutputStream cw = new ByteArrayOutputStream(length)) {
			for (int i = 0; i < length; ++i) {
				final char c = source.charAt(i);
				if (c == '%' && i + 2 < length) {
					final char c1 = Character.toUpperCase(source.charAt(i + 1));
					final char c2 = Character.toUpperCase(source.charAt(i + 2));
					if ((c1 >= '0' && c1 <= '9' || c1 >= 'A' && c1 <= 'F') && (c2 >= '0' && c2 <= '9' || c2 >= 'A' && c2 <= 'F')) {
						i += 2;
						final int b1 = c1 < 'A'
							? c1 + HttpProtocol.BASE_N
							: c1 + HttpProtocol.BASE_A;
						final int b2 = c2 < 'A'
							? c2 + HttpProtocol.BASE_N
							: c2 + HttpProtocol.BASE_A;
						final int code = (b1 << 4) + b2;
						if (code == 0x25 && i + 2 < length && source.charAt(i + 1) != '%') {
							final char cc1 = Character.toUpperCase(source.charAt(i + 1));
							final char cc2 = Character.toUpperCase(source.charAt(i + 2));
							if ((cc1 >= '0' && cc1 <= '9' || cc1 >= 'A' && cc1 <= 'F') && (cc2 >= '0' && cc2 <= '9' || cc2 >= 'A' && cc2 <= 'F')) {
								i += 2;
								final int bb1 = cc1 < 'A'
									? cc1 + HttpProtocol.BASE_N
									: cc1 + HttpProtocol.BASE_A;
								final int bb2 = cc2 < 'A'
									? cc2 + HttpProtocol.BASE_N
									: cc2 + HttpProtocol.BASE_A;
								cw.write((bb1 << 4) + bb2);
								continue;
							}
						}
						cw.write(code);
						continue;
					}
				}
				cw.write(c);
			}
			return cw.toByteArray();
		} catch (final RuntimeException e) {
			throw e;
		} catch (final Exception e) {
			throw new RuntimeException(e);
		}
	}

	static final String urlEncode(final String source) {

		if (source == null) {
			return null;
		}
		final byte[] bytes = source.getBytes(StandardCharsets.UTF_8);
		final StringBuilder sb = new StringBuilder();
		for (int i = 0; i < bytes.length; ++i) {
			final char c = (char) (bytes[i] & 0xFF);
			switch (c) {
				case '%' : {
					if (bytes.length > i + 2) {
						if (HttpProtocol.IS_HEX[bytes[i + 1] & 0xFF] && HttpProtocol.IS_HEX[bytes[i + 2] & 0xFF]) {
							sb.append('%');
							continue;
						}
					}
					HttpProtocol.toHex(sb, c);
				}
					break;
				case '+' :
				case '"' :
				case ';' :
				case '@' :
				case '<' :
				case '>' :
				case '{' :
				case '}' :
				case '|' :
				case '\\' :
				case '^' :
				case '~' :
				case '[' :
				case ']' :
				case '`' : {
					HttpProtocol.toHex(sb, c);
				}
					break;
				default :
					if (c <= 32 || c >= 127) {
						HttpProtocol.toHex(sb, c);
					} else {
						sb.append(c);
					}
			}
		}
		return sb.toString();
	}
}
