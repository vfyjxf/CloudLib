# Nimbus Projection — In-world UI API

A Jade-style "look at a thing, get its UI" mod — except the UI is a real
CloudLib widget tree living in the 3D world, and interaction goes far beyond
pointing a crosshair at pixels.

## Two-layer architecture

| Layer | Home | Contents |
|---|---|---|
| primitives | `cloudlib.api.ui.inworld` | what a world-anchored UI surface *is*: `PanelKey`, `InworldAnchor` (+`AnchorCodec` registry), `Presentation` (open, 6 builtin descriptors), `Projection`, `LayoutHint`/`OcclusionClass`, `InworldPanel`, `InworldPanelContext`, `InworldOverlayScreen`, `PanelChannel`, and the widget-facing interaction contracts `InworldTraceable`, `WorldDraggable`/`WorldDrag`, `SplitPlan` |
| semantics | `nimbusprojection.api` | who provides panels and when they live/die, presentation solving, policy overrides, network model |

CloudLib never depends on Nimbus; Nimbus builds on CloudLib. Third-party
consumers write against `nimbusprojection.api` and the CloudLib widget kit.

## Panel model

A panel = `PanelSpec(PanelKey, anchor, presentation, content)`.

- **key** — `PanelKey(namespace, path)`, serializable; providers re-offer
  keys each pass, missing keys close, re-offered keys preserve widget
  state. Path may be hierarchical; the parent path derives the default
  zoning group.
- **anchor** (`InworldAnchor`, open) — block pos (+offset), entity, fixed
  pos, or dynamic supplier; resolved per frame. Custom anchors become
  network-shareable by registering an `AnchorCodec`.
- **presentation** (`Presentation`, open) — descriptor records:
  `Face` / `Floating` / `Follow` / `Dock` / `Expand` / `InspectOnly`.
  Custom presentations = a descriptor record + a `PresentationDriver`
  registered on `NimbusClient.registerPresentation`.
- **content** — `InworldPanelContext → Widget`: an ordinary CloudLib
  widget tree.

### Containers

`PanelGroup(key, anchor, members)` — offered via `PanelSink.offerGroup`.
Membership: explicit `members` / same-anchor implicit merge (escape via
`spec.standalone(true)`) / cross-anchor via `spec.groupKey(groupKey)`.
Roles (`GroupRole`): `PRIMARY` opens on engage, `SECONDARY` expands after,
`AMBIENT` always visible (implicit: `onDemand(false)` members).

### Lifecycle

- `decay(Decay(ttl, fade, lingerOnHover))` — transient panels: toasts,
  pick-up feedback. TTL from creation, re-offers don't extend.
- `suspendPolicy(SuspendPolicy)` — open SPI returning
  `LIVE`/`SUSPEND`/`CLOSE` per tick; `SuspendPolicy.standard()` is the
  default (dead anchor/dimension change → CLOSE, screen/pause → SUSPEND).

### Layout participation

All space-budget declarations live in one object — `spec.layoutHint(hint)`
or `spec.hint().zone(...)`:

- `zone`/`zoneLimit` — displaced panels merge into a `+N` rail
- `offscreenIndicator` — collapse to an edge marker when the anchor leaves
  the view
- `foldable` — join the fold→hide→`+N` budget chain
- `occlusionTolerance` — acceptable chrome-overlap fraction ("a little
  cover is fine")
- `occlusion` — `OCCLUDED_BY_WORLD` / `OCCLUDED_BY_UI_ONLY` /
  `ALWAYS_ON_TOP`

## Presentation drivers

`PresentationDriver<P>` — the open SPI solving one `Presentation.type()`:

- `resolve(ctx) → PanelGeometry` — world quad or screen rect this frame
- `pick(ctx) → uv` — pointer (ray or screen point) → surface-local coords
- `flatten(ctx) → FlattenedGeometry` — what it becomes in inspect mode
- `inspectPolicy()` — FLATTEN / HIDDEN / CUSTOM

Occlusion avoidance, chrome collision and focus participation are the
runtime's uniform pipeline — drivers only report geometry. The six
builtins are ordinary drivers over the same SPI.

## Interaction model

- **Soft focus** — cone-based auto-selection, angle-dominant scoring,
  incumbent hysteresis. `spec.focusPolicy(FocusPolicy)` overrides scoring
  (SPI; hysteresis stays runtime-owned). Single primary focus.
- **Engage toggle** — interact key: tap opens dormant focus target, tap
  closes engaged panel, hold activates the pointer. `onDemand` panels stay
  dormant until engaged.
- **Widget contracts** — `InworldTraceable` (surface strokes),
  `WorldDraggable`/`WorldDrag` (drag into world, server-authoritative
  commit, `SplitPlan` math). Richer forms (radial menus) are widget
  shapes, not new verbs.
- **Inspect** — hold-key flat projection; `InspectOnly` panels exist only
  there. Keyboard cycles the focus ring.

## Network model

1. **Expose** (CloudLib) — BE-backed panels: `SyncedBlockEntity` S→C,
   `ReversedOnly` C→S. The default path.
2. **Panel channels** — `context.channel().sendToServer(payload)` (C→S,
   server handler on `SharedPanelSpec.channel`); `SharedPanel.channel()`
   `sendTo`/`broadcast` (S→C, client handler on `PanelSpec.channel`).
   Contract: per-payload delivery, **ordering not guaranteed**.
3. **Shared panels** — `NimbusServer.share(SharedPanelSpec)` broadcasts
   `(PanelKey, anchor[codec'd], view, payload)`; clients materialize via
   registered `SharedPanelView`. Two-tier permission:
   `visibleTo` (who sees it at all) and `canInteract` (who may operate —
   others get a read-only view and their input is dropped).
   `SharedPanel.update(payload)` re-pushes data.
4. **Presence** — clients report their interaction state per panel key;
   the server relays to co-watchers. Applies to shared panels and to
   provider panels inside a declared shared domain
   (`ProviderOptions.shared()` — the provider certifies its keys encode
   world identity).

## Layout & compatibility rules carried from the prototype

- Screen-space chrome obeys a budget: fold → hide → `+N` overflow chip.
  Partial occlusion is acceptable; strict avoidance is not required.
- Folding state is per-presentation-scene, recomputed every frame.
- World-space draws always declare their own depth state; 2D painter-order
  canvas batches must restore `GL_DEPTH_TEST` when done.
- Translations come from `src/main/lang/**.yaml` via the CloudLang plugin
  (`NimbusLang` constants), never handwritten json.

## What is deliberately not here yet

This is the API skeleton — contracts only. The runtime (provider
reconciliation, grouping/engage, drivers, inspect, rendering, channels,
shared-panel broadcast, presence relay) is built feature by feature
against these types.
