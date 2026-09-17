package dev.vfyjxf.cloudlib.ui.hud;

import java.util.List;

/**
 * The dynamic measurements {@link VanillaHudGeometry} turns into exclusion
 * rectangles — everything the vanilla HUD's own render code would read off
 * {@code Gui}/{@code Minecraft}, captured by {@link VanillaHudSampler} and
 * kept free of Minecraft types so the geometry stays headless-testable.
 *
 * @param screenWidth         gui-scaled screen width
 * @param screenHeight        gui-scaled screen height
 * @param leftHeight          the sampled {@code Gui.leftHeight} (hearts + armor stack)
 * @param rightHeight         the sampled {@code Gui.rightHeight} (food + air + vehicle hearts stack)
 * @param healthColumns       whether the health/food/air columns render ({@code gameMode.canHurtPlayer()})
 * @param experienceBar       whether the experience bar renders
 * @param jumpMeter           whether the jump meter renders instead (riding a jumpable mount)
 * @param experienceLevel     whether the experience level number renders above the bar
 * @param offhandSlot         whether the offhand slot renders next to the hotbar
 * @param offhandLeft         whether the offhand slot is on the left of the hotbar
 * @param effects             whether any effect icon renders on the right edge
 * @param beneficialEffects   how many beneficial (top row) effect icons render
 * @param otherEffects        how many non-beneficial (second row) effect icons render
 * @param demo                whether the demo countdown shifts the effect rows down
 * @param bossBars            one entry per rendered boss bar
 * @param chat                whether the chat panel renders
 * @param chatX               the chat panel's x origin (0 unless a mod repositioned it)
 * @param chatWidth           the chat panel width in screen pixels
 * @param chatHeight          the chat panel height in screen pixels
 * @param toasts              one entry per visible toast slot run
 * @param subtitleHalfWidth   the subtitle row half-width (screen pixels; 0 hides subtitles)
 * @param subtitleCount       how many subtitle rows render
 */
public record HudInputs(
        int screenWidth,
        int screenHeight,
        int leftHeight,
        int rightHeight,
        boolean healthColumns,
        boolean experienceBar,
        boolean jumpMeter,
        boolean experienceLevel,
        boolean offhandSlot,
        boolean offhandLeft,
        boolean effects,
        int beneficialEffects,
        int otherEffects,
        boolean demo,
        List<BossBar> bossBars,
        boolean chat,
        int chatX,
        int chatWidth,
        int chatHeight,
        List<Toast> toasts,
        int subtitleHalfWidth,
        int subtitleCount) {

    /** A rendered boss bar: progress-bar y origin and the per-bar row increment. */
    public record BossBar(int y, int increment) {}

    /** A visible toast: first occupied slot index, slot run length, and pixel width. */
    public record Toast(int index, int slotCount, int width) {}

    public HudInputs {
        bossBars = List.copyOf(bossBars);
        toasts = List.copyOf(toasts);
    }

    public static Builder builder(int screenWidth, int screenHeight) {
        return new Builder(screenWidth, screenHeight);
    }

    /** Fluent constructor for the long positional record. */
    public static final class Builder {

        private final int screenWidth;
        private final int screenHeight;
        private int leftHeight = 39;
        private int rightHeight = 39;
        private boolean healthColumns;
        private boolean experienceBar;
        private boolean jumpMeter;
        private boolean experienceLevel;
        private boolean offhandSlot;
        private boolean offhandLeft;
        private boolean effects;
        private int beneficialEffects;
        private int otherEffects;
        private boolean demo;
        private List<BossBar> bossBars = List.of();
        private boolean chat;
        private int chatX;
        private int chatWidth;
        private int chatHeight;
        private List<Toast> toasts = List.of();
        private int subtitleHalfWidth;
        private int subtitleCount;

        private Builder(int screenWidth, int screenHeight) {
            this.screenWidth = screenWidth;
            this.screenHeight = screenHeight;
        }

        public Builder leftHeight(int value) {
            this.leftHeight = value;
            return this;
        }

        public Builder rightHeight(int value) {
            this.rightHeight = value;
            return this;
        }

        public Builder healthColumns(boolean value) {
            this.healthColumns = value;
            return this;
        }

        public Builder experienceBar(boolean value) {
            this.experienceBar = value;
            return this;
        }

        public Builder jumpMeter(boolean value) {
            this.jumpMeter = value;
            return this;
        }

        public Builder experienceLevel(boolean value) {
            this.experienceLevel = value;
            return this;
        }

        public Builder offhandSlot(boolean value) {
            this.offhandSlot = value;
            return this;
        }

        public Builder offhandLeft(boolean value) {
            this.offhandLeft = value;
            return this;
        }

        public Builder effects(boolean value) {
            this.effects = value;
            return this;
        }

        public Builder beneficialEffects(int count) {
            this.beneficialEffects = count;
            return this;
        }

        public Builder otherEffects(int count) {
            this.otherEffects = count;
            return this;
        }

        public Builder demo(boolean value) {
            this.demo = value;
            return this;
        }

        public Builder bossBars(List<BossBar> value) {
            this.bossBars = value;
            return this;
        }

        public Builder chat(boolean value) {
            this.chat = value;
            return this;
        }

        public Builder chatX(int value) {
            this.chatX = value;
            return this;
        }

        public Builder chatSize(int width, int height) {
            this.chatWidth = width;
            this.chatHeight = height;
            return this;
        }

        public Builder toasts(List<Toast> value) {
            this.toasts = value;
            return this;
        }

        public Builder subtitles(int halfWidth, int count) {
            this.subtitleHalfWidth = halfWidth;
            this.subtitleCount = count;
            return this;
        }

        public HudInputs build() {
            return new HudInputs(
                    screenWidth,
                    screenHeight,
                    leftHeight,
                    rightHeight,
                    healthColumns,
                    experienceBar,
                    jumpMeter,
                    experienceLevel,
                    offhandSlot,
                    offhandLeft,
                    effects,
                    beneficialEffects,
                    otherEffects,
                    demo,
                    bossBars,
                    chat,
                    chatX,
                    chatWidth,
                    chatHeight,
                    toasts,
                    subtitleHalfWidth,
                    subtitleCount);
        }
    }
}
