package dev.vfyjxf.cloudlib.ui.hud;

import dev.vfyjxf.cloudlib.Constants;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.components.SubtitleOverlay;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.HumanoidArm;
import net.neoforged.neoforge.client.event.CustomizeGuiOverlayEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientMobEffectExtensions;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The Minecraft-facing half of vanilla HUD exclusion: samples every dynamic
 * value {@link VanillaHudGeometry} needs and feeds it as a {@link HudInputs}.
 * <ul>
 *   <li>{@code leftHeight}/{@code rightHeight}: a no-op layer registered
 *       above {@code VanillaGuiLayers.AIR_LEVEL} (the last vanilla column
 *       layer) reads the running column heights after they are final — the
 *       {@code IGuiOverlay} API no longer exists in NeoForge 21.1</li>
 *   <li>boss bars: each {@code CustomizeGuiOverlayEvent.BossEventProgress}
 *       contributes a row; the list is cleared by the sampling layer each
 *       frame, so it always holds the bars of the frame in progress</li>
 *   <li>chat position: {@code CustomizeGuiOverlayEvent.Chat} posX (0 unless a
 *       mod moved the panel)</li>
 *   <li>toasts: the toast component's occupied-slot bits (via access
 *       transformer), assuming vanilla's 160 px toast width</li>
 *   <li>subtitles: the subtitle overlay's audible rows measured with the
 *       vanilla font (via access transformer)</li>
 * </ul>
 */
public final class VanillaHudSampler {

    /** Vanilla {@code Toast#width()} — every vanilla toast sprite is 160 px wide. */
    private static final int toastWidth = 160;

    private static final ResourceLocation samplerLayerId = ResourceLocation
            .fromNamespaceAndPath(Constants.modId, "hud_exclusion_sampler");

    private final List<HudInputs.BossBar> bossBars = new CopyOnWriteArrayList<>();
    private volatile int leftHeight = 39;
    private volatile int rightHeight = 39;
    private volatile int chatX;

    /** Registers the height-sampling layer above the vanilla air layer. Call on the mod bus. */
    public void registerGuiLayer(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.AIR_LEVEL, samplerLayerId, this::sampleHeights);
    }

    private void sampleHeights(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        Gui gui = Minecraft.getInstance().gui;
        if (gui == null) {
            return;
        }
        leftHeight = gui.leftHeight;
        rightHeight = gui.rightHeight;
        bossBars.clear();
    }

    /** Records one rendered boss-bar row; subscribe on the game bus. */
    public void onBossEventProgress(CustomizeGuiOverlayEvent.BossEventProgress event) {
        if (!event.isCanceled()) {
            bossBars.add(new HudInputs.BossBar(event.getY(), event.getIncrement()));
        }
    }

    /** Tracks the chat panel x origin; subscribe on the game bus. */
    public void onChatOverlay(CustomizeGuiOverlayEvent.Chat event) {
        chatX = event.getPosX();
    }

    /** Captures the current frame's HUD state. */
    public HudInputs sample() {
        Minecraft minecraft = Minecraft.getInstance();
        Gui gui = minecraft.gui;
        HudInputs.Builder builder = HudInputs
                .builder(minecraft.getWindow().getGuiScaledWidth(), minecraft.getWindow().getGuiScaledHeight())
                .leftHeight(leftHeight).rightHeight(rightHeight).demo(minecraft.isDemo())
                .bossBars(List.copyOf(bossBars));
        LocalPlayer player = minecraft.player;
        if (gui != null && player != null && minecraft.gameMode != null) {
            builder.healthColumns(minecraft.gameMode.canHurtPlayer());
            boolean ridingJumpable = player.jumpableVehicle() != null;
            boolean hasExperience = minecraft.gameMode.hasExperience();
            builder.experienceBar(!ridingJumpable && hasExperience);
            builder.jumpMeter(ridingJumpable);
            builder.experienceLevel(!ridingJumpable && hasExperience && player.experienceLevel > 0);
            builder.offhandSlot(!player.getOffhandItem().isEmpty());
            builder.offhandLeft(player.getMainArm().getOpposite() == HumanoidArm.LEFT);
            sampleEffects(minecraft, player, builder);
            sampleChat(gui, builder);
            sampleSubtitles(minecraft, gui, builder);
        }
        builder.toasts(sampleToasts(minecraft));
        return builder.build();
    }

    private static void sampleEffects(Minecraft minecraft, LocalPlayer player, HudInputs.Builder builder) {
        if (minecraft.screen instanceof EffectRenderingInventoryScreen<?> inventory && inventory.canSeeEffects()) {
            return; // the open inventory screen renders the effect panel itself
        }
        int beneficial = 0;
        int other = 0;
        for (MobEffectInstance instance : player.getActiveEffects()) {
            if (!instance.showIcon()) {
                continue;
            }
            if (!IClientMobEffectExtensions.of(instance).isVisibleInGui(instance)) {
                continue;
            }
            if (instance.getEffect().value().isBeneficial()) {
                beneficial++;
            } else {
                other++;
            }
        }
        if (beneficial + other > 0) {
            builder.effects(true).beneficialEffects(beneficial).otherEffects(other);
        }
    }

    private void sampleChat(Gui gui, HudInputs.Builder builder) {
        ChatComponent chat = gui.getChat();
        if (!chat.isChatHidden() && !chat.isChatFocused()) {
            builder.chat(true).chatX(chatX).chatSize(chat.getWidth(), chat.getHeight());
        }
    }

    private static List<HudInputs.Toast> sampleToasts(Minecraft minecraft) {
        BitSet occupiedSlots = minecraft.getToasts().occupiedSlots;
        List<HudInputs.Toast> toasts = new ArrayList<>();
        for (int slot = occupiedSlots.nextSetBit(0); slot >= 0;) {
            int runLength = 1;
            while (occupiedSlots.get(slot + runLength)) {
                runLength++;
            }
            toasts.add(new HudInputs.Toast(slot, runLength, toastWidth));
            slot = occupiedSlots.nextSetBit(slot + runLength);
        }
        return toasts;
    }

    private static void sampleSubtitles(Minecraft minecraft, Gui gui, HudInputs.Builder builder) {
        if (!minecraft.options.showSubtitles().get()) {
            return;
        }
        List<SubtitleOverlay.Subtitle> audible = gui.subtitleOverlay.audibleSubtitles;
        if (audible.isEmpty()) {
            return;
        }
        int maxWidth = 0;
        for (SubtitleOverlay.Subtitle subtitle : audible) {
            maxWidth = Math.max(maxWidth, minecraft.font.width(subtitle.getText()));
        }
        int arrowWidth = minecraft.font.width("<") + 2 * minecraft.font.width(" ") + minecraft.font.width(">");
        builder.subtitles((maxWidth + arrowWidth) / 2, audible.size());
    }
}
