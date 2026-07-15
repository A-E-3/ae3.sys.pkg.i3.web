package ru.myx.ae3.i3.web;

import java.util.ArrayList;
import java.util.List;

import ru.myx.ae3.base.Base;
import ru.myx.ae3.help.FileName;
import ru.myx.ae3.i3.TargetInterface;
import ru.myx.ae3.serve.ServeRequest;

/** Reply-format dispatcher. All shortName -&gt; WebContext wiring lives in
 * {@link WebContextOutputRegistry}'s JSON descriptors under /union/settings/system/l3/targets/,
 * contributed by whichever unit actually implements a given target/output - this class hardcodes
 * no WebContext subclass itself (see settings/system/l3/targets/README in this unit's
 * ae3-packages folder for why: this unit only dispatches, it doesn't implement targets).
 *
 * @author myx */
final class WebContextType {

	private static String[] parseAcceptContentTypes(final String acceptHeader) {

		if (acceptHeader == null) {
			return null;
		}
		final String value = acceptHeader.trim().toLowerCase();
		if (value.length() == 0) {
			return null;
		}
		final String[] parts = value.split(",");
		final List<String> result = new ArrayList<>(parts.length);
		for (final String part : parts) {
			if (part == null) {
				continue;
			}
			final String token = part.trim();
			if (token.length() == 0) {
				continue;
			}
			final int semicolon = token.indexOf(';');
			final String contentType = semicolon >= 0
				? token.substring(0, semicolon).trim()
				: token;
			if (contentType.length() > 0) {
				result.add(contentType);
			}
		}
		return result.isEmpty()
			? null
			: result.toArray(new String[result.size()]);
	}

	public static WebContext<?> createMatchingContext(
			final TargetInterface target,
			final ServeRequest query) {

		/**
		 * explicit
		 */
		{
			final String check = Base.getString( query.getParameters(), "___output", "" ).trim();
			if (check.length() > 0) {
				final WebContext<?> context = WebContextOutputRegistry.createByKeyword( check, target, query, true );
				if (context != null) {
					return context;
				}
			}
		}
		/**
		 * extension
		 */
		{
			final String path = query.getResourceIdentifier();
			final String check = FileName.extensionExact( path );
			if (check != null && check.length() > 0) {
				final WebContext<?> context = WebContextOutputRegistry.createByExtension( check, target, query, false );
				if (context != null) {
					return context;
				}
			}
		}
		/**
		 * MIME-like detect by Accept content-types.
		 */
		{
			final String[] check = WebContextType.parseAcceptContentTypes(Base.getString(query.getAttributes(), "Accept", ""));
			if (check != null) {
				final WebContext<?> context = WebContextOutputRegistry.createByContentTypes( check, target, query, false );
				if (context != null) {
					return context;
				}
			}
		}
		/**
		 * auto-detect: neither explicit output, extension nor content-type matched.
		 * matcher keywords are hardcoded and compared by priority: auto-detect and wildcard.
		 */
		{
			final WebContext<?> context =
					WebContextOutputRegistry.createByKeywords( WebContextOutputRegistry.DEFAULT_MATCHER_SHORT_NAMES, target, query, false );
			if (context != null) {
				return context;
			}
		}
		/**
		 * bugger all - nothing registered even for the wildcard
		 */
		{
			return new WebContextSimple( target, query );
		}
	}

	private WebContextType() {

		// static utility, not instantiable
	}
}
