package dev.vfyjxf.cloudlib.ui.debug;

import dev.vfyjxf.cloudlib.api.math.Insets;

/**
 * Computes the DevTools panel bounds and the remaining canvas insets for each
 * dock mode and for the floating window.
 */
final class DockLayout {

    private DockMode mode = DockMode.floating;
    private boolean minimized;

    private int screenW;
    private int screenH;

    // floating window
    private float winX;
    private float winY;
    private float winW = DebugTheme.defaultWidth;
    private float winH;

    // docked split size
    private float dockSize = DebugTheme.defaultWidth;

    private boolean resizing;
    private int resizeStart;
    private float resizeStartSize;

    DockLayout(int screenW, int screenH) {
        this.screenW = screenW;
        this.screenH = screenH;
        initWindowBounds();
    }

    // region state

    DockMode mode() {
        return mode;
    }

    void setMode(DockMode mode) {
        this.mode = mode;
        if (mode == DockMode.floating) {
            initWindowBounds();
        } else {
            clampDockSize();
        }
    }

    boolean minimized() {
        return mode == DockMode.floating && minimized;
    }

    void setMinimized(boolean minimized) {
        this.minimized = minimized;
    }

    void setScreenSize(int w, int h) {
        this.screenW = w;
        this.screenH = h;
        clampWindow();
        clampDockSize();
    }

    void setWindowBounds(float x, float y, float w, float h) {
        this.winX = x;
        this.winY = y;
        this.winW = w;
        this.winH = h;
        clampWindow();
    }

    // endregion

    // region panel bounds

    float x() {
        if (mode == DockMode.floating) return winX;
        return switch (mode) {
            case dockRight -> screenW - panelW();
            case dockLeft, dockTop, dockBottom -> 0;
            default -> 0;
        };
    }

    float y() {
        if (mode == DockMode.floating) return winY;
        return switch (mode) {
            case dockBottom -> screenH - panelH();
            case dockTop, dockLeft, dockRight -> 0;
            default -> 0;
        };
    }

    float width() {
        if (mode == DockMode.floating) return winW;
        return switch (mode) {
            case dockLeft, dockRight -> panelW();
            case dockTop, dockBottom -> screenW;
            default -> screenW;
        };
    }

    float height() {
        if (mode == DockMode.floating) return winH;
        return switch (mode) {
            case dockTop, dockBottom -> panelH();
            case dockLeft, dockRight -> screenH;
            default -> screenH;
        };
    }

    // endregion

    // region canvas insets (gui-scaled space)

    Insets canvasInsets() {
        if (mode == DockMode.floating) {
            return Insets.zero;
        }
        int w = (int) panelW();
        int h = (int) panelH();
        return switch (mode) {
            case dockRight -> new Insets(0, w, 0, 0);
            case dockLeft -> new Insets(0, 0, 0, w);
            case dockTop -> new Insets(h, 0, 0, 0);
            case dockBottom -> new Insets(0, 0, h, 0);
            default -> Insets.zero;
        };
    }

    // endregion

    // region resize

    boolean isResizing() {
        return resizing;
    }

    void startResize(double mouseX, double mouseY) {
        resizing = true;
        resizeStart = mode.isHorizontal() ? (int) mouseX : (int) mouseY;
        resizeStartSize = dockSize;
    }

    void updateResize(double mouseX, double mouseY) {
        if (!resizing) return;
        int current = mode.isHorizontal() ? (int) mouseX : (int) mouseY;
        int delta = current - resizeStart;
        float newSize = resizeStartSize;
        switch (mode) {
            case dockRight -> newSize -= delta;
            case dockLeft -> newSize += delta;
            case dockBottom -> newSize -= delta;
            case dockTop -> newSize += delta;
        }
        dockSize = newSize;
        clampDockSize();
    }

    void endResize() {
        resizing = false;
    }

    boolean hitResizeHandle(double mouseX, double mouseY) {
        if (mode == DockMode.floating) return false;
        float x = x();
        float y = y();
        float w = width();
        float h = height();
        return switch (mode) {
            case dockRight -> mouseX >= x && mouseX <= x + 4 && mouseY >= y && mouseY <= y + h;
            case dockLeft -> mouseX >= x + w - 4 && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
            case dockBottom -> mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + 4;
            case dockTop -> mouseX >= x && mouseX <= x + w && mouseY >= y + h - 4 && mouseY <= y + h;
            default -> false;
        };
    }

    // endregion

    // region internal

    private float panelW() {
        return mode.isHorizontal() ? dockSize : screenW;
    }

    private float panelH() {
        return mode.isVertical() ? dockSize : screenH;
    }

    private void initWindowBounds() {
        winW = Math.min(DebugTheme.defaultWidth, Math.max(DebugTheme.minWidth, screenW - 24));
        winH = Math.max(DebugTheme.minHeight, screenH * 4 / 5f);
        winX = Math.max(0, screenW - winW - 12);
        winY = 12;
        dockSize = winW;
    }

    private void clampWindow() {
        winX = Math.max(0, Math.min(winX, screenW - DebugTheme.minWidth));
        winY = Math.max(0, Math.min(winY, screenH - DebugTheme.minHeight));
        winW = Math.max(DebugTheme.minWidth, Math.min(winW, screenW - winX));
        winH = Math.max(DebugTheme.minHeight, Math.min(winH, screenH - winY));
    }

    private void clampDockSize() {
        float maxH = screenH - DebugTheme.titleBarHeight;
        float maxW = screenW - DebugTheme.dockMinSize;
        float max = mode.isHorizontal() ? maxW : maxH;
        float min = DebugTheme.dockMinSize;
        dockSize = Math.max(min, Math.min(dockSize, max));
    }

    // endregion

}
