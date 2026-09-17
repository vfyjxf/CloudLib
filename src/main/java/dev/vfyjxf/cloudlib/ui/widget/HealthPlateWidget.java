package dev.vfyjxf.cloudlib.ui.widget;

import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.canvas.SceneCanvas;
import dev.vfyjxf.taffy.geometry.FloatSize;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.DoubleSupplier;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/**
 * A Neat-style health plate: a dark board carrying an optional name line, a
 * 4px health bar whose colour follows the remaining fraction
 * ({@code hue = max(0, fraction/3 - 0.07)}, full saturation/value, alpha 127)
 * and up to three tiny numbers inside the bar (current left-aligned, maximum
 * right-aligned and bold, percentage centered — each switchable). An optional
 * armor row draws one chestplate icon per 5 armor points below the bar.
 * <p>
 * Values are pulled from the suppliers on every render — the bar snaps to the
 * new fraction with no interpolation or residue, exactly like Neat's bars.
 */
public final class HealthPlateWidget extends Widget {

    // region layout constants (logical px, relative to the 52px board)

    static final int boardWidth = 52;
    static final int barHeight = 4;
    static final int padX = 2;
    static final int padTopWithName = 6;
    static final int padTopNameless = 2;
    static final int padBottom = 6;
    static final int iconSize = 16;

    static final int boardColor = 0x3C000000;
    static final int emptyColor = 0x7F000000;
    static final int fillAlpha = 127;
    static final float numberScale = 0.375f;

    // endregion

    // region state

    private final DoubleSupplier health;
    private final DoubleSupplier maxHealth;
    private final Supplier<Component> name;
    private final BooleanSupplier customName;
    private final IntSupplier armor;

    private boolean showCurrentHealth = true;
    private boolean showMaxHealth = true;
    private boolean showPercentage = true;
    private boolean showArmor = false;

    // endregion

    // region factory

    /** Binds the plate to a live entity — name, health, maximum and armor read straight from it. */
    public static HealthPlateWidget of(LivingEntity entity) {
        return new HealthPlateWidget(
                entity::getHealth,
                entity::getMaxHealth,
                entity::getDisplayName,
                entity::hasCustomName,
                entity::getArmorValue);
    }

    public HealthPlateWidget(DoubleSupplier health, DoubleSupplier maxHealth, Supplier<Component> name) {
        this(health, maxHealth, name, () -> false, () -> 0);
    }

    public HealthPlateWidget(
            DoubleSupplier health,
            DoubleSupplier maxHealth,
            Supplier<Component> name,
            BooleanSupplier customName,
            IntSupplier armor) {
        this.health = health;
        this.maxHealth = maxHealth;
        this.name = name;
        this.customName = customName;
        this.armor = armor;
        onMount((scene, context, handle) -> scene.layoutTree().setMeasureFunc(nodeId(), (style, space) -> {
            boolean named = name.get() != null && !name.get().getString().isEmpty();
            int nameHeight = named ? context.font().lineHeight : 0;
            int boardW = panelWidth(named ? context.font().width(name.get()) : 0);
            int rows = showArmor ? armorRows(armor.getAsInt(), boardW) : 0;
            return new FloatSize(boardW, panelHeight(nameHeight, rows));
        }));
    }

    // endregion

    // region configuration

    public HealthPlateWidget setShowCurrentHealth(boolean showCurrentHealth) {
        this.showCurrentHealth = showCurrentHealth;
        return this;
    }

    public HealthPlateWidget setShowMaxHealth(boolean showMaxHealth) {
        this.showMaxHealth = showMaxHealth;
        return this;
    }

    public HealthPlateWidget setShowPercentage(boolean showPercentage) {
        this.showPercentage = showPercentage;
        return this;
    }

    public HealthPlateWidget setShowArmor(boolean showArmor) {
        this.showArmor = showArmor;
        return this;
    }

    public boolean showCurrentHealth() {
        return showCurrentHealth;
    }

    public boolean showMaxHealth() {
        return showMaxHealth;
    }

    public boolean showPercentage() {
        return showPercentage;
    }

    public boolean showArmor() {
        return showArmor;
    }

    /** Current health / maximum, clamped to [0, 1]. */
    public double fraction() {
        double max = maxHealth.getAsDouble();
        return max > 0 ? Math.clamp(health.getAsDouble() / max, 0f, 1f) : 0;
    }

    // endregion

    // region geometry & colour (pure — the headless tests drive these)

    /** The fill hue for a health fraction: green-leaning as it fills, red at the bottom. */
    public static double hue(double fraction) {
        return Math.max(0, fraction / 3 - 0.07);
    }

    /** The fill colour — HSV(hue, 1, 1) at alpha 127; {@code fraction} is clamped. */
    public static int fillColor(double fraction) {
        return (fillAlpha << 24) | hsvToRgb(hue(Math.clamp(fraction, 0f, 1f)));
    }

    /** HSV to RGB with full saturation and value — the segment Neat colours its bars with. */
    static int hsvToRgb(double hue) {
        double h = Math.clamp(hue, 0f, 1f) * 6;
        int segment = (int) Math.floor(h);
        double f = h - segment;
        return switch (Math.floorMod(segment, 6)) {
            case 0 -> rgb(1.0, f, 0.0);
            case 1 -> rgb(1.0 - f, 1.0, 0.0);
            case 2 -> rgb(0.0, 1.0, f);
            case 3 -> rgb(0.0, 1.0 - f, 1.0);
            case 4 -> rgb(f, 0.0, 1.0);
            default -> rgb(1.0, 0.0, 1.0 - f);
        };
    }

