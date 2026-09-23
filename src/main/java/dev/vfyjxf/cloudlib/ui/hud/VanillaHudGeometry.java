package dev.vfyjxf.cloudlib.ui.hud;

import dev.vfyjxf.cloudlib.api.math.Rect;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure geometry of the vanilla HUD, in gui-scaled screen pixels. Every
 * formula below transcribes vanilla 1.21.1 render code — cited per method —
 * so the exclusion rectangles track what actually renders. All inputs arrive
 * via {@link HudInputs}; nothing here touches Minecraft, keeping the formulas
 * unit-testable and centralized for when a version bump moves them.
 */
public final class VanillaHudGeometry {

    /** Gui#renderItemHotbar: {@code blitSprite(HOTBAR_SPRITE, i - 91, height - 22, 182, 22)}. */
    public static final int hotbarWidth = 182;

    /** See {@link #hotbarWidth}. */
    public static final int hotbarHeight = 22;

    /** Hearts/armor icons are 9 px sprites at 8 px pitch, ten per row. */
    public static final int columnWidth = 81;

    /** The y gap between the chat panel bottom and the screen bottom. */
    public static final int chatBottomMargin = 40;

    /** ToastComponent: one toast slot is 32 px tall, at most five slots. */
    public static final int toastSlotHeight = 32;

    private VanillaHudGeometry() {}

    /** Computes every exclusion rectangle for the sampled frame, in a fixed order. */
    public static List<Rect> compute(HudInputs inputs) {
        List<Rect> rects = new ArrayList<>();
        int width = inputs.screenWidth();
        int height = inputs.screenHeight();

        hotbar(inputs, rects);
        if (inputs.experienceBar() || inputs.jumpMeter()) {
            rects.add(experienceBar(width, height));
        }
        if (inputs.experienceLevel()) {
            rects.add(experienceLevel(width, height));
        }
        if (inputs.healthColumns()) {
            rects.add(leftColumn(width, height, inputs.leftHeight()));
            rects.add(rightColumn(width, height, inputs.rightHeight()));
        }
        if (inputs.effects()) {
            effectIcons(inputs, rects);
        }
        for (HudInputs.BossBar bossBar : inputs.bossBars()) {
            rects.add(bossBar(width, bossBar));
        }
        if (inputs.chat()) {
            rects.add(chat(width, height, inputs.chatX(), inputs.chatWidth(), inputs.chatHeight()));
        }
        for (HudInputs.Toast toast : inputs.toasts()) {
            rects.add(toast(width, toast));
        }
        subtitles(inputs, rects);
        return List.copyOf(rects);
    }

    /**
     * Gui#renderItemHotbar: the hotbar at {@code (width/2 - 91, height - 22,
     * 182, 22)} — spectator mode draws the spectator hotbar in the same rect —
     * plus the offhand slot at {@code (width/2 - 120, height - 23, 29, 24)}
     * (left arm) or {@code (width/2 + 91, height - 23, 29, 24)} (right arm).
     */
    public static void hotbar(HudInputs inputs, List<Rect> out) {
        int width = inputs.screenWidth();
        int height = inputs.screenHeight();
        out.add(new Rect(width / 2 - 91, height - hotbarHeight, hotbarWidth, hotbarHeight));
        if (inputs.offhandSlot()) {
            int x = inputs.offhandLeft() ? width / 2 - 91 - 29 : width / 2 + 91;
            out.add(new Rect(x, height - 23, 29, 24));
        }
    }

    /**
     * Gui#renderExperienceBar and Gui#renderJumpMeter both draw
     * {@code (x, height - 32 + 3, 182, 5)} with {@code x = width/2 - 91}.
     */
    public static Rect experienceBar(int screenWidth, int screenHeight) {
        return new Rect(screenWidth / 2 - 91, screenHeight - 32 + 3, 182, 5);
    }

    /**
     * Gui#renderExperienceLevel: the level string draws at
     * {@code height - 31 - 4} with font line height 9, centered on the same
     * 182 px span.
     */
    public static Rect experienceLevel(int screenWidth, int screenHeight) {
        return new Rect(screenWidth / 2 - 91, screenHeight - 31 - 4, 182, 9);
    }

