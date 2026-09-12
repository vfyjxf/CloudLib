package dev.vfyjxf.nimbusprojection.api.section;

import net.minecraft.resources.ResourceLocation;

/**
 * Phantom-typed token identifying a section kind — the
 * {@code CustomPacketPayload.Type} idiom: the generic only exists at
 * compile time to keep providers, codecs and widget factories aligned.
 *
 * @param <D> the snapshot data this section carries
 */
public record SectionType<D extends SectionData>(ResourceLocation id) {

    public static <D extends SectionData> SectionType<D> of(String namespace, String path) {
        return new SectionType<>(ResourceLocation.fromNamespaceAndPath(namespace, path));
    }
}
