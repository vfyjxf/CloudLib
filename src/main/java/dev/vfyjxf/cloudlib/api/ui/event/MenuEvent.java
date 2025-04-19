package dev.vfyjxf.cloudlib.api.ui.event;

import dev.vfyjxf.cloudlib.api.event.EventDefinition;
import dev.vfyjxf.cloudlib.api.event.EventFactory;
import net.minecraft.world.entity.player.Player;

public interface MenuEvent {


    EventDefinition<QuickMove> onQuickMove = EventFactory.define(
            QuickMove.class, (listeners) -> (player, from) -> {
                for (QuickMove listener : listeners) {
                    if (listener.onQuickMove(player, from)) {
                        return true;
                    }
                }
                return false;
            }
    );

    interface QuickMove extends MenuEvent {
        boolean onQuickMove(Player player, Object from);
    }

}
