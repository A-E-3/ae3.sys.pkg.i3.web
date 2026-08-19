# MAGIC.md — ae3.sys.pkg.i3.web

Team-owned. Durable findings/gotchas for this repo, not investigation narration.

## For keeper-ae3

- **Dead-code-via-aliasing risk when sweeping an old dispatch enum**: a branch that aliases to another instead of constructing its own can be missed if the sweep only checks names the enum explicitly references (real case: `WebContextType`'s old `HTTP_RSS` entry aliased to `WebContextHtml` instead of constructing `WebContextRss`, leaving it dead code) — cross-check via `git ls-tree`/a full directory listing too, not just the enum's own named entries.
- **`___output=xhtml` has no registered consumer currently** (nothing constructs `WebContextXhtml`) — its superclass `XhtmlDomTargetContext` has a static-initializer bug fix pending build verification; see `ae3.sys.pkg.l2.tgt.html`'s `MAGIC.md`.
- **`___output=xhtml` circular-dependency design**: resolved by construction via `WebContextOutputRegistry` (this unit never references a concrete format's class by name, only by reflection). Two alternatives are rejected, don't re-propose without new information: (a) moving `WebContextXmlXhtml`/`XslServerRender`/the templates cache into this unit — undoes the separation, reintroduces skin-specific knowledge here; (b) a `WebTargetActor.createWebContext(query)` per-target override seam — superseded by the pluggable-at-format-level registry.
- **`settings/web/outputs/{default,raw}.json`**: a different, never-finished feature (a UI-facing export-format picker), not an earlier version of `WebContextOutputRegistry`. Left unmerged — revisit only if a real consumer for it surfaces.
