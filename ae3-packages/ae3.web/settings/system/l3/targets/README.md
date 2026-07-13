Descriptors for `ru.myx.ae3.i3.web.WebContextOutputRegistry`, scanned from `/union/settings/system/l3/targets/*.json` across every unit.

This unit (`ae3.web` / `ae3.sys.pkg.i3.web`) only reads and dispatches - it does not implement any target/output itself, so it ships no descriptors here. Each unit that actually implements a target (an `l2.tgt.*` unit, or any other) contributes its own `*.json` file under its own `ae3-packages/<pkg>/settings/system/l3/targets/`.

Descriptor shape:

```json
{
	"extensions" : ["xhtml", "xhtm"],
	"contentTypes" : ["application/xhtml+xml"],
	"default" : false,
	"priority" : 0,
	"context" : {
		"reference" : "java.class/fully.Qualified.ClassName"
	}
}
```

- `extensions` and `contentTypes` are both matched the exact same way, against either the explicit `___output` request parameter or the request's file extension - `___output=text/html` has always been just as valid as `___output=html`. Two descriptors may register the same value; the higher `priority` wins.
- `default: true` additionally registers this descriptor as the auto-detect/no-explicit-output fallback, used when neither an explicit `___output` nor a recognized extension matched anything. Whichever descriptor wins the default slot (by priority, possibly from a different unit than any of the concrete formats) gets constructed, and it's then that class's own job to inspect the request (e.g. the `Accept` header) and decide what to actually do - see `ae3.sys.pkg.l2.tgt.xml`'s `WebContextXmlAutoDetect`.
- `context.reference` must be `java.class/<FQCN>` naming a public class with a `(TargetInterface, ServeRequest)` constructor.
