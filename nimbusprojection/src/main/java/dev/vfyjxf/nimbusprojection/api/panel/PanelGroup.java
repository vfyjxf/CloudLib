package dev.vfyjxf.nimbusprojection.api.panel;

import dev.vfyjxf.cloudlib.api.ui.inworld.InworldAnchor;
import dev.vfyjxf.cloudlib.api.ui.inworld.PanelKey;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A declarative panel container: several panels organized under one
 * affordance on one anchor.
 * <p>
 * Offered through {@code PanelSink.offerGroup}. Membership is resolved
 * three ways:
 * <ul>
 *   <li><b>explicit</b> — panels listed in {@link #members};</li>
 *   <li><b>same anchor</b> — standalone specs resolving to the group's
 *       anchor merge in implicitly (unless they opted out via
 *       {@code PanelSpec.standalone()});</li>
 *   <li><b>named join</b> — a spec declaring
 *       {@code PanelSpec.groupKey(this.key())} joins across anchors — the
 *       multi-block-structure case.</li>
 * </ul>
 * Engagement is primary-first: the container affordance opens the
 * {@link GroupRole#primary} member; {@link GroupRole#secondary} members
 * expand afterwards, {@link GroupRole#ambient} members are always on.
 */
public final class PanelGroup {

    /**
     * One grouped panel.
     *
     * @param spec the member panel
     * @param role its claimed role — see {@link GroupRole}
     */
    public record Member(PanelSpec spec, GroupRole role) {}

    private final PanelKey key;
    private final InworldAnchor anchor;
    private final List<Member> members = new ArrayList<>();

    private PanelGroup(PanelKey key, InworldAnchor anchor) {
        this.key = key;
        this.anchor = anchor;
    }

    public static PanelGroup of(PanelKey key, InworldAnchor anchor) {
        return new PanelGroup(key, anchor);
    }

    /** The container's identity — the reconcile unit, and the join target of {@code PanelSpec.groupKey}. */
    public PanelKey key() {
        return key;
    }

    /** Where the group's affordance lives. */
    public InworldAnchor anchor() {
        return anchor;
    }

    public List<Member> members() {
        return Collections.unmodifiableList(members);
    }

    /** Adds a member with an explicit role. */
    public PanelGroup member(PanelSpec spec, GroupRole role) {
        members.add(new Member(spec, role));
        return this;
    }

    /** Adds a member claiming its own declared {@link PanelSpec#groupRole()}. */
    public PanelGroup member(PanelSpec spec) {
        return member(spec, spec.groupRole());
    }
}