    private static int rgb(double r, double g, double b) {
        return ((int) Math.round(r * 255) << 16) | ((int) Math.round(g * 255) << 8) | (int) Math.round(b * 255);
    }

    /** Filled bar length — the width times the fraction, never negative nor overflowing. */
    public static int fillWidth(int barWidth, double fraction) {
        return (int) (barWidth * Math.clamp(fraction, 0f, 1f));
    }

    /** Board width: the fixed 52px unless a wider name needs room. */
    public static int panelWidth(int nameWidth) {
        return Math.max(boardWidth, nameWidth + 2 * padX);
    }

    /** Board height for a name line of {@code nameHeight} px (0 = nameless) and an armor row setup. */
    public static int panelHeight(int nameHeight, int armorRows) {
        int top = nameHeight > 0 ? padTopWithName : padTopNameless;
        return top + nameHeight + barHeight + armorRows * iconSize + padBottom;
    }

    /** One chestplate icon per 5 armor points. */
    public static int armorIcons(int armor) {
        return Math.max(0, armor) / 5;
    }

    /** How many icon rows the armor display wraps into at the given board width. */
    public static int armorRows(int armor, int width) {
        int icons = armorIcons(armor);
        if (icons == 0) return 0;
        int perRow = Math.max(1, (width - 2 * padX) / iconSize);
        return (icons + perRow - 1) / perRow;
    }

    /** The chestplate tier for an armor total — diamond from 20 (a full suit) up. */
    public static ItemStack armorIcon(int armor) {
        return armor >= 20 ? new ItemStack(Items.DIAMOND_CHESTPLATE) : new ItemStack(Items.IRON_CHESTPLATE);
    }

    /** {@code #.##} — up to two decimals, trailing zeros trimmed. */
    public static String formatNumber(double value) {
        String s = String.format(Locale.ROOT, "%.2f", value);
        if (s.contains(".")) {
            s = s.replaceAll("0+$", "").replaceAll("\\.$", "");
        }
        return "-0".equals(s) ? "0" : s;
    }

    // endregion

    // region rendering

    @Override
    protected void renderInternal(SceneCanvas canvas, int mouseX, int mouseY, float partialTicks) {
        int w = width();
        int h = height();
        canvas.fill(0, 0, w, h, boardColor);

        Component nameComponent = name.get();
        boolean named = nameComponent != null && !nameComponent.getString().isEmpty();
        int y;
        if (named) {
            if (customName.getAsBoolean()) {
                nameComponent = nameComponent.copy().withStyle(ChatFormatting.ITALIC);
            }
            canvas.text(nameComponent, padX, padTopWithName, 0xFFFFFFFF, true);
            y = padTopWithName + context().font().lineHeight;
        } else {
            y = padTopNameless;
        }

        // the bar: black remainder, coloured fill — the fraction snaps, no animation
        int barX = padX;
        int barW = w - 2 * padX;
        double fraction = fraction();
        canvas.fill(barX, y, barW, barHeight, emptyColor);
        int fill = fillWidth(barW, fraction);
        if (fill > 0) {
            canvas.fill(barX, y, fill, barHeight, fillColor(fraction));
        }

        drawNumbers(canvas, barX, y, barW, fraction);

        if (showArmor) {
            drawArmor(canvas, y + barHeight);
        }
    }

    /** The three tiny numbers inside the bar — left current, right maximum (bold), center percentage. */
    private void drawNumbers(SceneCanvas canvas, int barX, int barY, int barW, double fraction) {
        if (!showCurrentHealth && !showMaxHealth && !showPercentage) return;
        var font = context().font();
        int heightPx = (int) Math.ceil(font.lineHeight * numberScale);
        int textY = barY + Math.max(0, (barHeight - heightPx) / 2);

        canvas.pushTransform();
        canvas.translate(barX, textY);
        canvas.scale(numberScale);
        int scaledWidth = (int) (barW / numberScale);
        if (showCurrentHealth) {
            canvas.text(formatNumber(health.getAsDouble()), 0, 0, 0xFFFFFFFF);
        }
        if (showMaxHealth) {
            String max = formatNumber(maxHealth.getAsDouble());
            canvas.text(
                    Component.literal(max).withStyle(ChatFormatting.BOLD),
                    scaledWidth - font.width(max),
                    0,
                    0xFFFFFFFF);
        }
        if (showPercentage) {
            String percent = formatNumber(Math.round(fraction * 100));
            canvas.text(percent, (scaledWidth - font.width(percent)) / 2, 0, 0xFFFFFFFF);
        }
        canvas.popTransform();
    }

    /** The armor row — chestplate icons, one per 5 points, wrapping at the board width. */
    private void drawArmor(SceneCanvas canvas, int y) {
        int total = armor.getAsInt();
        int icons = armorIcons(total);
        if (icons == 0) return;
        ItemStack icon = armorIcon(total);
        int perRow = Math.max(1, (width() - 2 * padX) / iconSize);
        for (int i = 0; i < icons; i++) {
            canvas.renderItemIcon(icon, padX + (i % perRow) * iconSize, y + (i / perRow) * iconSize);
        }
    }

    // endregion
}
