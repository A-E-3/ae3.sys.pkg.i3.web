# CLAUDE.md — ae3.sys.pkg.i3.web

Base Web (UI) SDK: the HTTP-facing dispatch layer. Requires `ae3.api`, `ae3.sdk`, `ae3.base`; provides `ae3.web`, `ae3.web.server`, `ae3.web.server-docs`.

## Content-type dispatch: fully JSON-registry-driven, this unit hardcodes no WebContext subclass and implements none itself

`java/ru/myx/ae3/i3/web/WebContextType.java` is the reply-format dispatcher. `WebContextType.createMatchingContext(target, query)` resolves which `WebContext` implementation handles a request, in this order, each step delegating to `WebContextOutputRegistry.create(shortName, target, query)`:

1. explicit `___output` query parameter, looked up as a shortName
2. file extension on the resource path (`FileName.extensionExact`), looked up as a shortName
3. auto-detect: `WebContextOutputRegistry.create("*", ...)` — the wildcard shortName, for when neither of the above matched anything
4. fallback: hardcoded `new WebContextSimple(target, query)` — the only WebContext class this unit still constructs directly (also the only WebContext-implementing class that still physically lives here — it's the generic "nothing else applies" case with no owning target unit), used only when even the wildcard has nothing registered

`WebContextOutputRegistry` (same package) scans `/union/settings/system/l3/targets/*.json` for descriptors of the shape `{"extensions":[...], "contentTypes":[...], "priority":0, "context":{"reference":"java.class/FQCN"}}` — both `extensions` and `contentTypes` feed the exact same shortName lookup (so `___output=text/html` has always been just as valid as `___output=html`), and higher `priority` wins ties. This unit ships **no descriptors of its own** — see `ae3-packages/ae3.web/settings/system/l3/targets/README.md` — because it doesn't implement any target/output itself; each unit that does (an `l2.tgt.*` unit, or any other) contributes its own descriptor files.

**All concrete `WebContext*` classes moved out of this unit into the repo that owns their target-context superclass**, alongside their descriptors (same session as the registry itself) — this unit now contains only the generic `WebContext` interface, the dispatcher, the registry, and `WebContextSimple`:

- `ru.myx.ae3.l2.html.WebContextHtml` (`text/html`, `.html`/`.htm`), `ru.myx.ae3.l2.html.WebContextRss` (`.rss`/`.xrss`), and `ru.myx.ae3.l2.xhtml.WebContextXhtml` (the old DOM-based renderer, no longer registered for anything — see below) — all `ae3.sys.pkg.l2.tgt.html`. `.csv` is still a stopgap alias to `WebContextHtml` (no dedicated renderer exists). `WebContextRss` is a genuine find from this pass: it already existed, unmodified, in this unit's original file set, but the old hardcoded `WebContextType` enum's `HTTP_RSS` entry never actually constructed it — it aliased to `WebContextHtml` instead, leaving `WebContextRss` dead code. Missed on the first sweep of this move (only files the old enum explicitly named were searched for); caught by a `git ls-tree` sanity check afterward. Now correctly wired.
- `ru.myx.ae3.l2.text.WebContextText` (`.txt`) — `ae3.sys.pkg.l2.tgt.text`
- `ru.myx.ae3.l2.pdf.WebContextPdf` (`.pdf`) — `ae3.sys.pkg.l2.tgt.pdf`
- `ru.myx.ae3.l2.json.WebContextJson` (`.json`) — `ae3.sys.pkg.l2.tgt.json`. Distinct from that same unit's `JsonTargetContext`/`JsonReplyTargetContext` — an older, parallel, non-`WebContext` JSON-reply mechanism used directly by other L2 targets (e.g. `l2.tgt.dhtml`'s JS-client bootstrap), unrelated to this dispatch system; don't conflate the two.
- `ru.myx.ae3.l2.xml.WebContextXml`/`WebContextXmlXhtml`/`WebContextXmlAutoDetect` (`.xml`, `.xhtml`/`.xhtm`, and the wildcard `*`/`auto-detect`) — `ae3.sys.pkg.l2.tgt.xml`, see its CLAUDE.md

Each of those four units now `Requires: ae3.web` (to see the `WebContext` interface) purely for this — `i3.web` needs no reciprocal dependency on any of them, since it only ever reaches their classes through the registry's reflection, never a compile-time reference. `ru.myx.ae3.l2.xhtml.WebContextXhtml` (the old DOM-based, non-XSLT renderer) still exists in `ae3.sys.pkg.l2.tgt.html`, but nothing registers it for `xhtml`/`xhtm` any more — that shortName now belongs entirely to `ae3.sys.pkg.l2.tgt.xml`'s `WebContextXmlXhtml` (server-side XSLT superseded the DOM approach, which is why the feature exists at all — see below). It's reachable only via a direct `java.class/ru.myx.ae3.l2.xhtml.WebContextXhtml` reference if something still wants the old behavior.

## Headers arrive as request attributes, not a separate API

`ServeRequest` has no `getHeader(...)` method. Raw HTTP headers land verbatim in `query.getAttributes()`, keyed by their literal HTTP header name. `http/QueryHttp.java` passes the raw header `BaseMap` straight to `super(...)` as attributes; `SocketHandler.java` (which parses the socket) reads sibling headers the same way, e.g. `Base.getString(this.qHeaders, "Accept-Encoding", null)`. So an `Accept` header check reads as `Base.getString(query.getAttributes(), "Accept", "")`.

## WebContextXml stays pure — server-side XSLT lives in a sibling class, same unit now

