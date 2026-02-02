package dev.vfyjxf.cloudlib.api.ui.texture;

/**
 * Easing function that controls the rate of change of an animation.
 * <p>
 * Both input and output are in the range 0.0 to 1.0.
 */
@FunctionalInterface
public interface Easing {

    float apply(float t);

    Easing LINEAR = t -> t;

    // Quad
    Easing QUAD_IN = t -> t * t;
    Easing QUAD_OUT = t -> t * (2 - t);
    Easing QUAD_IN_OUT = t -> t < 0.5f ? 2 * t * t : -1 + (4 - 2 * t) * t;

    // Cubic
    Easing CUBIC_IN = t -> t * t * t;
    Easing CUBIC_OUT = t -> {
        float t1 = t - 1;
        return t1 * t1 * t1 + 1;
    };
    Easing CUBIC_IN_OUT = t -> t < 0.5f ? 4 * t * t * t : (t - 1) * (2 * t - 2) * (2 * t - 2) + 1;

    // Quart
    Easing QUART_IN = t -> t * t * t * t;
    Easing QUART_OUT = t -> {
        float t1 = t - 1;
        return 1 - t1 * t1 * t1 * t1;
    };

    // Sine
    Easing SINE_IN = t -> 1 - (float) Math.cos(t * Math.PI / 2);
    Easing SINE_OUT = t -> (float) Math.sin(t * Math.PI / 2);
    Easing SINE_IN_OUT = t -> 0.5f * (1 - (float) Math.cos(Math.PI * t));

    // Expo
    Easing EXPO_IN = t -> t == 0 ? 0 : (float) Math.pow(2, 10 * (t - 1));
    Easing EXPO_OUT = t -> t == 1 ? 1 : 1 - (float) Math.pow(2, -10 * t);

    // Elastic
    Easing ELASTIC_OUT = t -> {
        if (t == 0 || t == 1) return t;
        return (float) (Math.pow(2, -10 * t) * Math.sin((t - 0.075) * (2 * Math.PI) / 0.3) + 1);
    };

    // Bounce
    Easing BOUNCE_OUT = t -> {
        if (t < 1 / 2.75f) {
            return 7.5625f * t * t;
        } else if (t < 2 / 2.75f) {
            t -= 1.5f / 2.75f;
            return 7.5625f * t * t + 0.75f;
        } else if (t < 2.5 / 2.75f) {
            t -= 2.25f / 2.75f;
            return 7.5625f * t * t + 0.9375f;
        } else {
            t -= 2.625f / 2.75f;
            return 7.5625f * t * t + 0.984375f;
        }
    };

    // Back
    Easing BACK_IN = t -> {
        float s = 1.70158f;
        return t * t * ((s + 1) * t - s);
    };
    Easing BACK_OUT = t -> {
        float s = 1.70158f;
        t -= 1;
        return t * t * ((s + 1) * t + s) + 1;
    };

    /**
     * Reverses the easing (converts in to out).
     */
    default Easing reverse() {
        return t -> 1 - this.apply(1 - t);
    }

    /**
     * Combines two easings (first half uses this, second half uses other).
     */
    default Easing andThen(Easing other) {
        return t -> t < 0.5f
                    ? this.apply(t * 2) * 0.5f
                    : other.apply((t - 0.5f) * 2) * 0.5f + 0.5f;
    }
}
