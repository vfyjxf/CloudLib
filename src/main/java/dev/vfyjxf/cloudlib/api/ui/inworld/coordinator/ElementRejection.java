package dev.vfyjxf.cloudlib.api.ui.inworld.coordinator;

import dev.vfyjxf.cloudlib.api.math.Rect;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * A structured rejection — the negotiation fuel the coordinator hands an
 * element when it could not grant a proposal. The {@link #reason} drives how
 * far the element's {@link VariantLadder} degrades on the retry;
 * {@link #blocker} names the committed element (not the exclusion) in the
 * way; {@link #suggestedRect} is a free-rect hint the element may or may not
 * use.
 *
 * @param reason why the proposal was rejected
 * @param blockerId the id of the committed element whose rect blocked the
 *        candidate, when the reason is {@link RejectionReason#overlap};
 *        {@code null} otherwise
 * @param suggestedRect a rectangle the coordinator knows is free and large
 *        enough for the rejected variant, in gui pixels; {@code null} when no
 *        such rectangle exists
 */
public record ElementRejection(RejectionReason reason, @Nullable String blockerId, @Nullable Rect suggestedRect) {

    public ElementRejection {
        Objects.requireNonNull(reason, "reason");
    }

    /** A rejection with no blocker and no suggestion. */
    public static ElementRejection of(RejectionReason reason) {
        return new ElementRejection(reason, null, null);
    }
}