    /**
     * Gui#renderHealthLevel / Gui#renderArmorLevel: the hearts-and-armor stack
     * occupies {@code (width/2 - 91, height - leftHeight, 81, leftHeight)} —
     * {@code leftHeight} is the running height the render itself accumulates
     * and {@link VanillaHudSampler} samples after the last column layer.
     */
    public static Rect leftColumn(int screenWidth, int screenHeight, int leftHeight) {
        return new Rect(screenWidth / 2 - 91, screenHeight - leftHeight, columnWidth, leftHeight);
    }

    /**
     * Gui#renderFoodLevel / Gui#renderAirLevel / Gui#renderVehicleHealth: the
     * right-hand stack mirrors the left one, icons at {@code width/2 + 91}
     * growing left, so the column spans {@code (width/2 + 10, height -
     * rightHeight, 81, rightHeight)}.
     */
    public static Rect rightColumn(int screenWidth, int screenHeight, int rightHeight) {
        return new Rect(screenWidth / 2 + 91 - columnWidth, screenHeight - rightHeight, columnWidth, rightHeight);
    }

    /**
     * Gui#renderEffects: icons are 24×24 at {@code x = width - 25 * column},
     * beneficial effects in the row at {@code y = 1} and non-beneficial at
     * {@code y = 27}, both shifted down 15 px in the demo.
     */
    public static void effectIcons(HudInputs inputs, List<Rect> out) {
        int width = inputs.screenWidth();
        int y = inputs.demo() ? 16 : 1;
        for (int column = 1; column <= inputs.beneficialEffects(); column++) {
            out.add(new Rect(width - 25 * column, y, 24, 24));
        }
        for (int column = 1; column <= inputs.otherEffects(); column++) {
            out.add(new Rect(width - 25 * column, y + 26, 24, 24));
        }
    }

    /**
     * BossHealthOverlay#render: the name draws at {@code y - 9} and the bar is
     * 182×5 at {@code y}, rows stepped by the customization event's increment
     * (vanilla default 19 = 10 + font line height); the whole row is one
     * exclusion rectangle at {@code (width/2 - 91, y - 9, 182, increment)}.
     */
    public static Rect bossBar(int screenWidth, HudInputs.BossBar bar) {
        return new Rect(screenWidth / 2 - 91, bar.y() - 9, 182, bar.increment());
    }

    /**
     * Gui#renderChat / ChatComponent#render: the panel is {@code chatWidth ×
     * chatHeight} with its bottom {@link #chatBottomMargin} px above the
     * screen bottom and its left at the customization event's x (0 unless a
     * mod moved it).
     */
    public static Rect chat(int screenWidth, int screenHeight, int chatX, int chatWidth, int chatHeight) {
        return new Rect(chatX, screenHeight - chatBottomMargin - chatHeight, chatWidth, chatHeight);
    }

    /**
     * ToastComponent.ToastInstance#render: a toast slides in from the right
     * edge at {@code (width - toastWidth, index * 32)} and spans its slot run;
     * the exclusion rect ignores the slide-in animation (conservative).
     */
    public static Rect toast(int screenWidth, HudInputs.Toast toast) {
        return new Rect(
            screenWidth - toast.width(),
            toast.index() * toastSlotHeight,
            toast.width(),
            toast.slotCount() * toastSlotHeight
        );
    }

    /**
     * SubtitleOverlay#render: each row is centered on the anchor
     * {@code (width - halfWidth - 2, height - 35 - 10 * index)} with the
     * background fill spanning {@code halfWidth + 1} px either side and 10 px
     * tall.
     */
    public static Rect subtitle(int screenWidth, int screenHeight, int halfWidth, int index) {
        int anchorX = screenWidth - halfWidth - 2;
        int anchorY = screenHeight - 35 - 10 * index;
        return new Rect(anchorX - halfWidth - 1, anchorY - 5, 2 * halfWidth + 2, 10);
    }

    private static void subtitles(HudInputs inputs, List<Rect> out) {
        if (inputs.subtitleHalfWidth() <= 0) {
            return;
        }
        for (int index = 0; index < inputs.subtitleCount(); index++) {
            out.add(subtitle(inputs.screenWidth(), inputs.screenHeight(), inputs.subtitleHalfWidth(), index));
        }
    }
}
