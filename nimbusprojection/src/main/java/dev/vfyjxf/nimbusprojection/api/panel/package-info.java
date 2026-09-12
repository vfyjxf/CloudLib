/**
 * The panel vocabulary.
 * <ul>
 *   <li>{@link PanelSpec} — one in-world panel: {@code PanelKey} identity,
 *       anchor, {@code Presentation} descriptor, content factory,
 *       engagement declaration, {@code LayoutHint}, {@link Decay},
 *       container membership and policy overrides.</li>
 *   <li>{@link PanelGroup} — a declarative container: several panels under
 *       one affordance with {@link GroupRole} roles (primary-first
 *       engagement). Membership is explicit, same-anchor implicit, or
 *       cross-anchor via {@code PanelSpec.groupKey}.</li>
 *   <li>{@link Decay} — transient lifecycle: TTL + fade + linger-on-hover.</li>
 * </ul>
 */
package dev.vfyjxf.nimbusprojection.api.panel;
