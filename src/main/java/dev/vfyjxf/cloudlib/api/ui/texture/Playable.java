package dev.vfyjxf.cloudlib.api.ui.texture;

/**
 * A playable animation driven by time.
 *
 * @param <T> the type of value this animation produces
 */
public interface Playable<T> extends Animation<T> {

    /**
     * Updates the animation state.
     *
     * @param deltaTime time elapsed since last frame (in seconds)
     */
    void update(float deltaTime);

    void play();

    void pause();

    void stop();

    boolean isPlaying();

    boolean isLooping();

    void setLooping(boolean loop);
}