`WebContextXml` itself moved to `ae3.sys.pkg.l2.tgt.xml` (package `ru.myx.ae3.l2.xml`) along with the two XSLT variants — see that unit's CLAUDE.md. Its `getResultReply()` is unmodified from its original behavior: for the `"xml".equals(layout)` branch, it always embeds the client-side `<?xml-stylesheet?>` PI (from `result.xsl`) and replies `text/xml`, regardless of `Accept`. This is deliberate — an explicit `___output=xml` request must always get pure, unnegotiated XML, and is why `l2.tgt.xml` never registers an override for shortName `xml` itself, only `xhtml`/`xhtm` and the wildcard.

`WebContextXmlAutoDetect`, same package, `extends WebContextXml` (same-package now, no cross-unit import needed) and overrides `getResultReply()`: it inspects the same `xml`-layout/`xsl` fields itself, and when the client's `Accept` header lists `application/xhtml+xml`, replies with a server-side XSLT-rendered `application/xhtml+xml` document instead — falling back to `super.getResultReply()` (the exact pure behavior above) whenever it doesn't apply or the transform fails.

Now wired via the registry's wildcard shortName (`WebContextOutputRegistry.WILDCARD_SHORT_NAME`, `"*"`): `ae3.sys.pkg.l2.tgt.xml` registers `WebContextXmlAutoDetect` for `extensions: ["*", "auto-detect"]`, so it becomes the auto-detect default whenever neither an explicit `___output` nor a recognized extension matched anything — and it's independently selectable via `___output=auto-detect` too. Which concrete `WebContext` class handles the "default" case is now a config-time decision (whichever descriptor wins `"*"` by priority), not something hardcoded in this package or requiring target-code edits.

## The one and only call site: WebTargetActor.apply()

`WebContextType.createMatchingContext(this, query)` is called from exactly one place in the whole unit: `WebTargetActor.apply()` (a `default` method on the `WebTargetActor` interface). Every target implementing `WebTargetActor` goes through this same call — there's no per-target override seam in Java, but since dispatch is now registry-driven end to end (see above), a target no longer needs one: registering a higher-priority `"*"` descriptor from any unit changes the default for every target without touching this unit's code.

## Gotcha (resolved, and now moot): WebContextXhtml's superclass lives in ae3.sys.pkg.l2.tgt.html

`WebContextXhtml` (the old DOM-based renderer — no longer registered for `xhtml`/`xhtm`, see above) `extends ru.myx.ae3.l2.xhtml.XhtmlDomTargetContext` — not in `ae3.sys.pkg.l2.tgt.dhtml` as first guessed (that unit's package is `ru.myx.ae3.l2.dhtml`, unrelated), but in `ae3.sys.pkg.l2.tgt.html`, alongside (but structurally separate from) that unit's generic `ru.myx.ae3.l2.html` hierarchy. It had a real static-initializer bug (two `Properties.setProperty` calls before the field's assignment — see `ae3.sys.pkg.l2.tgt.html`'s CLAUDE.md), fixed earlier this session. `WebContextXhtml` itself has since moved into that same unit too (package `ru.myx.ae3.l2.xhtml`, alongside its superclass) — it and its superclass live in the same repo and package now, so this was a real gotcha at the time (a `NoClassDefFoundError`/`ExceptionInInitializerError` when exercising `___output=xhtml`, unrelated to the XSLT feature, just discovered while working on it) but isn't a cross-unit surprise any more.

## `___output=xhtml` for the new XSLT-based renderer: resolved via the registration mechanism

The circular-dependency problem this used to describe (this unit would've needed to reference `WebContextXmlXhtml`, which lives in `l2.tgt.xml`, which already depends back on this unit) is resolved by construction now: this unit never references any concrete format's class by name, only by reflection through `java.class/FQCN` strings in JSON descriptors it doesn't ship — and per the section above, it doesn't even contain the concrete classes any more, only the generic interface/dispatcher/registry. `l2.tgt.xml` simply registers `xhtml`/`xhtm` → `WebContextXmlXhtml` in its own `settings/system/l3/targets/xhtml.json`, no compile-time edge required in either direction. See `WebContextOutputRegistry`'s javadoc for the descriptor shape, and `l2.tgt.xml`'s CLAUDE.md for what it registers.

Two considered-and-rejected alternatives from before this was built, kept for context: moving `WebContextXmlXhtml`/`XslServerRender`/the templates cache into this unit (undoes the separation, reintroduces skin-specific knowledge here), and a `WebTargetActor.createWebContext(query)` extension seam (a per-target override method) — superseded by the registry being genuinely pluggable at the format level, which covers the same need without a new interface method.

## `settings/web/outputs/{default,raw}.json`: separate, unrelated, still-unfinished scaffolding

Not to be confused with `settings/system/l3/targets/` above. `ae3-packages/ae3.web.server/settings/web/outputs/` holds two pre-existing (`"Initial"` commit) `"ae3.web/Output"`-typed descriptors with `ui` metadata (`title`/`abstract`/`download`/`preview`/`important`) and a `layouts` block — shaped like scaffolding for a UI-facing "export format picker" (abstract base + inheriting concrete entries via `layouts.parent`), not like content-negotiation dispatch. Both files' `context.reference` points at `WebContextType` itself, and nothing anywhere in this workspace's checked-out units reads this folder. Left untouched and unmerged with the registry above — investigated during this session, concluded to be a different, never-finished feature rather than an earlier version of the same one; revisit if a real consumer for it ever surfaces.
