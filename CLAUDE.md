# CLAUDE.md — ae3.sys.pkg.i3.web

Base Web (UI) SDK: the HTTP-facing dispatch layer. Requires `ae3.api`, `ae3.sdk`, `ae3.base`; provides `ae3.web`, `ae3.web.server`, `ae3.web.server-docs`.

## Content-type dispatch: JSON-registry-driven, no hardcoded WebContext subclasses

`java/ru/myx/ae3/i3/web/WebContextType.java` is the reply-format dispatcher. `WebContextType.createMatchingContext(target, query)` resolves which `WebContext` implementation handles a request, in this order, each step delegating to a different `WebContextOutputRegistry` lookup method:

1. explicit `___output` query parameter, looked up as a shortName via `createByKeyword(...)`
2. file extension on the resource path (`FileName.extensionExact`), looked up as a shortName via `createByExtension(...)`
3. `Accept` request header, parsed into content-type tokens (`WebContextType.parseAcceptContentTypes`, strips `;q=...` parameters off each comma-separated entry) and matched via `createByContentTypes(...)` — lets a plain browser/client request (no `___output`, no recognized extension) still get content-negotiated instead of falling straight to auto-detect
4. auto-detect: `createByKeywords(WebContextOutputRegistry.DEFAULT_MATCHER_SHORT_NAMES, ...)` — the wildcard shortName, for when none of the above matched anything
5. fallback: hardcoded `new WebContextSimple(target, query)` — the only WebContext class this unit still constructs directly (also the only WebContext-implementing class that still physically lives here — it's the generic "nothing else applies" case with no owning target unit), used only when even the wildcard has nothing registered

`WebContextOutputRegistry` (same package) scans `/union/settings/system/l3/targets/*.json` for `{"extensions":[...], "contentTypes":[...], "priority":0, "context":{"reference":"java.class/FQCN"}}` — `extensions` and `contentTypes` feed the same shortName lookup (`___output=text/html` is as valid as `___output=html`), higher `priority` wins ties. This unit ships no descriptors of its own — each unit that implements a target/output contributes its own (see `ae3-packages/ae3.web/settings/system/l3/targets/README.md`).

Concrete `WebContext*` classes live with the target-context they wrap, not here:

- `ru.myx.ae3.l2.html.WebContextHtml`/`WebContextRss`, `ru.myx.ae3.l2.xhtml.WebContextXhtml` — `ae3.sys.pkg.l2.tgt.html`
- `ru.myx.ae3.l2.text.WebContextText` — `ae3.sys.pkg.l2.tgt.text`
- `ru.myx.ae3.l2.pdf.WebContextPdf` — `ae3.sys.pkg.l2.tgt.pdf`
- `ru.myx.ae3.l2.json.WebContextJson` — `ae3.sys.pkg.l2.tgt.json`. Distinct from that unit's `JsonTargetContext`/`JsonReplyTargetContext`, an older, parallel, non-`WebContext` mechanism used directly by other L2 targets — don't conflate the two.
- `ru.myx.ae3.l2.xml.WebContextXml`/`WebContextXmlXhtml`/`WebContextXmlAutoDetect` — `ae3.sys.pkg.l2.tgt.xml`, see its CLAUDE.md

Each of those units `Requires: ae3.web` only for the `WebContext` interface — `i3.web` has no reciprocal dependency on any of them, since dispatch only ever reaches their classes through registry reflection.

`WebContextXhtml` (`ae3.sys.pkg.l2.tgt.html`, old DOM-based renderer) is not registered for anything — `xhtml`/`xhtm` belongs to `ae3.sys.pkg.l2.tgt.xml`'s `WebContextXmlXhtml` (server-side XSLT superseded the DOM approach). Reachable only via a direct `java.class/ru.myx.ae3.l2.xhtml.WebContextXhtml` descriptor reference.

## Headers arrive as request attributes, not a separate API

See `ae3.api`'s CLAUDE.md for the general convention. Here: `http/QueryHttp.java` passes the raw header `BaseMap` straight to `super(...)` as attributes; `SocketHandler.java` reads sibling headers the same way, e.g. `Base.getString(this.qHeaders, "Accept-Encoding", null)`.

## WebContextXml stays pure — server-side XSLT lives in a sibling class

`WebContextXml` (`ae3.sys.pkg.l2.tgt.xml`, package `ru.myx.ae3.l2.xml`) — its `getResultReply()`, for the `"xml".equals(layout)` branch, always embeds the client-side `<?xml-stylesheet?>` PI and replies `text/xml`, regardless of `Accept`. Deliberate: an explicit `___output=xml` request always gets pure, unnegotiated XML — which is why `l2.tgt.xml` never registers an override for shortName `xml` itself, only `xhtml`/`xhtm` and the wildcard.

`WebContextXmlAutoDetect extends WebContextXml`, overrides `getResultReply()`: when the client's `Accept` header lists `application/xhtml+xml`, replies with server-side XSLT-rendered `application/xhtml+xml` instead, falling back to `super.getResultReply()` otherwise. Registered via the registry's wildcard shortName (`extensions: ["*", "auto-detect"]`) — the auto-detect default is a config-time descriptor-priority decision, not something hardcoded here.

## The one and only call site: WebTargetActor.apply()

`WebContextType.createMatchingContext(this, query)` is called from exactly one place: `WebTargetActor.apply()` (a `default` method on the `WebTargetActor` interface). Since dispatch is registry-driven, changing the default for every target just means registering a higher-priority `"*"` descriptor — no code change here.

## settings/web/outputs/{default,raw}.json — unrelated, unfinished scaffolding, leave alone

Not the same thing as `settings/system/l3/targets/` above. `ae3-packages/ae3.web.server/settings/web/outputs/` holds two `"ae3.web/Output"`-typed descriptors with `ui` metadata (`title`/`abstract`/`download`/`preview`/`important`) and a `layouts` block — shaped like scaffolding for a UI-facing "export format picker", not content-negotiation dispatch. Both files' `context.reference` points at `WebContextType` itself, but nothing in this workspace's checked-out units reads this folder. A different, never-finished feature, not an earlier version of the registry above — leave untouched unless a real consumer surfaces.
