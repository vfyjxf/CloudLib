package dev.vfyjxf.cloudlib.api.ui.floating;

import org.jetbrains.annotations.Nullable;

/**
 * A middleware in the floating positioning pipeline.
 * <p>
 * FloatingMiddleware are executed in order. Each one receives the current {@link FloatingState},
 * may modify coordinates / placement, and may return a {@link Result} that
 * indicates whether to reset the pipeline.
 */
public interface FloatingMiddleware {

    /**
     * @return the unique name of this middleware (used as key in middlewareData)
     */
    String name();

    /**
     * Executes this middleware.
     * <p>
     * Implementations should:
     * <ol>
     *   <li>Read the current state (x, y, placement, rects, etc.)</li>
     *   <li>Optionally modify x, y, placement via the state setters</li>
     *   <li>Optionally store data via {@link FloatingState#putData}</li>
     *   <li>Return a {@link Result} — use {@link Result#done()} for no reset,
     *       or {@link Result#reset(FloatingPlacement)} to restart the pipeline with a new placement</li>
     * </ol>
     *
     * @param state the current positioning state
     * @return the result controlling pipeline flow
     */
    Result run(FloatingState state);

    /**
     * Result of a middleware execution.
     * <p>
     * {@code done()} means the pipeline continues to the next middleware.
     * {@code reset(placement)} means the pipeline restarts from the beginning
     * with recomputed coordinates for the given placement.
     */
    record Result(@Nullable FloatingPlacement resetPlacement, boolean rectsChanged) {

        private static final Result DONE = new Result(null, false);

        /**
         * Continue to the next middleware without resetting.
         */
        public static Result done() {
            return DONE;
        }

        /**
         * Reset the pipeline with a new placement. Coordinates will be recomputed.
         *
         * @param placement the new placement to use
         */
        public static Result reset(FloatingPlacement placement) {
            return new Result(placement, false);
        }

        /**
         * Reset the pipeline with updated rects (e.g. after the size middleware resized the floating element).
         */
        public static Result resetRects() {
            return new Result(null, true);
        }

        /**
         * @return true if this result signals a pipeline reset
         */
        public boolean shouldReset() {
            return resetPlacement != null || rectsChanged;
        }
    }
}
