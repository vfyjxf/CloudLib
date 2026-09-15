package dev.vfyjxf.cloudlib.api.ui.inworld.layout;

import org.jetbrains.annotations.Nullable;

/**
 * What a policy needs to evaluate a request this frame: the frame being
 * solved, the sampled source snapshot and the panel's pose last frame (for
 * continuity), which may be absent on the first solve.
 */
public record IntentContext(LayoutFrame frame, SourceSnapshot source, @Nullable Pose previousPose) {}
