/*
 * Created on 30.11.2005
 */
package ru.myx.ae3.i3.web.http;

import java.nio.charset.Charset;
import java.util.Properties;

import ru.myx.ae3.base.Base;
import ru.myx.ae3.base.BaseObject;
import ru.myx.ae3.help.Message;
import ru.myx.ae3.produce.ObjectFactory;

/**
 * @author myx
 *
 */
public final class FactoryQueryStringToProperties implements ObjectFactory<String, Properties> {
	
	private static final Class<?>[] SOURCES = {
			String.class
	};

	private static final Class<?>[] TARGETS = {
			Properties.class
	};

	private static final String[] VARIETY = {
			"application/x-url-encoded", "application/x-www-form-urlencoded", "query_string"
	};

	@Override
	public boolean accepts(final String variant, final BaseObject attributes, final Class<?> source) {
		
		return true;
	}

	@Override
	public Properties produce(final String variant, final BaseObject attributes, final String s) {
		
		final Properties result = new Properties();
		final BaseObject wrapped = Base.forUnknown(result);
		final StringBuilder current = new StringBuilder(256);
		final int length = s.length();
		for (int i = 0; i < length; ++i) {
			final char c = s.charAt(i);
			switch (c) {
				case '&' : {
					final Charset charset = Message.getCharset(attributes);
					MaterializerXUrl.toMap(wrapped, current.toString(), charset);
					current.setLength(0);
					break;
				}
				default :
					current.append(c);
			}
		}
		MaterializerXUrl.toMap(wrapped, current.toString(), Message.getCharset(attributes));
		return result;
	}

	@Override
	public Class<?>[] sources() {
		
		return FactoryQueryStringToProperties.SOURCES;
	}

	@Override
	public Class<?>[] targets() {
		
		return FactoryQueryStringToProperties.TARGETS;
	}

	@Override
	public String[] variety() {
		
		return FactoryQueryStringToProperties.VARIETY;
	}
}
