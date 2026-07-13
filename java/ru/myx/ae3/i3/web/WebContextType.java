package ru.myx.ae3.i3.web;

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

	public static WebContext<?> createMatchingContext(
			final TargetInterface target,
			final ServeRequest query) {

		/**
		 * explicit
		 */
		{
			final String check = Base.getString( query.getParameters(), "___output", "" ).trim();
			if (check.length() > 0) {
				final WebContext<?> context = WebContextOutputRegistry.create( check, target, query );
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
				final WebContext<?> context = WebContextOutputRegistry.create( check, target, query );
				if (context != null) {
					return context;
				}
			}
		}
		/**
		 * auto-detect: neither an explicit ___output nor a recognized extension matched -
		 * whichever unit's descriptor wins the wildcard shortName (by priority) decides what to
		 * do, e.g. inspecting the Accept header itself
		 */
		{
			final WebContext<?> context = WebContextOutputRegistry.create( WebContextOutputRegistry.WILDCARD_SHORT_NAME, target, query );
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
