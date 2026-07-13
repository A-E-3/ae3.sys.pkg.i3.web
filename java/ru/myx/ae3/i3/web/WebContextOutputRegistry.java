package ru.myx.ae3.i3.web;

import java.lang.reflect.Constructor;

import ru.myx.ae3.base.Base;
import ru.myx.ae3.base.BaseArray;
import ru.myx.ae3.base.BaseMapEditable;
import ru.myx.ae3.base.BaseObject;
import ru.myx.ae3.exec.Exec;
import ru.myx.ae3.i3.TargetInterface;
import ru.myx.ae3.serve.ServeRequest;
import ru.myx.ae3.util.fn.SupplierVfsFolderMapCached;
import ru.myx.ae3.vfs.Entry;
import ru.myx.ae3.vfs.Storage;
import ru.myx.sapi.JsonSAPI;

/** Reads "*.json" output-type descriptors from /union/settings/system/l3/targets/. Each unit that
 * actually implements a target/output (an "l2.tgt.*" unit, or any other) contributes its own
 * descriptor files under its own ae3-packages resources - this unit (i3.web) ships none itself,
 * it only reads and dispatches (see the settings/system/l3/targets/README in this unit's ae3-packages folder).
 *
 * Cached like SupplierVfsFolderXslTemplatesCached (lock-free, ~2.5s recheck), so a lookup is a
 * map lookup plus a reflective newInstance - Class.forName only runs when a descriptor's text
 * actually changes.
 *
 * Descriptor shape (mirrors the existing, otherwise-unconsumed "ae3.web/Output" settings/web/
 * outputs/*.json convention's nested context.reference):
 *
 * <pre>
 * {
 *   "extensions": ["xhtml", "xhtm"],
 *   "contentTypes": ["application/xhtml+xml"],
 *   "priority": 0,
 *   "context": { "reference": "java.class/fully.Qualified.ClassName" }
 * }
 * </pre>
 *
 * "extensions" and "contentTypes" are both matched the exact same way, against either the
 * explicit "___output" request parameter or the request's file extension (mirroring the original
 * WebContextType enum, whose acceptExtensions and mimeTypes fed the very same lookup map - e.g.
 * "___output=text/html" has always been just as valid as "___output=html"). Two descriptors may
 * register the same value; the higher "priority" wins (a real number, not necessarily an integer).
 *
 * There's no separate "is this the default" flag - the auto-detect/no-explicit-output fallback is
 * configured the exact same way as everything else: a descriptor just lists
 * {@link #WILDCARD_SHORT_NAME} among its "extensions" (typically alongside a human-friendly
 * explicit alias like "auto-detect", so it's independently selectable via "___output" too).
 * Whichever descriptor wins the wildcard slot (by priority, possibly from a different unit than
 * any of the concrete formats) gets constructed when neither an explicit "___output" nor a
 * recognized extension matched anything - it's then that class's own job to inspect the request
 * (e.g. Accept header) and decide what to actually do, the way WebContextXmlAutoDetect already
 * does.
 *
 * @author myx */
final class WebContextOutputRegistry extends SupplierVfsFolderMapCached {

	private static final String JAVA_CLASS_PREFIX = "java.class/";

	static final String WILDCARD_SHORT_NAME = "*";

	static final WebContextOutputRegistry INSTANCE = new WebContextOutputRegistry(//
			Storage.UNION.relative("settings/system/l3/targets", null));

	/** @param shortName
	 *            an explicit "___output" value, a request's file extension, or
	 *            {@link #WILDCARD_SHORT_NAME}
	 * @param target
	 * @param query
	 * @return constructed WebContext, or null when nothing is registered for shortName or
	 *         construction failed */
	static WebContext<?> create(final String shortName, final TargetInterface target, final ServeRequest query) {

		final Object holder = WebContextOutputRegistry.INSTANCE.get().baseGet(shortName, BaseObject.UNDEFINED).baseValue();
		if (!(holder instanceof RegisteredFactory)) {
			return null;
		}
		try {
			return (WebContext<?>) ((RegisteredFactory) holder).constructor.newInstance(target, query);
		} catch (final Exception e) {
			return null;
		}
	}

	private static void register(final BaseMapEditable result, final String key, final RegisteredFactory factory) {

		final Object existing = result.baseGet(key, BaseObject.UNDEFINED).baseValue();
		if (!(existing instanceof RegisteredFactory) || ((RegisteredFactory) existing).priority <= factory.priority) {
			result.putAppend(key, Base.forUnknown(factory));
		}
	}

	private static String[] stringArray(final BaseObject descriptor, final String field) {

		final BaseObject value = descriptor.baseGet(field, BaseObject.UNDEFINED);
		if (!(value instanceof BaseArray)) {
			return new String[0];
		}
		final BaseArray array = (BaseArray) value;
		final int length = array.length();
		final String[] result = new String[length];
		for (int i = 0; i < length; ++i) {
			result[i] = array.baseGet(i, BaseObject.UNDEFINED).baseToJavaString();
		}
		return result;
	}

	private WebContextOutputRegistry(final Entry folder) {

		super(folder);
	}

	@Override
	protected String runDescriptorFilter(final String name) {

		final String key = name.toLowerCase();
		return key.endsWith(".json")
			? key
			: null;
	}

	@Override
	protected BaseObject runDescriptorMapper(final Entry entry, final String name) {

		return JsonSAPI.parse(Exec.currentProcess(), entry);
	}

	@Override
	protected BaseObject runDescriptorReducer(final BaseMapEditable result, final BaseObject descriptor, final String name) {

		final String reference = Base.getString(descriptor.baseGet("context", BaseObject.UNDEFINED), "reference", "").trim();
		if (!reference.startsWith(WebContextOutputRegistry.JAVA_CLASS_PREFIX)) {
			return result;
		}
		final Constructor<?> constructor;
		try {
			constructor = Class.forName(reference.substring(WebContextOutputRegistry.JAVA_CLASS_PREFIX.length()))//
					.getConstructor(TargetInterface.class, ServeRequest.class);
		} catch (final Exception e) {
			return result;
		}
		final double priority = Base.getDouble(descriptor, "priority", 0);
		final RegisteredFactory factory = new RegisteredFactory(constructor, priority);
		for (final String extension : WebContextOutputRegistry.stringArray(descriptor, "extensions")) {
			WebContextOutputRegistry.register(result, extension, factory);
		}
		for (final String contentType : WebContextOutputRegistry.stringArray(descriptor, "contentTypes")) {
			WebContextOutputRegistry.register(result, contentType, factory);
		}
		return result;
	}

	@Override
	protected String runFolderFilter(final Entry entry) {

		return null;
	}

	@Override
	protected BaseObject runFolderMapper(final String name, final Entry entry) {

		return null;
	}

	private static final class RegisteredFactory {

		final Constructor<?> constructor;

		final double priority;

		RegisteredFactory(final Constructor<?> constructor, final double priority) {

			this.constructor = constructor;
			this.priority = priority;
		}
	}
}
