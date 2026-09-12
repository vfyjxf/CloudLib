# Nimbus Projection — In-world UI API

A Jade-style "look at a thing, get its UI" mod — except the UI is a real
CloudLib widget tree living in the 3D world, and interaction goes far beyond
pointing a crosshair at pixels.

## Two-layer architecture

| Layer | Home | Contents |
|---|---|---|
| primitives | `cloudlib.api.ui.inworld` | what a world-anchored UI surface *is*: `InworldAnchor`, `InworldPlacement`, `Projection`, `InworldPanel`, `InworldPanelContext`, `InworldOverlayScreen`, `PanelChannel`, plus the widget-facing interaction contracts `InworldTraceable`, `WorldDraggable`/`WorldDrag`, `SplitPlan` |
| semantics | `nimbusprojection.api` | who provides panels and when they live/die, presentation policy, network model — `PanelSpec`, providers, `NimbusClient`/`NimbusServer`, shared panels, channels, presence |

CloudLib never depends on Nimbus; Nimbus builds on CloudLib. Third-party
consumers write against `nimbusprojection.api` and the CloudLib widget kit.

## Panel model

A panel = `PanelSpec(key, anchor, placement, content)`.

- **key** — identity. Providers re-offer keys each pass; a missing key
  closes the panel, a re-offered key preserves widget state.
- **anchor** (`InworldAnchor`, CloudLib) — block pos (+offset), entity, or
  dynamic supplier; resolved per frame.
- **placement** (`InworldPlacement`, CloudLib) — the presentation mode:
  - `Face` — flat on a block face, world-space quad, crosshair-ray input
  - `Floating` — screen-space, positioned near the projected anchor via
    the floating middleware pipeline
  - `Follow` — screen-space, pinned to the anchor's projection (nameplate)
  - `Dock` — stacked into a screen corner with a leader line back to the
    anchor; the folding/overflow budget system
  - `Expand` — world-space hologram at a free spot near the anchor,
    billboarded toward the player
- **content** — `InworldPanelContext → Widget`: an ordinary CloudLib
  widget tree.

Provisioning paths:

1. `NimbusClient.registerProvider(provider, interval)` — declarative scans
   ("a panel per synced BE in range", "the block I look at").
2. `NimbusClient.open(spec)` / `close(key)` — imperative.
3. `NimbusServer.share(spec)` — server-declared, broadcast to all watchers
   (see below).

## Interaction model

The runtime owns input; specs/widgets declare capabilities.

- **Soft focus** — the actionable panel nearest the look vector inside a
  cone is focused; angle dominates, distance breaks ties, hysteresis
  prevents flapping. No pixel-perfect aim.
- **Engage toggle** — interact key (V): tap on a dormant focus target opens
  it, tap on an engaged panel closes it, hold activates the pointer for
  click/drag. `onDemand` panels stay dormant (anchor affordance only)
  until engaged; `onDemand(false)` panels always present.
- **Widget contracts** (CloudLib) — a content widget opts in:
  - `InworldTraceable` — pointer strokes on the panel surface
    (Witness-style); a quick tap falls back to the panel's `action`.
  - `WorldDraggable` → `WorldDrag` — press hands the drag to the world:
    carried content floats at the view ray, world objects (containers)
    become drop targets, commit is server-authoritative, `SplitPlan`
    provides the shared distribution math.
- **Inspect** — hold-key (R): camera look captured by a transparent
  `InworldOverlayScreen`, every panel flattens to screen space under a
  free cursor with leader lines back to anchors.
- **Focus ring** — cycle-focus keys walk panels; keyboard input routes to
  the focused panel.

## Network model

Three mechanisms, each for a different shape of data:

1. **Expose** (CloudLib) — BE-backed panels read live state through
   `SyncedBlockEntity` handles (S→C, batched per tick) and send actions
   through `ReversedOnly` exposes (C→S). The default path.
2. **Panel channels** — for panels *without* a synced BE. Client sends via
   `InworldPanelContext.channel()` → `PanelChannel.sendToServer`;
   server-side receives at the `ServerPanelMessageHandler` declared on
   `SharedPanelSpec.channel(...)`. Server pushes via
   `SharedPanel.channel()` (`sendTo`/`broadcast`) → client's
   `PanelChannelHandler` declared on `PanelSpec.channel(...)`.
   Payloads are ordinary `CustomPacketPayload`s. **Server re-validates
   everything** — a channel payload is a request, not a fact.
3. **Shared panels** — `NimbusServer.share(SharedPanelSpec)` declares
   `(key, anchor, view, payload)` once; the runtime broadcasts it and every
   client in range materializes the panel through the `SharedPanelView`
   registered under `view` (`NimbusClient.registerView(id, codec, view)`).
   `SharedPanel.update(payload)` re-pushes data. This is how *every player
   sees the same UI*.

## Presence

Each client reports its own panel state (watching / engaged / dragging /
tracing); the server relays it to other watchers as `PresenceInfo`, read
via `NimbusClient.presence()`. This is how *other players' operations are
visible* — ghost affordances on shared panels.

## What is deliberately not here yet

This commit is the API skeleton — contracts only. The runtime (provider
reconciliation, soft focus, engage lifecycle, presentation solvers,
inspect screen, OIT rendering, channels, shared-panel broadcast, presence
relay) is rebuilt feature by feature against these types.

## Layout & compatibility rules carried from the prototype

- Screen-space chrome obeys a budget: fold to a strip → hide → `+N`
  overflow chip. Partial occlusion is acceptable; strict avoidance is not
  required.
- Folding state is per-presentation-scene, recomputed every frame — it
  must never leak between world and inspect presentation.
- World-space draws always declare their own depth state; 2D painter-order
  canvas batches must restore `GL_DEPTH_TEST` when done.
- Translations come from `src/main/lang/**.yaml` via the CloudLang plugin
  (`NimbusLang` constants), never handwritten json.
