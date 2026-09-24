package dev.vfyjxf.cloudlib.api.ui.tooltip;

import dev.vfyjxf.cloudlib.api.math.FloatPos;
import dev.vfyjxf.cloudlib.util.ScreenUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.neoforged.neoforge.common.util.AttributeTooltipContext;
import org.jspecify.annotations.Nullable;

/**
 * The tooltip context the item and attribute tooltip producers are handed: the
 * registry lookup, the level, the player and the flag their rendering may ask
 * for. Everything a caller cannot supply stays absent — a context built with no
 * world carries a null level, a null player and, with them, no registry lookup,
 * which is {@link Item.TooltipContext#EMPTY}'s answer too.
 */
public record TooltipContext(
    HolderLookup.@Nullable Provider registries,
    @Nullable Level level,
    @Nullable Player player,
    @Nullable TooltipFlag flag,
    float tickRate
) implements AttributeTooltipContext {

    public static TooltipContext create(
        @Nullable Level level,
        @Nullable Player player,
        @Nullable TooltipFlag tooltipFlag
    ) {
        var minecraft = Minecraft.getInstance();
        var advancedItemTooltips = minecraft.options.advancedItemTooltips;
        var resolvedFlag = tooltipFlag == null
                ? (advancedItemTooltips ? TooltipFlag.ADVANCED : TooltipFlag.NORMAL)
                : tooltipFlag;
        var noWorld = Item.TooltipContext.EMPTY;
        var registryAccess = level == null ? noWorld.registries() : level.registryAccess();
        float tickRate = level == null ? noWorld.tickRate() : level.tickRateManager().tickrate();
        return new TooltipContext(registryAccess, level, player, resolvedFlag, tickRate);
    }

    public static TooltipContext create() {
        var minecraft = Minecraft.getInstance();
        return create(minecraft.level, minecraft.player, null);
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
