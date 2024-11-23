package ru.myx.ae3.i3.web.telnet;

import java.util.function.Function;
import ru.myx.ae3.base.BaseObject;
import ru.myx.ae3.binary.TransferSocket;
import ru.myx.ae3.flow.ObjectTarget;
import ru.myx.ae3.help.Convert;
import ru.myx.ae3.produce.ObjectFactory;

/*
 * Created on 09.02.2005
 */
/**
 * @author myx
 *
 */
public final class TelnetTargetFactory implements ObjectFactory<Object, ObjectTarget<TransferSocket>> {
	
	private static final Class<?>[] TARGETS = {
			ObjectTarget.class, Function.class
	};

	private static final Class<?>[] SOURCES = null;

	private static final String[] VARIETY = {
			"telnet_parser", Telnet.PNAME_TELNET
	};

	/**
	 *
	 */
	public static final ObjectTarget<TransferSocket> DEFAULT_TARGET = new TelnetSocketTarget(false, false, false, false);

	@Override
	public final boolean accepts(final String variant, final BaseObject attributes, final Class<?> source) {
		
		return true;
	}

	@Override
	public final ObjectTarget<TransferSocket> produce(final String variant, final BaseObject attributes, final Object object) {
		
		final boolean ignoreTargetPort = Convert.MapEntry.toBoolean(attributes, "ignoreTargetPort", false);
		final boolean ignoreGzip = Convert.MapEntry.toBoolean(attributes, "ignoreGzip", false);
		final boolean ignoreKeepAlive = Convert.MapEntry.toBoolean(attributes, "ignoreKeepAlive", false);
		final boolean reverseProxied = Convert.MapEntry.toBoolean(attributes, "reverseProxied", false);
		return new TelnetSocketTarget(ignoreTargetPort, ignoreGzip, ignoreKeepAlive, reverseProxied);
	}

	@Override
	public final Class<?>[] sources() {
		
		return TelnetTargetFactory.SOURCES;
	}

	@Override
	public final Class<?>[] targets() {
		
		return TelnetTargetFactory.TARGETS;
	}

	@Override
	public final String[] variety() {
		
		return TelnetTargetFactory.VARIETY;
	}
}
