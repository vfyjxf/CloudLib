package dev.vfyjxf.cloudlib.api.ui.reactive;

import java.util.function.Supplier;

/**
 * Theme system for reactive UI.
 * <p>
 * Provides centralized color and style management with support for
 * dark/light mode switching and custom themes.
 * <p>
 * Example usage:
 * <pre>{@code
 * // Create a theme
 * Theme theme = Theme.dark();
 * 
 * // Use in DSL
 * Column(() -> {
 *     Text("Hello", theme.text());
 *     Button("Click", onClick, theme.primaryButton());
 * });
 * 
 * // Switch modes
 * theme.setDarkMode(false);
 * 
 * // Create reactive theme
 * Signal<Boolean> darkMode = Signal.of(true);
 * Theme theme = Theme.reactive(darkMode::get);
 * }</pre>
 */
public class Theme {

    // ===== Color Palette =====
    
    /** Primary colors */
    private int primary = 0xFF4488FF;
    private int primaryHover = 0xFF5599FF;
    private int primaryPressed = 0xFF3377EE;
    
    /** Secondary colors */
    private int secondary = 0xFF666666;
    private int secondaryHover = 0xFF777777;
    private int secondaryPressed = 0xFF555555;
    
    /** Success/Error/Warning colors */
    private int success = 0xFF44AA44;
    private int error = 0xFFFF4444;
    private int warning = 0xFFFFAA00;
    
    /** Background colors */
    private int background = 0xFF2A2A3E;
    private int backgroundLight = 0xFF1A1A2E;
    private int surface = 0xFF3A3A4E;
    private int surfaceHover = 0xFF4A4A5E;
    
    /** Text colors */
    private int textPrimary = 0xFFFFFFFF;
    private int textSecondary = 0xFFAAAAAA;
    private int textMuted = 0xFF888888;
    private int textDisabled = 0xFF666666;
    
    /** Border colors */
    private int border = 0xFF555555;
    private int borderLight = 0xFF666666;
    
    /** Dark mode flag */
    private boolean darkMode = true;
    
    /** Reactive dark mode supplier */
    private Supplier<Boolean> darkModeSupplier;

    // ===== Constructors =====
    
    private Theme() {}
    
    /**
     * Creates a dark theme.
     */
    public static Theme dark() {
        return new Theme().applyDarkPalette();
    }
    
    /**
     * Creates a light theme.
     */
    public static Theme light() {
        return new Theme().applyLightPalette();
    }
    
    /**
     * Creates a reactive theme that responds to dark mode changes.
     * 
     * @param darkModeSupplier supplier for dark mode state
     */
    public static Theme reactive(Supplier<Boolean> darkModeSupplier) {
        Theme theme = new Theme();
        theme.darkModeSupplier = darkModeSupplier;
        return theme;
    }

    // ===== Dark Mode Management =====
    
    /**
     * Checks if currently in dark mode.
     */
    public boolean isDarkMode() {
        if (darkModeSupplier != null) {
            return darkModeSupplier.get();
        }
        return darkMode;
    }
    
    /**
     * Sets dark mode state.
     */
    public Theme setDarkMode(boolean dark) {
        this.darkMode = dark;
        if (dark) {
            applyDarkPalette();
        } else {
            applyLightPalette();
        }
        return this;
    }
    
    private Theme applyDarkPalette() {
        this.background = 0xFF2A2A3E;
        this.backgroundLight = 0xFF1A1A2E;
        this.surface = 0xFF3A3A4E;
        this.surfaceHover = 0xFF4A4A5E;
        this.textPrimary = 0xFFFFFFFF;
        this.textSecondary = 0xFFAAAAAA;
        this.textMuted = 0xFF888888;
        this.textDisabled = 0xFF666666;
        this.border = 0xFF555555;
        this.borderLight = 0xFF666666;
        this.darkMode = true;
        return this;
    }
    
