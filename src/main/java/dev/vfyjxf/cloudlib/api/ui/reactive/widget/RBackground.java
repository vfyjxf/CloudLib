package dev.vfyjxf.cloudlib.api.ui.reactive.widget;

import dev.vfyjxf.cloudlib.api.ui.reactive.Theme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Font;

import java.util.function.Supplier;

/**
 * A background widget that fills the entire area with a color.
 * <p>
 * Supports reactive colors and theme integration.
 * <p>
 * Example usage:
 * <pre>{@code
 * // Static background
 * RBackground bg = new RBackground(0xFF2A2A3E);
 * 
 * // Theme-based background
 * Theme theme = Theme.reactive(darkMode::get);
 * RBackground bg = new RBackground(theme::background);
 * 
 * // Gradient background
 * RBackground bg = RBackground.gradient(0xFF1A1A2E, 0xFF2A2A3E, Direction.VERTICAL);
 * }</pre>
 */
public class RBackground extends RWidget {

    private Supplier<Integer> colorSupplier;
    private int cachedColor;
    
    // Gradient support
    private boolean isGradient = false;
    private Supplier<Integer> gradientEndSupplier;
    private int cachedGradientEnd;
    private Direction gradientDirection = Direction.VERTICAL;
    
    // Border support
    private int borderColor = 0;
    private int borderWidth = 0;
    
    // Corner radius
    private int cornerRadius = 0;

    public enum Direction {
        HORIZONTAL, VERTICAL, DIAGONAL_DOWN, DIAGONAL_UP
    }

    // ===== Constructors =====
    
    public RBackground(int color) {
        this(() -> color);
    }
    
    public RBackground(Supplier<Integer> colorSupplier) {
        this.colorSupplier = colorSupplier;
        this.cachedColor = colorSupplier.get();
    }
    
    /**
     * Creates a background from a theme.
     */
    public static RBackground fromTheme(Theme theme) {
        return new RBackground(theme::background);
    }
    
    /**
     * Creates a gradient background.
     */
    public static RBackground gradient(int startColor, int endColor, Direction direction) {
        return gradient(() -> startColor, () -> endColor, direction);
    }
    
    /**
     * Creates a reactive gradient background.
     */
    public static RBackground gradient(Supplier<Integer> startColor, Supplier<Integer> endColor, Direction direction) {
        RBackground bg = new RBackground(startColor);
        bg.isGradient = true;
        bg.gradientEndSupplier = endColor;
        bg.cachedGradientEnd = endColor.get();
        bg.gradientDirection = direction;
        return bg;
    }

    // ===== Configuration =====
    
    public RBackground setColor(int color) {
        this.colorSupplier = () -> color;
        this.cachedColor = color;
        return this;
    }
    
    public RBackground setColor(Supplier<Integer> colorSupplier) {
        this.colorSupplier = colorSupplier;
        return this;
    }
    
    public RBackground setBorder(int color, int width) {
        this.borderColor = color;
        this.borderWidth = width;
        return this;
    }
    
    public RBackground setCornerRadius(int radius) {
        this.cornerRadius = radius;
        return this;
    }

    // ===== Rendering =====
    
    @Override
    public void render(GuiGraphics graphics, Font font, int mouseX, int mouseY, float delta) {
        if (!visible) return;

        // Update cached colors
        cachedColor = colorSupplier.get();
        if (isGradient && gradientEndSupplier != null) {
            cachedGradientEnd = gradientEndSupplier.get();
        }

        if (isGradient) {
            renderGradient(graphics);
        } else {
            if (cornerRadius > 0) {
                renderRounded(graphics);
            } else {
                graphics.fill(x, y, x + width, y + height, cachedColor);
            }
        }

        // Draw border
        if (borderWidth > 0 && borderColor != 0) {
            drawBorder(graphics);
        }
    }
    
    private void renderGradient(GuiGraphics graphics) {
        int steps = gradientDirection == Direction.HORIZONTAL ? width : height;
        
        for (int i = 0; i < steps; i++) {
            float t = (float) i / steps;
            int color = Theme.lerp(cachedColor, cachedGradientEnd, t);
            
            switch (gradientDirection) {
                case HORIZONTAL -> graphics.fill(x + i, y, x + i + 1, y + height, color);
                case VERTICAL -> graphics.fill(x, y + i, x + width, y + i + 1, color);
                case DIAGONAL_DOWN -> {
                    // Simplified diagonal
                    int startX = x + (int)(i * ((float)width / steps));
                    int startY = y + (int)(i * ((float)height / steps));
                    graphics.fill(startX, y, startX + 1, y + height, color);
                }
                case DIAGONAL_UP -> {
                    int startX = x + (int)(i * ((float)width / steps));
                    graphics.fill(startX, y, startX + 1, y + height, color);
                }
            }
        }
    }
    
    private void renderRounded(GuiGraphics graphics) {
        // Simplified rounded corners - draw main rect and corner pixels
        int r = Math.min(cornerRadius, Math.min(width / 2, height / 2));
        
        // Main body (excluding corners)
        graphics.fill(x + r, y, x + width - r, y + height, cachedColor);
        graphics.fill(x, y + r, x + r, y + height - r, cachedColor);
        graphics.fill(x + width - r, y + r, x + width, y + height - r, cachedColor);
        
        // Simple corner approximation
        for (int cx = 0; cx < r; cx++) {
            for (int cy = 0; cy < r; cy++) {
                double dist = Math.sqrt(cx * cx + cy * cy);
                if (dist <= r) {
                    // Top-left
                    graphics.fill(x + r - cx - 1, y + r - cy - 1, x + r - cx, y + r - cy, cachedColor);
                    // Top-right
                    graphics.fill(x + width - r + cx, y + r - cy - 1, x + width - r + cx + 1, y + r - cy, cachedColor);
                    // Bottom-left
                    graphics.fill(x + r - cx - 1, y + height - r + cy, x + r - cx, y + height - r + cy + 1, cachedColor);
                    // Bottom-right
                    graphics.fill(x + width - r + cx, y + height - r + cy, x + width - r + cx + 1, y + height - r + cy + 1, cachedColor);
                }
            }
        }
    }
    
    private void drawBorder(GuiGraphics graphics) {
        int bw = borderWidth;
        // Top
        graphics.fill(x, y, x + width, y + bw, borderColor);
        // Bottom
        graphics.fill(x, y + height - bw, x + width, y + height, borderColor);
        // Left
        graphics.fill(x, y, x + bw, y + height, borderColor);
        // Right
        graphics.fill(x + width - bw, y, x + width, y + height, borderColor);
    }

    @Override
    public void update() {
        super.update();
        // Update colors
        cachedColor = colorSupplier.get();
        if (isGradient && gradientEndSupplier != null) {
            cachedGradientEnd = gradientEndSupplier.get();
        }
    }

    @Override
    public int[] measure(Font font) {
        return new int[]{width, height};
    }
}
