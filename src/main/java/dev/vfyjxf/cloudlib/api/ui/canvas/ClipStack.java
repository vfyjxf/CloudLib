package dev.vfyjxf.cloudlib.api.ui.canvas;

import dev.vfyjxf.cloudlib.api.math.Rect;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Manages a stack of scissor (clipping) regions with intersection support.
 */
public final class ClipStack {

    private final Deque<Rect> stack = new ArrayDeque<>();
    private @Nullable Rect currentClip = null;
    private boolean dirty = false;

    public void push(int x, int y, int width, int height) {
        push(new Rect(x, y, width, height));
    }

    /**
     * Pushes a new clip region. The effective clip is the intersection with current.
     */
    public void push(Rect region) {
        stack.push(region);
        if (currentClip == null) {
            currentClip = region;
        } else {
            currentClip = currentClip.intersection(region);
        }
        dirty = true;
    }

    public void pop() {
        if (stack.isEmpty()) {
            throw new IllegalStateException("Clip stack underflow");
        }
        stack.pop();
        recalculateClip();
        dirty = true;
    }

    public @Nullable Rect current() {
        return currentClip;
    }

    public boolean hasClip() {
        return currentClip != null;
    }

    /**
     * Returns true if changed since last check. Clears the dirty flag.
     */
    public boolean isDirty() {
        boolean wasDirty = dirty;
        dirty = false;
        return wasDirty;
    }

    public int depth() {
        return stack.size();
    }

    public boolean isClipped(int x, int y, int width, int height) {
        if (currentClip == null) return false;
        return !currentClip.intersects(x, y, width, height);
    }

    public boolean isClipped(Rect bounds) {
        return isClipped(bounds.x(), bounds.y(), bounds.width(), bounds.height());
    }

    public void clear() {
        stack.clear();
        currentClip = null;
        dirty = true;
    }

    private void recalculateClip() {
        currentClip = null;
        for (Rect rect : stack) {
            if (currentClip == null) {
                currentClip = rect;
            } else {
                currentClip = currentClip.intersection(rect);
            }
        }
    }
}