    private Theme applyLightPalette() {
        this.background = 0xFFF5F5F5;
        this.backgroundLight = 0xFFFFFFFF;
        this.surface = 0xFFE8E8E8;
        this.surfaceHover = 0xFFDDDDDD;
        this.textPrimary = 0xFF1A1A1A;
        this.textSecondary = 0xFF555555;
        this.textMuted = 0xFF888888;
        this.textDisabled = 0xFFAAAAAA;
        this.border = 0xFFCCCCCC;
        this.borderLight = 0xFFDDDDDD;
        this.darkMode = false;
        return this;
    }

    // ===== Color Accessors =====
    
    /** Gets the effective background color based on current mode. */
    public int background() {
        return isDarkMode() ? 0xFF2A2A3E : 0xFFF5F5F5;
    }
    
    /** Gets the darker/lighter background variant. */
    public int backgroundAlt() {
        return isDarkMode() ? 0xFF1A1A2E : 0xFFFFFFFF;
    }
    
    /** Gets the surface color (cards, panels). */
    public int surfaceColor() {
        return isDarkMode() ? 0xFF3A3A4E : 0xFFE8E8E8;
    }
    
    /** Gets the surface hover color. */
    public int surfaceHover() {
        return isDarkMode() ? 0xFF4A4A5E : 0xFFDDDDDD;
    }
    
    /** Gets the primary text color. */
    public int textPrimaryColor() {
        return isDarkMode() ? 0xFFFFFFFF : 0xFF1A1A1A;
    }
    
    /** Gets the secondary text color. */
    public int textSecondaryColor() {
        return isDarkMode() ? 0xFFAAAAAA : 0xFF555555;
    }
    
    /** Gets the muted text color. */
    public int textMutedColor() {
        return isDarkMode() ? 0xFF888888 : 0xFF888888;
    }
    
    /** Gets the primary color. */
    public int primary() {
        return primary;
    }
    
    /** Gets the secondary color. */
    public int secondary() {
        return secondary;
    }
    
    /** Gets the success color. */
    public int success() {
        return success;
    }
    
    /** Gets the error color. */
    public int error() {
        return error;
    }
    
    /** Gets the warning color. */
    public int warning() {
        return warning;
    }
    
    /** Gets the border color. */
    public int border() {
        return isDarkMode() ? 0xFF555555 : 0xFFCCCCCC;
    }

    // ===== Style Factories =====
    
    /**
     * Creates a text style with primary text color.
     */
    public Style text() {
        return Style.of(Style.color(textPrimaryColor()));
    }
    
    /**
     * Creates a text style with secondary text color.
     */
    public Style textSecondary() {
        return Style.of(Style.color(textSecondaryColor()));
    }
    
    /**
     * Creates a text style with muted text color.
     */
    public Style textMuted() {
        return Style.of(Style.color(textMutedColor()));
    }
    
    /**
     * Creates a title text style.
     */
    public Style title() {
        return Style.of(
            Style.fontSize(16),
            Style.bold(),
            Style.color(textPrimaryColor())
        );
    }
    
    /**
     * Creates a subtitle text style.
     */
    public Style subtitle() {
        return Style.of(
            Style.fontSize(14),
            Style.bold(),
            Style.color(textSecondaryColor())
        );
    }
    
    /**
     * Creates a primary button style.
     */
    public Style primaryButton() {
        return Style.of(
            Style.padding(6, 12),
            Style.background(primary),
            Style.color(0xFFFFFFFF),
            Style.rounded(4)
        );
    }
    
    /**
     * Creates a secondary button style.
     */
    public Style secondaryButton() {
        return Style.of(
            Style.padding(6, 12),
            Style.background(secondary()),
            Style.color(0xFFFFFFFF),
            Style.rounded(4)
        );
    }
    
    /**
     * Creates a small button style.
     */
    public Style smallButton() {
        return Style.of(
            Style.padding(4, 8),
            Style.background(secondary()),
            Style.color(0xFFFFFFFF),
            Style.rounded(2)
        );
    }
    
    /**
     * Creates a success button style.
     */
    public Style successButton() {
        return Style.of(
            Style.padding(6, 12),
            Style.background(success),
            Style.color(0xFFFFFFFF),
            Style.rounded(4)
        );
    }
    
