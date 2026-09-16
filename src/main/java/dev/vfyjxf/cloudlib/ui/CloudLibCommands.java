package dev.vfyjxf.cloudlib.ui;

import dev.vfyjxf.cloudlib.api.ui.style.Themes;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

import static net.minecraft.commands.Commands.literal;

/**
 * Client-side debug commands ({@code /cloudlib ...}).
 * <p>
 * {@code cloudlib reload} re-parses every theme descriptor and stylesheet —
 * the manual entry for theme development, alongside the file watcher.
 */
public final class CloudLibCommands {

    private CloudLibCommands() {}

    public static void register(RegisterClientCommandsEvent event) {
        event.getDispatcher()
                .register(literal("cloudlib").then(literal("reload").executes(ctx -> {
                    Themes.reload();
                    ctx.getSource()
                            .sendSystemMessage(Component.literal("Themes reloaded — active: " + Themes.activeIds()));
                    return 1;
                })));
    }
}
