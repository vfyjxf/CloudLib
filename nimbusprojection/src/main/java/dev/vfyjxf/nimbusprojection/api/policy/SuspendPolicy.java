package dev.vfyjxf.nimbusprojection.api.policy;

/**
 * Per-panel suspend/close policy — an open SPI evaluated each client tick.
 * <p>
 * Declared via {@code PanelSpec.suspendPolicy(...)}; {@code null} =
 * {@link #standard()}. Policies return a {@link SuspendVerdict}, so a
 * custom policy can distinguish "temporarily hidden" (SUSPEND) from
 * "permanently gone" (CLOSE) — e.g. a waypoint panel that survives a
 * real screen being open, or a machine panel that closes when its chunk
 * unloads.
 */
@FunctionalInterface
public interface SuspendPolicy {

    SuspendVerdict evaluate(SuspendContext ctx);

    /**
     * The default: dead anchor or dimension change → {@link SuspendVerdict#CLOSE};
     * a real screen open or the game paused → {@link SuspendVerdict#SUSPEND};
     * otherwise {@link SuspendVerdict#LIVE}.
     */
    static SuspendPolicy standard() {
        return ctx -> {
            if (!ctx.anchorAlive() || ctx.dimensionChanged()) return SuspendVerdict.CLOSE;
            if (ctx.screenOpen() || ctx.paused()) return SuspendVerdict.SUSPEND;
            return SuspendVerdict.LIVE;
        };
    }

    /** The panel suspends for nothing — it lives until its key disappears. */
    static SuspendPolicy immortal() {
        return ctx -> ctx.anchorAlive() ? SuspendVerdict.LIVE : SuspendVerdict.CLOSE;
    }

}
