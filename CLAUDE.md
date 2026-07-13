# CLAUDE.md — ae3.sys.pkg.i3.web

Base Web (UI) SDK: the HTTP-facing dispatch layer. Requires `ae3.api`, `ae3.sdk`, `ae3.base`; provides `ae3.web`, `ae3.web.server`, `ae3.web.server-docs`.

## Content-type dispatch

`java/ru/myx/ae3/i3/web/WebContextType.java` is the reply-format dispatcher. `WebContextType.createMatchingContext(target, query)` resolves which `WebContext` implementation handles a request, in this order:

1. explicit `___output` query parameter (matched against `WebContextType.MAP`)
2. file extension on the resource path (`FileName.extensionExact`)
3. **headers — currently an empty stub** (`/** headers */ { // }`), i.e. Accept-header content negotiation is not implemented yet
4. fallback: `WebContextSimple`

Each `WebContextType` enum constant maps extensions + MIME types to a `WebContext` subclass:

- `WebContextHtml` — `text/html`
- `WebContextXhtml` — `application/xhtml+xml`, built via `XhtmlDomTargetContext` (constructs a DOM directly — does not run XSLT)
- `WebContextXml` — `text/xml`/`application/xml`; see gotcha below
- `WebContextJson`, `WebContextText`, `WebContextPdf`, `WebContextSimple`
- `.rss`/`.csv` currently route to `WebContextHtml`, not dedicated renderers

## Headers arrive as request attributes, not a separate API

`ServeRequest` has no `getHeader(...)` method. Raw HTTP headers land verbatim in `query.getAttributes()`, keyed by their literal HTTP header name. `http/QueryHttp.java` passes the raw header `BaseMap` straight to `super(...)` as attributes; `SocketHandler.java` (which parses the socket) reads sibling headers the same way, e.g. `Base.getString(this.qHeaders, "Accept-Encoding", null)`. So an `Accept` header check reads as `Base.getString(query.getAttributes(), "Accept", "")`.

## WebContextXml stays pure — server-side XSLT lives in a subclass, in a different unit

`WebContextXml.getResultReply()` is unmodified from its original behavior: for the `"xml".equals(layout)` branch, it always embeds the client-side `<?xml-stylesheet?>` PI (from `result.xsl`) and replies `text/xml`, regardless of `Accept`. This is deliberate — an explicit `___output=xml` request must always get pure, unnegotiated XML.

A separate class, `WebContextXmlAutoDetect` (in `ae3.sys.pkg.l2.tgt.xml`, package `ru.myx.ae3.l2.xml`), `extends WebContextXml` and overrides `getResultReply()`: it inspects the same `xml`-layout/`xsl` fields itself, and when the client's `Accept` header lists `application/xhtml+xml`, replies with a server-side XSLT-rendered `application/xhtml+xml` document instead — falling back to `super.getResultReply()` (the exact pure behavior above) whenever it doesn't apply or the transform fails. It lives in `l2.tgt.xml`, not here, because that's the unit that actually owns `skin-standard-xml` and the stylesheets involved — `i3.web` stays generic and has no knowledge of any specific skin. See `l2.tgt.xml`'s CLAUDE.md for the class itself.

Not wired into `WebContextType`'s empty `headers` stub (stage 3 above) — that stub only runs when there's neither an explicit `___output` param nor a matching extension, and can't know `layout` yet anyway (nothing has been rendered at that point). Which concrete `WebContext` class gets constructed for the "default" (no `___output=xml`) case — plain `WebContextXml` vs. `WebContextXmlAutoDetect` — is decided by application/target code outside this generic SDK, not by anything in this package.
