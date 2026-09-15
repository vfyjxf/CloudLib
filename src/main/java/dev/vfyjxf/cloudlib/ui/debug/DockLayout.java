package dev.vfyjxf.cloudlib.ui.debug;

import dev.vfyjxf.cloudlib.api.math.Insets;

/**
 * Computes the DevTools panel bounds and the remaining canvas insets for each
 * dock mode and for the floating window.
 */
final class DockLayout {

    private DockMode mode = DockMode.FLOAT;
    private boolean minimized;

    private int screenW;
    private int screenH;

    // floating window
    private float winX;
    private float winY;
    private float winW = DebugTheme.DEFAULT_WIDTH;
    private float winH;

    // docked split size
    private float dockSize = DebugTheme.DEFAULT_WIDTH;

    private boolean resizing;
    private int resizeStart;
    private float resizeStartSize;

    DockLayout(int screenW, int screenH) {
        this.screenW = screenW;
        this.screenH = screenH;
        initWindowBounds();
    }

    //region state

    DockMode mode() {
        return mode;
    }

    void setMode(DockMode mode) {
        this.mode = mode;
        if (mode == DockMode.FLOAT) {
            initWindowBounds();
        } else {
            clampDockSize();
        }
    }

    boolean minimized() {
        return mode == DockMode.FLOAT && minimized;
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

    //endregion

    //region panel bounds

    float x() {
        if (mode == DockMode.FLOAT) return winX;
        return switch (mode) {
            case DOCK_RIGHT -> screenW - panelW();
            case DOCK_LEFT, DOCK_TOP, DOCK_BOTTOM -> 0;
            default -> 0;
        };
    }

    float y() {
        if (mode == DockMode.FLOAT) return winY;
        return switch (mode) {
            case DOCK_BOTTOM -> screenH - panelH();
            case DOCK_TOP, DOCK_LEFT, DOCK_RIGHT -> 0;
            default -> 0;
        };
    }

    float width() {
        if (mode == DockMode.FLOAT) return winW;
        return switch (mode) {
            case DOCK_LEFT, DOCK_RIGHT -> panelW();
            case DOCK_TOP, DOCK_BOTTOM -> screenW;
            default -> screenW;
        };
    }

    float height() {
        if (mode == DockMode.FLOAT) return winH;
        return switch (mode) {
            case DOCK_TOP, DOCK_BOTTOM -> panelH();
            case DOCK_LEFT, DOCK_RIGHT -> screenH;
            default -> screenH;
        };
    }

    //endregion

    //region canvas insets (gui-scaled space)

    Insets canvasInsets() {
        if (mode == DockMode.FLOAT) {
            return Insets.zero;
        }
        int w = (int) panelW();
        int h = (int) panelH();
        return switch (mode) {
            case DOCK_RIGHT -> new Insets(0, w, 0, 0);
            case DOCK_LEFT -> new Insets(0, 0, 0, w);
            case DOCK_TOP -> new Insets(h, 0, 0, 0);
            case DOCK_BOTTOM -> new Insets(0, 0, h, 0);
            default -> Insets.zero;
        };
    }

    //endregion

    //region resize

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
            case DOCK_RIGHT -> newSize -= delta;
            case DOCK_LEFT -> newSize += delta;
            case DOCK_BOTTOM -> newSize -= delta;
            case DOCK_TOP -> newSize += delta;
        }
        dockSize = newSize;
        clampDockSize();
    }

    void endResize() {
        resizing = false;
    }

    boolean hitResizeHandle(double mouseX, double mouseY) {
        if (mode == DockMode.FLOAT) return false;
        float x = x();
        float y = y();
        float w = width();
        float h = height();
        return switch (mode) {
            case DOCK_RIGHT -> mouseX >= x && mouseX <= x + 4 && mouseY >= y && mouseY <= y + h;
            case DOCK_LEFT -> mouseX >= x + w - 4 && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
            case DOCK_BOTTOM -> mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + 4;
            case DOCK_TOP -> mouseX >= x && mouseX <= x + w && mouseY >= y + h - 4 && mouseY <= y + h;
            default -> false;
        };
    }

    //endregion

    //region internal

    private float panelW() {
        return mode.isHorizontal() ? dockSize : screenW;
    }

    private float panelH() {
        return mode.isVertical() ? dockSize : screenH;
    }

    private void initWindowBounds() {
        winW = Math.min(DebugTheme.DEFAULT_WIDTH, Math.max(DebugTheme.MIN_WIDTH, screenW - 24));
        winH = Math.max(DebugTheme.MIN_HEIGHT, screenH * 4 / 5f);
        winX = Math.max(0, screenW - winW - 12);
        winY = 12;
        dockSize = winW;
    }

    private void clampWindow() {
        winX = Math.max(0, Math.min(winX, screenW - DebugTheme.MIN_WIDTH));
        winY = Math.max(0, Math.min(winY, screenH - DebugTheme.MIN_HEIGHT));
        winW = Math.max(DebugTheme.MIN_WIDTH, Math.min(winW, screenW - winX));
        winH = Math.max(DebugTheme.MIN_HEIGHT, Math.min(winH, screenH - winY));
    }

    private void clampDockSize() {
        float maxH = screenH - DebugTheme.TITLE_BAR_HEIGHT;
        float maxW = screenW - DebugTheme.DOCK_MIN_SIZE;
        float max = mode.isHorizontal() ? maxW : maxH;
        float min = DebugTheme.DOCK_MIN_SIZE;
        dockSize = Math.max(min, Math.min(dockSize, max));
    }

    //endregion

}
