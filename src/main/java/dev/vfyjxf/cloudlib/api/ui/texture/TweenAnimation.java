package dev.vfyjxf.cloudlib.api.ui.texture;

/**
 * Tween animation that interpolates between two values.
 * <p>
 * Supports easing functions and frame interpolation.
 *
 * @param <T> the type of value this animation produces
 */
public class TweenAnimation<T> implements Playable<T> {

    private final T startValue;
    private final T endValue;
    private final float duration;
    private final Interpolator<T> interpolator;
    private final Easing easing;

    private float elapsed = 0;
    private float prevElapsed = 0;
    private boolean playing = false;
    private boolean loop = false;

    public TweenAnimation(T startValue, T endValue, float duration, Interpolator<T> interpolator) {
        this(startValue, endValue, duration, interpolator, Easing.LINEAR);
    }

    public TweenAnimation(T startValue, T endValue, float duration, Interpolator<T> interpolator, Easing easing) {
        this.startValue = startValue;
        this.endValue = endValue;
        this.duration = duration;
        this.interpolator = interpolator;
        this.easing = easing;
    }

    public static TweenAnimation<Float> ofFloat(float start, float end, float duration) {
        return new TweenAnimation<>(start, end, duration, Interpolator.FLOAT);
    }

    public static TweenAnimation<Float> ofFloat(float start, float end, float duration, Easing easing) {
        return new TweenAnimation<>(start, end, duration, Interpolator.FLOAT, easing);
    }

    public static TweenAnimation<Integer> ofInt(int start, int end, float duration) {
        return new TweenAnimation<>(start, end, duration, Interpolator.INT);
    }

    public static TweenAnimation<Integer> ofColor(int startColor, int endColor, float duration) {
        return new TweenAnimation<>(startColor, endColor, duration, Interpolator.COLOR);
    }

    public static TweenAnimation<Integer> ofColor(int startColor, int endColor, float duration, Easing easing) {
        return new TweenAnimation<>(startColor, endColor, duration, Interpolator.COLOR, easing);
    }

    @Override
    public float progress() {
        return duration > 0 ? Math.min(elapsed / duration, 1.0f) : 1.0f;
    }

    @Override
    public void setProgress(float progress) {
        progress = Math.max(0, Math.min(1, progress));
        prevElapsed = elapsed;
        elapsed = progress * duration;
    }

    @Override
    public T value() {
        float t = easing.apply(progress());
        return interpolator.interpolate(startValue, endValue, t);
    }

    @Override
    public T value(float partialTick) {
        float interpolatedElapsed = prevElapsed + (elapsed - prevElapsed) * partialTick;
        float rawProgress = duration > 0 ? Math.min(interpolatedElapsed / duration, 1.0f) : 1.0f;
        float t = easing.apply(rawProgress);
        return interpolator.interpolate(startValue, endValue, t);
    }

    @Override
    public Easing easing() {
        return easing;
    }

    @Override
    public void update(float deltaTime) {
        if (!playing) return;

        prevElapsed = elapsed;
        elapsed += deltaTime;

        if (elapsed >= duration) {
            if (loop) {
                elapsed = elapsed % duration;
                prevElapsed = 0;
            } else {
                elapsed = duration;
                playing = false;
            }
        }
    }

    @Override
    public void play() {
        playing = true;
    }

    @Override
    public void pause() {
        playing = false;
    }

    @Override
    public void stop() {
        playing = false;
        prevElapsed = 0;
        elapsed = 0;
    }

    @Override
    public boolean isPlaying() {
        return playing;
    }

    @Override
    public boolean isLooping() {
        return loop;
    }

    @Override
    public void setLooping(boolean loop) {
        this.loop = loop;
    }

    public T startValue() {
        return startValue;
    }

    public T endValue() {
        return endValue;
    }

    public float duration() {
        return duration;
    }

    /**
     * Creates a reversed animation.
     */
    public TweenAnimation<T> reversed() {
        return new TweenAnimation<>(endValue, startValue, duration, interpolator, easing);
    }
}
