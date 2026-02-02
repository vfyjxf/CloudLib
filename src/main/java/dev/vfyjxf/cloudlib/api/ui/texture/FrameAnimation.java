package dev.vfyjxf.cloudlib.api.ui.texture;

import java.util.ArrayList;
import java.util.List;

/**
 * Frame-based animation that produces textures over time.
 * <p>
 * Frame animations are discrete and do not interpolate between frames.
 * For smooth effects, use {@link #frameProgress(float)} to get the progress within the current frame.
 */
public class FrameAnimation implements Playable<VisualTexture> {

    private record Frame(VisualTexture texture, float duration) {}

    private final List<Frame> frames = new ArrayList<>();
    private int currentIndex = 0;
    private float elapsed = 0;
    private float prevElapsed = 0;
    private float totalElapsed = 0;
    private float prevTotalElapsed = 0;
    private float speed = 1.0f;
    private boolean playing = false;
    private boolean loop = true;

    public FrameAnimation() {}

    public FrameAnimation addFrame(VisualTexture texture, float duration) {
        frames.add(new Frame(texture, duration));
        return this;
    }

    public FrameAnimation addFrames(float duration, VisualTexture... textures) {
        for (VisualTexture t : textures) {
            addFrame(t, duration);
        }
        return this;
    }

    public FrameAnimation setSpeed(float speed) {
        this.speed = speed;
        return this;
    }

    public float speed() {
        return speed;
    }

    public int frameCount() {
        return frames.size();
    }

    public int currentFrameIndex() {
        return currentIndex;
    }

    /**
     * Returns the texture at the specified frame index.
     */
    public VisualTexture frameAt(int index) {
        if (index < 0 || index >= frames.size()) return VisualTexture.empty;
        return frames.get(index).texture;
    }

    /**
     * Returns the progress within the current frame (0.0 to 1.0).
     */
    public float frameProgress() {
        if (frames.isEmpty()) return 0;
        Frame frame = frames.get(currentIndex);
        return frame.duration > 0 ? Math.min(elapsed / frame.duration, 1.0f) : 0;
    }

    /**
     * Returns the progress within the current frame with interpolation.
     *
     * @param partialTick interpolation factor
     * @return interpolated frame progress
     */
    public float frameProgress(float partialTick) {
        if (frames.isEmpty()) return 0;
        Frame frame = frames.get(currentIndex);
        if (frame.duration <= 0) return 0;
        float interpolatedElapsed = prevElapsed + (elapsed - prevElapsed) * partialTick;
        return Math.min(interpolatedElapsed / frame.duration, 1.0f);
    }

    //region animation

    @Override
    public VisualTexture value() {
        if (frames.isEmpty()) return VisualTexture.empty;
        return frames.get(currentIndex).texture;
    }

    @Override
    public VisualTexture value(float partialTick) {
        // 帧动画是离散的，直接返回当前帧
        // partialTick 主要影响 frameProgress()，供外部使用
        return value();
    }

    @Override
    public float progress() {
        if (frames.isEmpty()) return 0;
        float total = totalDuration();
        return total > 0 ? Math.min(totalElapsed / total, 1.0f) : 0;
    }

    /**
     * 获取带帧间插值的进度。
     */
    public float progress(float partialTick) {
        if (frames.isEmpty()) return 0;
        float total = totalDuration();
        if (total <= 0) return 0;
        float interpolated = prevTotalElapsed + (totalElapsed - prevTotalElapsed) * partialTick;
        return Math.min(interpolated / total, 1.0f);
    }

    @Override
    public void setProgress(float progress) {
        if (frames.isEmpty()) return;

        progress = Math.max(0, Math.min(1, progress));
        float total = totalDuration();
        float targetTime = total * progress;

        prevTotalElapsed = totalElapsed;
        totalElapsed = targetTime;

        float accumulated = 0;
        for (int i = 0; i < frames.size(); i++) {
            float frameDuration = frames.get(i).duration;
            if (accumulated + frameDuration >= targetTime) {
                currentIndex = i;
                prevElapsed = elapsed;
                elapsed = targetTime - accumulated;
                return;
            }
            accumulated += frameDuration;
        }
        currentIndex = frames.size() - 1;
        elapsed = frames.get(currentIndex).duration;
    }

    public float totalDuration() {
        float total = 0;
        for (Frame f : frames) {
            total += f.duration;
        }
        return total;
    }

    @Override
    public void update(float deltaTime) {
        if (!playing || frames.isEmpty()) return;

        float dt = deltaTime * speed;
        prevElapsed = elapsed;
        prevTotalElapsed = totalElapsed;

        elapsed += dt;
        totalElapsed += dt;

        Frame frame = frames.get(currentIndex);

        while (elapsed >= frame.duration) {
            elapsed -= frame.duration;
            currentIndex++;

            if (currentIndex >= frames.size()) {
                if (loop) {
                    currentIndex = 0;
                    totalElapsed = elapsed;
                    prevTotalElapsed = 0;
                } else {
                    currentIndex = frames.size() - 1;
                    elapsed = frame.duration;
                    totalElapsed = totalDuration();
                    playing = false;
                    return;
                }
            }
            frame = frames.get(currentIndex);
            prevElapsed = 0;
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
        currentIndex = 0;
        elapsed = 0;
        prevElapsed = 0;
        totalElapsed = 0;
        prevTotalElapsed = 0;
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
