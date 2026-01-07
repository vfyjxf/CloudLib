package dev.vfyjxf.cloudlib.api.ui.texture;

import java.util.ArrayList;
import java.util.List;

/**
 * Animation sequence that plays multiple animations in order.
 *
 * @param <T> the type of value this animation produces
 */
public class AnimationSequence<T> implements Playable<T> {

    private final List<Playable<T>> animations = new ArrayList<>();
    private int currentIndex = 0;
    private boolean playing = false;
    private boolean loop = false;

    public AnimationSequence() {}

    @SafeVarargs
    public AnimationSequence(Playable<T>... animations) {
        for (Playable<T> anim : animations) {
            this.animations.add(anim);
        }
    }

    public AnimationSequence<T> add(Playable<T> animation) {
        animations.add(animation);
        return this;
    }

    public int size() {
        return animations.size();
    }

    public int currentIndex() {
        return currentIndex;
    }

    public Playable<T> current() {
        if (animations.isEmpty()) return null;
        return animations.get(currentIndex);
    }

    @Override
    public float progress() {
        if (animations.isEmpty()) return 0;
        float completed = currentIndex;
        Playable<T> current = current();
        if (current != null) {
            completed += current.progress();
        }
        return completed / animations.size();
    }

    @Override
    public void setProgress(float progress) {
        if (animations.isEmpty()) return;
        progress = Math.max(0, Math.min(1, progress));
        
        float perAnim = 1.0f / animations.size();
        int targetIndex = (int) (progress / perAnim);
        targetIndex = Math.min(targetIndex, animations.size() - 1);
        

        for (int i = 0; i < targetIndex; i++) {
            animations.get(i).stop();
        }
        
        currentIndex = targetIndex;
        Playable<T> current = current();
        if (current != null) {
            float localProgress = (progress - targetIndex * perAnim) / perAnim;
            current.setProgress(localProgress);
        }
    }

    @Override
    public T value() {
        Playable<T> current = current();
        return current != null ? current.value() : null;
    }

    @Override
    public T value(float partialTick) {
        Playable<T> current = current();
        return current != null ? current.value(partialTick) : null;
    }

    @Override
    public void update(float deltaTime) {
        if (!playing || animations.isEmpty()) return;

        Playable<T> current = current();
        if (current == null) return;

        current.update(deltaTime);

        if (current.isComplete() && !current.isLooping()) {
            currentIndex++;
            if (currentIndex >= animations.size()) {
                if (loop) {
                    currentIndex = 0;
                    for (Playable<T> anim : animations) {
                        anim.stop();
                    }
                    animations.get(0).play();
                } else {
                    currentIndex = animations.size() - 1;
                    playing = false;
                }
            } else {
                animations.get(currentIndex).play();
            }
        }
    }

    @Override
    public void play() {
        playing = true;
        Playable<T> current = current();
        if (current != null) {
            current.play();
        }
    }

    @Override
    public void pause() {
        playing = false;
        Playable<T> current = current();
        if (current != null) {
            current.pause();
        }
    }

    @Override
    public void stop() {
        playing = false;
        currentIndex = 0;
        for (Playable<T> anim : animations) {
            anim.stop();
        }
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
}
