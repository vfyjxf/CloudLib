package dev.vfyjxf.nimbusprojection.api.sync;

import dev.vfyjxf.nimbusprojection.api.panel.PanelSpec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Client-side materializer for a server-declared shared panel. Registered
 * under a {@link net.minecraft.resources.ResourceLocation} view id via
 * {@link dev.vfyjxf.nimbusprojection.api.NimbusClient#registerView}.
 * <p>
 * When the server shares a {@link SharedPanelSpec} naming this view, each
 * watching client calls {@link #open} once with the decoded payload; the
 * returned spec joins the normal panel lifecycle (its key is the shared
 * panel's key).
 */
@FunctionalInterface
public interface SharedPanelView<P extends CustomPacketPayload> {

    PanelSpec open(SharedViewContext<P> context);
}
