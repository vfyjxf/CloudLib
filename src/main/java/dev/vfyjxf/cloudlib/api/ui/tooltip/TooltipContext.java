package dev.vfyjxf.cloudlib.api.ui.tooltip;

import dev.vfyjxf.cloudlib.api.math.FloatPos;
import dev.vfyjxf.cloudlib.util.ScreenUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.neoforged.neoforge.common.util.AttributeTooltipContext;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public record TooltipContext(
    HolderLookup.Provider registries,
    Level level,
    Player player,
    TooltipFlag flag,
    float tickRate
) implements AttributeTooltipContext {

    public static TooltipContext create(@Nullable Level level, @Nullable Player player, @Nullable TooltipFlag tooltipFlag) {
        var minecraft = Minecraft.getInstance();
        level = level == null ? Objects.requireNonNull(minecraft.level) : level;
        player = player == null ? Objects.requireNonNull(minecraft.player) : player;
        var advancedItemTooltips = minecraft.options.advancedItemTooltips;
        tooltipFlag = tooltipFlag == null ? (
            advancedItemTooltips ? TooltipFlag.ADVANCED : TooltipFlag.NORMAL
        ) : tooltipFlag;
        var registryAccess = level.registryAccess();
        float tickRate = level.tickRateManager().tickrate();
        return new TooltipContext(registryAccess, level, player, tooltipFlag, tickRate);
    }

    public static TooltipContext create() {
        var minecraft = Minecraft.getInstance();
        var level = minecraft.level;
        return create(level, minecraft.player, null);
    }

    public FloatPos mousePos() {
        return ScreenUtil.getMousePos();
    }

    public boolean isCtrlDown() {
        return Screen.hasControlDown();
    }

    public boolean isShiftDown() {
        return Screen.hasShiftDown();
    }

    public boolean isAltDown() {
        return Screen.hasAltDown();
    }

    @Override
    public TooltipFlag flag() {
        if (flag == null) {
            var options = Minecraft.getInstance().options;
            if (options.advancedItemTooltips) return TooltipFlag.ADVANCED;
            else return TooltipFlag.NORMAL;
        } else return flag;
    }

    @Override
    public @Nullable MapItemSavedData mapData(MapId mapId) {
        return level == null ? null : level.getMapData(mapId);
    }
}