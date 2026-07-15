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
 * Matching is category-based:
 * - extensions: implicit file-extension matching.
 * - contentTypes: implicit MIME-like matching from request Accept tokens.
 * - keywords: explicit ___output/API matching, populated from extensions, contentTypes and aliases.
 * Two descriptors may register the same key; the higher "priority" wins.
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

	static final String AUTO_DETECT_SHORT_NAME = "auto-detect";

	static final String WILDCARD_SHORT_NAME = "*";

	static final String[] DEFAULT_MATCHER_SHORT_NAMES = new String[]{
			WebContextOutputRegistry.AUTO_DETECT_SHORT_NAME,
			WebContextOutputRegistry.WILDCARD_SHORT_NAME,
	};

	private static final String CATEGORY_CONTENT_TYPES = "contentTypes";

	private static final String CATEGORY_EXTENSIONS = "extensions";

	private static final String CATEGORY_KEYWORDS = "keywords";

	static final WebContextOutputRegistry INSTANCE = new WebContextOutputRegistry(//
			Storage.UNION.relative("settings/system/l3/targets", null));

	/** @param shortName
	 *            an explicit "___output" value
	 * @param target
	 * @param query
	 * @param explicit
	 *            true for explicit ___output/API selection; false for implicit selection
	 * @return constructed WebContext, or null when nothing is registered for shortName or
	 *         construction failed */
	static WebContext<?> createByKeyword(final String shortName, final TargetInterface target, final ServeRequest query, final boolean explicit) {

		final RegisteredFactory factory = WebContextOutputRegistry.getFactory(
				WebContextOutputRegistry.CATEGORY_KEYWORDS,
				shortName,
				explicit);
		if (factory == null) {
			return null;
		}
		try {
			return (WebContext<?>) factory.constructor.newInstance(target, query);
		} catch (final Exception e) {
			return null;
		}
	}

	/** @param shortName
	 *            file extension
	 * @param target
	 * @param query
	 * @param explicit
	 *            true for explicit ___output/API selection; false for implicit selection
	 * @return constructed WebContext, or null when nothing is registered for shortName or
	 *         construction failed */
	static WebContext<?> createByExtension(final String shortName, final TargetInterface target, final ServeRequest query, final boolean explicit) {

		final RegisteredFactory factory = WebContextOutputRegistry.getFactory(
				WebContextOutputRegistry.CATEGORY_EXTENSIONS,
				shortName,
				explicit);
		if (factory == null) {
			return null;
		}
		try {
			return (WebContext<?>) factory.constructor.newInstance(target, query);
		} catch (final Exception e) {
			return null;
		}
	}

	/** @param shortNames
	 *            matcher short names to test as one candidate set (all matches considered)
	 * @param target
	 * @param query
	 * @param explicit
	 *            true for explicit ___output/API selection; false for implicit selection
	 * @return constructed WebContext, or null when nothing is registered for shortName or
	 *         construction failed */
	static WebContext<?> createByKeywords(final String[] shortNames, final TargetInterface target, final ServeRequest query, final boolean explicit) {

		final RegisteredFactory best = WebContextOutputRegistry.findBest(
				WebContextOutputRegistry.CATEGORY_KEYWORDS,
				shortNames,
				explicit);
		if (best == null) {
			return null;
		}
		try {
			return (WebContext<?>) best.constructor.newInstance(target, query);
		} catch (final Exception e) {
			return null;
		}
	}

	/** @param shortNames
	 *            content-type matcher values to test as one candidate set (all matches considered)
	 * @param target
	 * @param query
	 * @param explicit
	 *            true for explicit ___output/API selection; false for implicit selection
	 * @return constructed WebContext, or null when nothing is registered for shortName or
	 *         construction failed */
	static WebContext<?> createByContentTypes(final String[] shortNames, final TargetInterface target, final ServeRequest query, final boolean explicit) {

		final RegisteredFactory best = WebContextOutputRegistry.findBest(
				WebContextOutputRegistry.CATEGORY_CONTENT_TYPES,
				shortNames,
				explicit);
		if (best == null) {
			return null;
		}
		try {
			return (WebContext<?>) best.constructor.newInstance(target, query);
		} catch (final Exception e) {
			return null;
		}
	}

	private static BaseMapEditable getCategoryMap(final BaseObject root, final String category) {

		final BaseObject holder = root.baseGet(category, BaseObject.UNDEFINED);
		if (holder instanceof BaseMapEditable) {
			return (BaseMapEditable) holder;
		}
		return null;
	}

	private static RegisteredFactory findBest(final String category, final String[] shortNames, final boolean explicit) {

		if (shortNames == null || shortNames.length == 0) {
			return null;
		}
		RegisteredFactory best = null;
		for (final String shortName : shortNames) {
			final RegisteredFactory factory = WebContextOutputRegistry.getFactory(category, shortName, explicit);
			if (factory == null) {
				continue;
			}
			if (best == null || best.priority <= factory.priority) {
				best = factory;
			}
		}
		return best;
	}

	private static RegisteredFactory getFactory(final String category, final String key, final boolean explicit) {

		final String normalized = WebContextOutputRegistry.normalizeKey(key);
		if (normalized == null) {
			return null;
		}
		final BaseMapEditable map = WebContextOutputRegistry.getCategoryMap(WebContextOutputRegistry.INSTANCE.get(), category);
		if (map == null) {
			return null;
		}
		final Object holder = map.baseGet(normalized, BaseObject.UNDEFINED).baseValue();
		if (!(holder instanceof RegisteredFactory)) {
			return null;
		}
		final RegisteredFactory factory = (RegisteredFactory) holder;
		if (!explicit && factory.priority < 0) {
			return null;
		}
		return factory;
	}

	private static String normalizeKey(final String key) {

		if (key == null) {
			return null;
		}
		final String normalized = key.trim().toLowerCase();
		return normalized.length() == 0
			? null
			: normalized;
	}

	private static double normalizePriority(final double priority) {

		if (priority < 0) {
			return priority;
		}
		return priority > 1.0
			? 1.0
			: priority;
	}

	private static void register(final BaseMapEditable result, final String category, final String key, final RegisteredFactory factory) {

		final String normalized = WebContextOutputRegistry.normalizeKey(key);
		if (normalized == null) {
			return;
		}
		BaseMapEditable map = WebContextOutputRegistry.getCategoryMap(result, category);
		if (map == null) {
			map = BaseObject.createObject();
			result.putAppend(category, map);
		}
		final Object existing = map.baseGet(normalized, BaseObject.UNDEFINED).baseValue();
		if (!(existing instanceof RegisteredFactory) || ((RegisteredFactory) existing).priority <= factory.priority) {
			map.putAppend(normalized, Base.forUnknown(factory));
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

		try {
			return JsonSAPI.parse(Exec.currentProcess(), entry);
		} catch (final Exception e) {
			return BaseObject.UNDEFINED;
		}
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
		final double priority = WebContextOutputRegistry.normalizePriority(Base.getDouble(descriptor, "priority", 0));
		final RegisteredFactory factory = new RegisteredFactory(constructor, priority);
		for (final String extension : WebContextOutputRegistry.stringArray(descriptor, "extensions")) {
			WebContextOutputRegistry.register(result, WebContextOutputRegistry.CATEGORY_EXTENSIONS, extension, factory);
			WebContextOutputRegistry.register(result, WebContextOutputRegistry.CATEGORY_KEYWORDS, extension, factory);
		}
		for (final String alias : WebContextOutputRegistry.stringArray(descriptor, "aliases")) {
			WebContextOutputRegistry.register(result, WebContextOutputRegistry.CATEGORY_KEYWORDS, alias, factory);
		}
		for (final String contentType : WebContextOutputRegistry.stringArray(descriptor, "contentTypes")) {
			WebContextOutputRegistry.register(result, WebContextOutputRegistry.CATEGORY_CONTENT_TYPES, contentType, factory);
			WebContextOutputRegistry.register(result, WebContextOutputRegistry.CATEGORY_KEYWORDS, contentType, factory);
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
