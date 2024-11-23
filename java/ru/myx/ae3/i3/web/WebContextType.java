package ru.myx.ae3.i3.web;

import java.util.Map;
import java.util.TreeMap;

import ru.myx.ae3.base.Base;
import ru.myx.ae3.help.FileName;
import ru.myx.ae3.i3.TargetInterface;
import ru.myx.ae3.serve.ServeRequest;

enum WebContextType {
	/**
	 * 
	 */
	HTTP_HTML(//
			new String[] { ".html", ".htm" }, //
			new String[] { "text/html" } //
	) {
		@Override
		WebContext<?> createContext(
				final TargetInterface target,
				final ServeRequest query) {
		
			return new WebContextHtml( target, query );
		}
	},
	/**
	 * 
	 */
	HTTP_XHTML(//
			new String[] { ".xhtml", ".xhtm" }, //
			new String[] { "application/xhtml+xml" }//
	) {
		@Override
		WebContext<?> createContext(
				final TargetInterface target,
				final ServeRequest query) {
		
			return new WebContextXhtml( target, query );
		}
	},
	/**
	 * 
	 */
	HTTP_XML(//
			new String[] { ".xml" }, //
			new String[] { "application/xml", "text/xml" }//
	) {
		@Override
		WebContext<?> createContext(
				final TargetInterface target,
				final ServeRequest query) {
		
			return new WebContextXml( target, query );
		}
	},
	/**
	 * 
	 */
	HTTP_JSON(//
			new String[] { ".json" }, //
			new String[] { "text/json" }//
	) {
		@Override
		WebContext<?> createContext(
				final TargetInterface target,
				final ServeRequest query) {
		
			return new WebContextJson( target, query );
		}
	},
	/**
	 * 
	 */
	HTTP_TEXT(//
			new String[] { ".txt" }, //
			new String[] { "text/plain" }//
	) {
		@Override
		WebContext<?> createContext(
				final TargetInterface target,
				final ServeRequest query) {
		
			return new WebContextText( target, query );
		}
	},
	/**
	 * 
	 */
	HTTP_JS(//
			new String[] {}, //
			new String[] {}//
	) {
		@Override
		WebContext<?> createContext(
				final TargetInterface target,
				final ServeRequest query) {
		
			return new WebContextHtml( target, query );
		}
	},
	/**
	 * 
	 */
	HTTP_RSS(//
			new String[] { ".rss", ".xrss" }, //
			new String[] { "text/rss" }//
	) {
		@Override
		WebContext<?> createContext(
				final TargetInterface target,
				final ServeRequest query) {
		
			return new WebContextHtml( target, query );
		}
	},
	/**
	 * 
	 */
	HTTP_PDF(//
			new String[] { ".pdf" }, //
			new String[] { "application/pdf" }//
	) {
		@Override
		WebContext<?> createContext(
				final TargetInterface target,
				final ServeRequest query) {
		
			return new WebContextPdf( target, query );
		}
	},
	/**
	 * 
	 */
	HTTP_CSV(//
			new String[] { ".csv" }, //
			new String[] { "text/csv" }//
	) {
		@Override
		WebContext<?> createContext(
				final TargetInterface target,
				final ServeRequest query) {
		
			return new WebContextHtml( target, query );
		}
	},
	/**
	 * 
	 */
	;
	
	static final Map<String, WebContextType>	MAP;
	static {
		MAP = new TreeMap<>();
		for (final WebContextType type : WebContextType.values()) {
			for (final String extension : type.acceptExtensions) {
				assert extension.startsWith( "." );
				WebContextType.MAP.put( extension.substring( 1 ), type );
			}
			for (final String mime : type.mimeTypes) {
				WebContextType.MAP.put( mime, type );
			}
		}
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
				final WebContextType type = WebContextType.MAP.get( check );
				if (type != null) {
					return type.createContext( target, query );
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
				final WebContextType type = WebContextType.MAP.get( check );
				if (type != null) {
					return type.createContext( target, query );
				}
			}
		}
		/**
		 * headers
		 */
		{
			//
		}
		/**
		 * bugger all
		 */
		{
			// return new WebContextHtml( target, query );
			return new WebContextSimple( target, query );
		}
	}
	
	final String[]	acceptExtensions;
	
	final String[]	mimeTypes;
	
	
	private WebContextType(final String[] acceptExtensions, final String[] mimeTypes) {
	
		this.acceptExtensions = acceptExtensions;
		this.mimeTypes = mimeTypes;
	}
	
	
	abstract WebContext<?> createContext(
			final TargetInterface target,
			final ServeRequest query);
}
