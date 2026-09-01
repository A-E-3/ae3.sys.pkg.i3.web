# MAGIC.md — ae3.sys.pkg.i3.web

Team-owned. Durable findings/gotchas for this repo, not investigation narration.

## For keeper-ae3

- **Dead-code-via-aliasing risk when sweeping an old dispatch enum**: a branch that aliases to another instead of constructing its own can be missed if the sweep only checks names the enum explicitly references (real case: `WebContextType`'s old `HTTP_RSS` entry aliased to `WebContextHtml` instead of constructing `WebContextRss`, leaving it dead code) — cross-check via `git ls-tree`/a full directory listing too, not just the enum's own named entries.
- **`___output=xhtml` has no registered consumer currently** (nothing constructs `WebContextXhtml`) — its superclass `XhtmlDomTargetContext` has a static-initializer bug fix pending build verification; see `ae3.sys.pkg.l2.tgt.html`'s `MAGIC.md`.
- **`___output=xhtml` circular-dependency design**: resolved by construction via `WebContextOutputRegistry` (this unit never references a concrete format's class by name, only by reflection). Two alternatives are rejected, don't re-propose without new information: (a) moving `WebContextXmlXhtml`/`XslServerRender`/the templates cache into this unit — undoes the separation, reintroduces skin-specific knowledge here; (b) a `WebTargetActor.createWebContext(query)` per-target override seam — superseded by the pluggable-at-format-level registry.
- **`settings/web/outputs/{default,raw}.json`**: a different, never-finished feature (a UI-facing export-format picker), not an earlier version of `WebContextOutputRegistry`. Left unmerged — revisit only if a real consumer for it surfaces.

## For keeper-ae3 / magic-tester

`ru.myx.ae3.i3.web.http.Main.main` does not itself open a socket or start serving — it only
does `String.valueOf(HttpProtocol.STARTED)`, a deliberate class-load trigger. `HttpProtocol`'s
own static initializer is what registers the HTTP-parsing `Produce` factories
(`FactoryHttpParser`/`FactoryHttpsParser`/`FactoryQueryStringToProperties`) and status
providers — real listening is `ae3.sys`'s own `transfer.nio` package's job (see that project's
own MAGIC.md), triggered separately by an `interfaces.xml` `<source><factory>ACCEPT</factory>...`
block. `ru.myx.ae3.i3.web.telnet.Main` is the same pattern for telnet.

`SocketHandler` (package-private, this package's real per-connection state machine) parses raw
HTTP bytes off a `TransferSocket`/`TransferTarget` (this framework's own binary-transfer
abstraction — not `java.net.Socket`/`OutputStream` directly) and renders a `ReplyAnswer` back
onto it. It is not reachable directly from outside this package; the only way to drive it is the
real `interfaces.xml`-configured `HTTP`/`HTTPS` filter + `ACCEPT` source pipeline.

## Real web-request dispatch and virtual-hosting, confirmed live

- Virtual hosting is by `Host` header, resolved against `settings/web/hosts/<pattern>.json`
  entries in the axiom root (`acm-cvs/sys-current/settings/web/hosts/`, or any project's own
  packaged `ae3-packages/*/settings/web/hosts/`) — an unmatched `Host` returns a real
  `200 OK` (not a connection-level error) with a `MessageShareUnknown` XML body,
  `<text>Share '&lt;host&gt;' is not known!</text>`.
- A `Host: ae3.local` request (its own `settings/web/hosts/ae3.local.json` entry is already
  wired in the packaged axiom) returns AE3's own built-in welcome/index page — safe,
  dependency-free, needs no ndss/application-specific content to be wired up. **Still returns
  byte-structurally-identical output regardless of `Accept` header even after the dispatch-race
  fix below landed** — this specific page's own `resultLayout` is already an `X-Debug-Origin:
  LAYOUT_FINAL` `ReplyAnswer` (built directly by the skin-layout walk, not a `layout="xml"`
  object), and both `WebContextXml`/`WebContextXmlAutoDetect` short-circuit identically on an
  already-final reply — so this welcome page can never demonstrate the fix's visible effect, by
  design, independent of whether the dispatch fix itself is correct. See "Dispatch-race fix"
  below for what was actually verified instead.
- `Accept: application/xhtml+xml` against the same `ae3.local` welcome page returns a real
  `500 Server error`, `Server-side XHTML render failed`, from the not-yet-committed
  `WebContextXmlXhtml`/`XslServerRender` path when it is present on the classpath — live
  confirmation that `show.xsl.tpl` genuinely fails to compile under the JDK's bundled XSLTC for
  this page, not just a static/theoretical claim.

## Output-format dispatch: WebContextType.createMatchingContext, a 4-step ordered chain

`WebContextType.createMatchingContext` (`java/ru/myx/ae3/i3/web/WebContextType.java:52-131`) tries,
in order, and returns on the first non-null match:

1. explicit `___output` query parameter (lines 56-67) — `WebContextOutputRegistry.createByKeyword`,
   resolved through the shared `keywords` category with `explicit=true` (bypasses the
   `priority>=0` gate implicit matches enforce).
2. URL resource-extension (lines 68-80) — `WebContextOutputRegistry.createByExtension`.
3. `Accept`-header content-type match (lines 81-113, since the dispatch-race fix below landed —
   was 81-92) — `WebContextOutputRegistry.createByContentTypes`, plus the tier-3 `WebContextXml`→
   `WebContextXmlAutoDetect` substitution.
4. the `auto-detect`/`*` wildcard (lines 114-124, was 93-103) —
   `WebContextOutputRegistry.createByKeywords(DEFAULT_MATCHER_SHORT_NAMES, ...)`, only reached once
   steps 1-3 all miss.

**Tier-3 fast path.** Before the general comma-split/parse, `createMatchingContext` checks the raw
`Accept` header for the literal substring `application/xhtml+xml` and, on a match, calls
`WebContextOutputRegistry.createByContentTypes` with a single-element array holding just that
content type (`WebContextType.XHTML_CONTENT_TYPES`, a `static final` constant, not allocated per
call). This can only reach the same outcome the general parse path would reach for that token — it
short-circuits the common case, never changes what's accepted. Any header without that exact
substring falls through to the general parse unaffected.

Step 3's tie-break is last-Accept-token-wins, not a numbered priority tier:
`WebContextOutputRegistry.findBest` (`WebContextOutputRegistry.java:182-198`) iterates the Accept
header's own tokens in header order (not the registry's), and on equal priority
(`best.priority <= factory.priority`) the later-matching token replaces the earlier one. `xml.json`/
`xhtml.json`/`html.json` (`ae3.sys.pkg.l2.tgt.xml`'s own target descriptors, see that project's own
MAGIC.md) all carry `"priority": 0`. For a real browser Accept header
(`text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8`), `application/xml` is the last
matching token in header order, so `xml.json`'s factory (plain `WebContextXml`) wins the tie —
confirmed the class actually constructed and replying for real browser and explicit-`Accept:text/xml`
traffic today, both by source-tracing and by live HTTP probes (a local isolated instance and
`https://ae3.myx.nz/`: `default`/`browser-typical`/`xml-explicit` all `200`, `text/xml`, identical
body). `WebContextXmlAutoDetect` sits only in the tier-4 wildcard slot and is never reached for any
Accept header carrying a recognized content-type token.

## Dispatch-race fix — Design B, implemented and verified

Real browser traffic tying at tier 3 and resolving to plain `WebContextXml` before the tier-4
auto-detect wildcard was a dispatch race, not a bug in any single descriptor. Two designs were
compared:

- **Design A (rejected)**: redirect `xml.json`'s `context.reference` to `WebContextXmlAutoDetect`,
  with self-detection logic added inside that class to stay byte-identical for explicit-XML
  requests. Selling point: no elders' files edited beyond one descriptor reference. Rejected
  because it would have needed to re-parse `___output`/the URL extension a second time inside a
  constructor with no access to which dispatch tier actually picked it, and it muddies `xml.json`'s
  own identity as the explicit-XML-only handler.
- **Design B (chosen and implemented)**: the only new code goes inside `createMatchingContext`'s
  own tier-3 block (`WebContextType.java`, now lines 88-110) — when
  `WebContextOutputRegistry.createByContentTypes` resolves to a plain `WebContextXml` instance
  (checked by exact class name, never `instanceof`, so `WebContextXmlXhtml`/`WebContextXmlAutoDetect`
  are never re-matched), the same `auto-detect` slot tier 4 would have reached anyway is substituted
  in. Reaching tier 3 at all already proves neither tier-1 `___output` nor tier-2 extension matched,
  so no re-derivation of "was this explicit" is needed. `xml.json`, `WebContextXml.java`,
  `WebContextXmlAutoDetect.java`, and `WebContextXmlXhtml.java` (all under `ae3.sys.pkg.l2.tgt.xml`)
  stayed completely untouched — `xml.json`/`WebContextXml` keeps being the legitimately
  explicit-XML-only handler by identity, and the already-built-and-tested
  `WebContextXmlAutoDetect`/`WebContextXmlXhtml`/`XslServerRender` classes are reused unchanged,
  with no duplicated/drift-prone detection logic.

**Real implementation deviation from the design's own literal wording, and why**: "construct
`WebContextXmlAutoDetect` instead" reads as a direct `new WebContextXmlAutoDetect(target, query)`.
That does not compile — this project's own `.classpath` (`ae3.api`/`ae3.sdk`/`ae3.sys.pkg.base`
only) carries no dependency on `ae3.sys.pkg.l2.tgt.xml`, confirmed by an independent `javac` run
against exactly that classpath. Implemented instead as
`WebContextOutputRegistry.createByKeyword(WebContextOutputRegistry.AUTO_DETECT_SHORT_NAME, target, query, false)`
— the identical reflective-lookup idiom (`Class.forName`/`Constructor`, no compile-time class
reference) tiers 1 and 4 already use in this same file. Zero new imports, zero `.classpath` change,
and arguably a closer fit to this class's own "hardcodes no `WebContext` subclass" principle than a
literal `new` would have been.

Real tradeoff, named rather than glossed: Design B does touch the shared dispatch mechanism every
output type flows through, which this class' own javadoc states hardcodes no `WebContext` subclass
itself and "only dispatches, it doesn't implement targets" (this file's own top-of-class comment,
`WebContextType.java:11-15`) — the new tier-3 substitution is a deliberate, narrowly-scoped,
clearly-commented exception to that stated principle, not a silent violation of it. `xhtml.json`/
`html.json` need no changes under either design — confirmed they never win the tier-3 tie for
standard browser Accept headers.

**Verification, and a real gap found in the existing probe tooling.** Direct proof the substitution
fires: a temporary diagnostic (added, exercised, then fully reverted before landing) confirmed the
tier-3 substitution resolves to `ru.myx.ae3.l2.xml.WebContextXmlAutoDetect` for both
`browser-typical` and `xml-explicit` Accept headers, and correctly does not fire for
`xhtml-explicit` (already resolves to the more-specific `WebContextXmlXhtml`). `curl`-level output
from `unit-test/magic-tester/verify-ae3-web-dispatch.sh run`, run immediately before and after the
change, was unchanged either side against `Host: ae3.local`'s welcome page — **not a fix failure**:
that page's `resultLayout` is already a final `ReplyAnswer` (see the welcome-page bullet above),
which both `WebContextXml` and `WebContextXmlAutoDetect` pass through unchanged by design regardless
of which one is constructed. A second local probe (a 404 on a nonexistent path) also showed no
difference, for an unrelated reason: that reply's XML root carries `layout="message"`, and
`WebContextXmlAutoDetect`'s render-upgrade branch only fires for `layout="xml"` — a "message"-layout
error reply is correctly not eligible either. **Neither locally-reachable probe target can exercise
this fix's visible effect** — that needs real `layout="xml"` + non-empty-`xsl` content that isn't
already a final `ReplyAnswer` (ordinary `ndss`-family pages, not anything wired into this isolated
instance or the live `ae3.local`/`ae3.myx.nz` welcome page). Compile-clean confirmed directly: a real
`javac` run against this project's actual declared classpath (`-Xlint:all`) exits 0, zero
errors/warnings; the real `bin/WebContextType.class` was also recompiled and postdates its `.java`.

Implemented 2026-08-26, human-owner go-ahead given directly via `AskUserQuestion`. Not re-verified
live against `https://ae3.myx.nz/` post-fix — nothing has been built/deployed there from this
change, so a live re-probe could currently only reproduce the pre-fix baseline already documented
above, not demonstrate anything about the fix.

## Dispatch-race fix's visible render effect — first real empirical proof, 2026-08-26

Closes the gap this file's own "Verification" section above left open ("neither locally-reachable
probe target can exercise this fix's visible effect"). `unit-test/magic-tester/verify-ae3-web-dispatch.sh`
gained two dedicated test pages (`run-testpages` / `probe-testpages`, `testpages/` subfolder — see
`unit-test/magic-tester/README.md`) whose resolved layout genuinely is `{layout:"xml", xsl:...,
content:...}` and is not already a final `ReplyAnswer` — the shape the welcome page and a 404 both
structurally cannot produce. One page names `showState.xsl` (XSLTC-compile-clean); a second,
deliberately separate page names `show.xsl` (the known-failing template), to reproduce that failure
through the real dispatch/render path rather than conflating the two cases.

**Dispatch-level result: the fix works, confirmed for the first time against genuinely non-final
content.** `browser-typical`/`xml-explicit` land on `WebContextXmlAutoDetect` and `xhtml-explicit`
on `WebContextXmlXhtml` exactly as designed — visibly different behavior from `default-star-star`
(`xhtml-explicit` returns a real `500` where `default-star-star` returns `200`), proving Design B's
tier-3 substitution reaches real render-attempting code for a page the welcome page/404 pair never
could.

**Render-level result: still blocked, for a wider reason than previously known.** Both test pages —
including the one naming a template independently confirmed XSLTC-compile-clean — fail live, because
`XslServerRender`'s template cache is all-or-nothing: one broken template (`show.xsl.tpl`) poisons
compilation for every template in the folder, not just its own. Full mechanism and a positive-control
proof (the same `showState.xsl` page renders correctly, real XHTML body, once `show.xsl.tpl` alone is
excluded from the scan) are recorded in `ae3.sys.pkg.l2.tgt.xml`'s own `MAGIC.md` — not duplicated
here, since the mechanism itself lives in that project's `XslServerRender`/`SupplierVfsFolderXslTemplatesCached`.

**Practical consequence for the still-open `show.xsl.tpl`-handling decision**: option (a) ("ship the
dispatch-race fix alone, ordinary pages keep serving raw XML") previously read as leaving only
`show.xsl`-routed pages unrendered. It actually leaves **every** `layout:"xml"` page unrendered,
project-wide, until `show.xsl.tpl` is resolved — confirmed empirically, not inferred.

## Skin assignment for Share-based web contexts (skin-standard-xml) — keeper-acm cross-check, 2026-08-26

Dispatched cross-check for the XHTML/AE3 server-side render epic: is `skin-standard-xml` genuinely
the live, unoverridden skin for every `ndss`-family site, the assumption the server-side render
mechanism (`XslServerRender`, `ae3.sys.pkg.l2.tgt.xml`) and its whole `Make*ReplyFn.js` family are
built on.

- **Mechanism, confirmed by source.** `WebTargetShareObject.onDrill()` (this project,
  `java/ru/myx/ae3/i3/web/WebTargetShareObject.java:165-170`) assigns a hardcoded static
  `SKIN_STANDARD_XML` (built directly from `resources/skin/skin-standard-xml`, not looked up
  per-share by name) to `context` as a **fallback only**: `if (context.getSkin() == null) {
  context.doSetSkin(WebTargetShareObject.SKIN_STANDARD_XML); }` — applied to *every* `Share`-typed
  web target (any host whose `settings/web/hosts/<host>.json` resolves to `{"type":"ae3.web/Share",
  "reference": ...}`), before that share's own scripted `onDrill` hook runs. No other code path in
  this workspace calls `context.doSetSkin(...)` earlier in a Share-target request's lifecycle — the
  only other `doSetSkin` callers are unrelated target types (`WebTargetUnknown`/`WebTargetError`/
  `WebTargetServiceStopped`/`WebTargetUpgradeYourBrowser`, XHTML/HTML folder targets, AWT/SWT
  targets).
- **A real host-level override mechanism does exist in this framework** — `settings/web/hosts/
  <host>.json` can carry `{"type":"skin","skin":"<skin-name>"}` instead of a `Share` reference,
  which bypasses `WebTargetShareObject` (and its `SKIN_STANDARD_XML` fallback) entirely. Confirmed
  real, in-use examples elsewhere in this workspace: `acm-skin-acmcms-info/settings/web/hosts/
  acmcms-info.json`, `ae3-info/.../settings/web/hosts/ae3-info.json`.
- **Empirical check across every `ndss`-named host config present in this workspace**:
  `ndss.local.json`, `ndss.*.json`, `ndss.**.json`, `ndss.macmyx.local.json` (both copies),
  `svc-proxy.ndss.local.json`. Every one resolves to a `Share`-typed reference
  (`com.ndmsystems.ndss/wapi/NdssWebShare` directly, or an alias chain terminating there). **None
  uses the `{"type":"skin",...}` override shape.** All funnel through `WebTargetShareObject.onDrill()`'s
  `SKIN_STANDARD_XML` fallback.
- **Corroborating in-repo assertion, independent of this cross-check.** `ae3.sys.pkg.l2.tgt.xml`'s
  `ReduceDataViewGrid{Html,Xls,Txt,Pdf}Fn.js` each carry the identical comment: "skin-standard-xml
  (the skin assigned to every Share-based context, HTML included — see `WebTargetShareObject.onDrill()`)".
- **Real scope boundary of this check, stated plainly.** This workspace holds only *local/dev*
  host-alias configs for `ndss` (`ndss.local`, `ndss.macmyx.local`, etc.) — it does not contain the
  real production `ndss` fleet's own deployed host-routing config (infra-owned, outside this Eclipse
  workspace, outside this cross-check's own scope). `NdssWebShare` itself
  (`com.ndmsystems.ndss/wapi/NdssWebShare`) is not source-present in this workspace either —
  external/deployed class — so its own `onDrill`/`onHandle` script body can't be inspected here to
  rule out a runtime skin override happening *inside* it, after `WebTargetShareObject` already set
  the fallback.
- **Conclusion.** `skin-standard-xml` is confirmed, by mechanism and by every config actually present
  in this source tree, to be the unconditional fallback skin for every `Share`-typed `ndss` host known
  to this workspace, with no host-level override present anywhere in it. The one structural gap —
  the real production fleet's own host-routing config and `NdssWebShare`'s own script body living
  outside this workspace — was not resolvable from within this cross-check's own scope.

## `WebContextOutputRegistry.runDescriptorReducer` silently drops a descriptor on any
`Class.forName`/`getConstructor` failure, 2026-08-26

Found live-verifying `?___output=xls` (`MakeDataViewReplyFn.js`'s Phase 0, full detail in
`ae3-interfaces.backlog.md`'s own `Context Facts`). `runDescriptorReducer` (`WebContextOutputRegistry.java`)
wraps its `Class.forName(reference).getConstructor(...)` call in `catch (final Exception e) { return
result; }` — no logging, no re-throw, the descriptor (and every keyword it would have registered) is
just silently absent from the registry. A real request explicitly asking for `___output=xls` (with a
real, present `xls.json` descriptor and a real, present `WebContextXls` class on the classpath) fell
through all 4 dispatch tiers to the tier-4 auto-detect wildcard instead — confirmed via the reply's own
`X-Debug-Origin: WebContextXmlAutoDetect` header — with zero trace in DEBUG-level server logs. Not
fixed this pass (out of Phase 0's own authorized scope); the exact reason `Class.forName`/
`getConstructor` fails for this one descriptor was not pinned down further (would need instrumenting
this already-built file, which needs its own check-back first).

## `WebContextType.parseAcceptContentTypes` reimplements MIME-value parsing instead of using `Flow.mimeAttribute`

The codebase already has a real, generic, name-agnostic MIME single-value-plus-parameters parser —
`Flow.mimeAttribute(owner, title, attributeValue)` (`ae3.api/java/ru/myx/ae3/flow/Flow.java`), used
elsewhere via `Message.subAttributeValue` (`ae3.sdk/java/ru/myx/ae3/help/Message.java`) for
Content-Type/charset extraction. It splits a value at the first `;`, then parses the remaining
`;`-separated `key=value` (or bare-flag) parameters, handling quoted values. It parses one MIME value
at a time and does not split on commas — a caller with a comma-separated list (an HTTP `Accept`
header) has to split on commas itself first, then call this per token.

`WebContextType.parseAcceptContentTypes` (this package) does not use it — it reimplements a cruder
ad-hoc parse of the `Accept` header that discards parameters and q-values entirely rather than
extracting and using them. Confirmed gap, not yet fixed — flagged here for future work, not addressed
in this pass.
