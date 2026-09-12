package dev.vfyjxf.nimbusprojection.api.section;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.ApiStatus;

/**
 * The narrow registration surface handed to
 * {@code NimbusPlugin.registerContainerSections} — one call wires a
 * section kind end to end: its token, its wire codec, and the provider
 * that produces snapshots.
 */
@ApiStatus.NonExtendable
public interface SectionRegister {

    <D extends SectionData> void register(
            SectionType<D> type, StreamCodec<RegistryFriendlyByteBuf, D> codec, SectionProvider<D> provider);
}