    /**
     * Creates an error/danger button style.
     */
    public Style dangerButton() {
        return Style.of(
            Style.padding(6, 12),
            Style.background(error),
            Style.color(0xFFFFFFFF),
            Style.rounded(4)
        );
    }
    
    /**
     * Creates a card/panel style.
     */
    public Style card() {
        return Style.of(
            Style.padding(12),
            Style.background(surfaceColor()),
            Style.border(1, border()),
            Style.rounded(4)
        );
    }
    
    /**
     * Creates a surface style.
     */
    public Style surface() {
        return Style.of(
            Style.background(surfaceColor())
        );
    }
    
    /**
     * Creates a tab button style.
     * 
     * @param active whether the tab is active
     */
    public Style tabButton(boolean active) {
        return Style.of(
            Style.padding(6, 12),
            Style.margin(0, 4),
            Style.background(active ? primary : secondary()),
            Style.color(0xFFFFFFFF)
        );
    }
    
    /**
     * Creates a toggle button style.
     * 
     * @param on whether the toggle is on
     */
    public Style toggleButton(boolean on) {
        return Style.of(
            Style.padding(4, 12),
            Style.background(on ? success : secondary()),
            Style.color(0xFFFFFFFF)
        );
    }
    
    /**
     * Creates a highlight style for selected items.
     */
    public Style highlight() {
        return Style.of(
            Style.background(primary),
            Style.color(0xFFFFFFFF)
        );
    }
    
    /**
     * Creates a hover highlight style.
     */
    public Style hoverHighlight() {
        return Style.of(
            Style.background(surfaceHover())
        );
    }

    // ===== Color Customization =====
    
    public Theme setPrimary(int color) {
        this.primary = color;
        this.primaryHover = brighten(color, 0.15f);
        this.primaryPressed = darken(color, 0.15f);
        return this;
    }
    
    public Theme setSecondary(int color) {
        this.secondary = color;
        this.secondaryHover = brighten(color, 0.15f);
        this.secondaryPressed = darken(color, 0.15f);
        return this;
    }
    
    public Theme setSuccess(int color) {
        this.success = color;
        return this;
    }
    
    public Theme setError(int color) {
        this.error = color;
        return this;
    }
    
    public Theme setWarning(int color) {
        this.warning = color;
        return this;
    }

    // ===== Color Utilities =====
    
    private static int brighten(int color, float factor) {
        int a = (color >> 24) & 0xFF;
        int r = Math.min(255, (int)(((color >> 16) & 0xFF) * (1 + factor)));
        int g = Math.min(255, (int)(((color >> 8) & 0xFF) * (1 + factor)));
        int b = Math.min(255, (int)((color & 0xFF) * (1 + factor)));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
    
    private static int darken(int color, float factor) {
        int a = (color >> 24) & 0xFF;
        int r = (int)(((color >> 16) & 0xFF) * (1 - factor));
        int g = (int)(((color >> 8) & 0xFF) * (1 - factor));
        int b = (int)((color & 0xFF) * (1 - factor));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
    
    /**
     * Interpolates between two colors.
     * 
     * @param from the start color
     * @param to the end color
     * @param t interpolation factor (0.0 - 1.0)
     */
    public static int lerp(int from, int to, float t) {
        int aFrom = (from >> 24) & 0xFF;
        int rFrom = (from >> 16) & 0xFF;
        int gFrom = (from >> 8) & 0xFF;
        int bFrom = from & 0xFF;
        
        int aTo = (to >> 24) & 0xFF;
        int rTo = (to >> 16) & 0xFF;
        int gTo = (to >> 8) & 0xFF;
        int bTo = to & 0xFF;
        
        int a = (int)(aFrom + (aTo - aFrom) * t);
        int r = (int)(rFrom + (rTo - rFrom) * t);
        int g = (int)(gFrom + (gTo - gFrom) * t);
        int b = (int)(bFrom + (bTo - bFrom) * t);
        
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
