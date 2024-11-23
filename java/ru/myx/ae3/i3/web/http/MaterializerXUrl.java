package ru.myx.ae3.i3.web.http;

/**
 * Created on 30.09.2002
 * 
 * myx - barachta */
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;

import ru.myx.ae3.base.Base;
import ru.myx.ae3.base.BaseMap;
import ru.myx.ae3.base.BaseObject;
import ru.myx.ae3.base.BasePrimitiveString;
import ru.myx.ae3.binary.TransferBuffer;
import ru.myx.ae3.help.Message;
import ru.myx.ae3.transform.TransformMaterializerSimple;

/**
 * @author myx
 * 		
 * myx - barachta 
 *         "typecomment": Window>Preferences>Java>Templates. To enable and
 *         disable the creation of type comments go to
 *         Window>Preferences>Java>Code Generation.
 */
public final class MaterializerXUrl extends TransformMaterializerSimple<BaseMap> {
	
	static void toMap(final BaseObject map, final String s, final Charset encoding) {
		
		final int pos = s.indexOf('=');
		if (pos == -1) {
			return;
		}
		final String name = HttpProtocol.urlDecode(s.substring(0, pos), encoding);
		final BasePrimitiveString value = Base.forString(HttpProtocol.urlDecode(s.substring(pos + 1), encoding));
		final BaseObject o = map.baseGet(name, BaseObject.UNDEFINED);
		if (o == BaseObject.UNDEFINED) {
			map.baseDefine(name, value);
		} else {
			assert o != null : "NULL java object";
			final MultipleList list;
			if (o instanceof MultipleList) {
				list = (MultipleList) o;
			} else {
				list = new MultipleList();
				list.add(o);
				map.baseDefine(name, list);
			}
			list.add(value);
		}
	}
	
	/**
	 * @param contentType
	 */
	public MaterializerXUrl(final String contentType) {
		super(contentType, BaseMap.class);
	}
	
	@Override
	protected BaseMap materialize(final String contentType, final TransferBuffer buf, final BaseObject attributes) throws IOException {
		
		if (buf == null || !buf.hasRemaining()) {
			return null;
		}
		final BaseMap result = BaseObject.createObject();
		final Charset charset = Message.getCharset(attributes);
		if (buf.isDirectAbsolutely()) {
			final String s = new String(buf.toDirectArray(), charset);
			final StringBuilder current = new StringBuilder(256);
			final int length = s.length();
			for (int i = 0; i < length; ++i) {
				final char c = s.charAt(i);
				switch (c) {
					case '&' :
						MaterializerXUrl.toMap(result, current.toString(), charset);
						current.setLength(0);
						break;
					default :
						current.append(c);
				}
			}
			MaterializerXUrl.toMap(result, current.toString(), charset);
			return result;
		}
		final InputStream in = buf.toInputStream();
		try (final Reader reader = new InputStreamReader(in, charset)) {
			final StringBuilder current = new StringBuilder(256);
			for (;;) {
				final int i = reader.read();
				if (i == -1) {
					break;
				}
				switch (i) {
					case '&' :
						MaterializerXUrl.toMap(result, current.toString(), charset);
						current.setLength(0);
						break;
					default :
						current.append((char) i);
				}
			}
			MaterializerXUrl.toMap(result, current.toString(), charset);
			return result;
		}
	}
}
