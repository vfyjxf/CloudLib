---
name: cloudlib-moddev-ui
description: Use when a ModDevMCP client session is on a CloudLib screen and standard ui inspection is not enough to understand widget hierarchy, layout, hit targets, or widget-specific properties.
---

# CloudLib ModDev UI

## Overview

Use this after `moddev-usage` has resolved a live client session. Prefer standard `status.live_screen`, `ui.inspect`, `ui.snapshot`, and `ui.action` first. Reach for CloudLib tools only when you need deeper widget structure, layout diagnostics, precise hit testing, or widget property export.

## When to Use

- `status.live_screen` shows a CloudLib-driven screen
- `ui.snapshot` or `ui.inspect` reports `driverId=cloudlib-screen`
- standard `ui.*` tells you that a widget exists, but not enough about its path, bounds, or state
- you need an exact `targetId` before calling `ui.action`
- you need proof about a layout or focus issue on a CloudLib `BasicScreen`

Do not use this skill as the entry point for service discovery. Start with `moddev-usage`.

## Preferred Order

1. Use `moddev-usage` to confirm `serviceReady=true`, `gameReady=true`, and a connected `client`.
2. Call `status.live_screen`.
3. Call `ui.snapshot` or `ui.inspect`.
4. Continue only if `driverId=cloudlib-screen`.
5. Use the smallest CloudLib tool that answers the question.
6. Reuse the returned `targetId` with standard `ui.action`.
7. Use `ui.capture` when you need image proof.

## Quick Reference

| Need | Tool | Notes |
|---|---|---|
| Full widget hierarchy | `cloudlib.ui.tree` | Best for parent/child structure and stable widget paths |
| Layout and bounds diagnostics | `cloudlib.ui.layout_report` | Use for absolute positions, sizes, and layout-related inspection |
| Precise hit path at coordinates | `cloudlib.ui.hit_test` | Best way to turn a pixel location into a widget path |
| Detailed widget properties | `cloudlib.ui.widget_info` | Use for focus, text, placeholder, editable, style-derived values |
| Real interaction | `ui.action` | Use the `targetId` returned by the CloudLib tools |
| Visual proof | `ui.capture` | Use framebuffer capture for reliable screenshots |

## Tool Selection

Use `cloudlib.ui.tree` when you need to understand screen structure or compare repeated siblings.

Use `cloudlib.ui.layout_report` when the question is about geometry, clipping, absolute bounds, or layout state.

Use `cloudlib.ui.hit_test` when you know the coordinates but not the target path.

Use `cloudlib.ui.widget_info` when you already have a `targetId` and need deep properties for one widget.

## Common Mistakes

- Do not guess that a screen is CloudLib-backed. Check `driverId=cloudlib-screen` first.
- Do not jump to `cloudlib.ui.tree` for every question. Use the smallest tool that fits.
- Do not keep clicking by coordinates after you already have a stable `targetId`; switch back to `ui.action`.
- Do not assume every visual element is a separate widget. Some screens draw complex visuals inside a canvas-like widget.
- Do not bypass ModDevMCP with OS-level mouse or keyboard automation.
