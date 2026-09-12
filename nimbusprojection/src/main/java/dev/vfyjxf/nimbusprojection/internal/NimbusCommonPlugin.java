package dev.vfyjxf.nimbusprojection.internal;

import dev.vfyjxf.cloudlib.api.plugin.PluginMarker;
import dev.vfyjxf.cloudlib.api.util.Namespace;
import dev.vfyjxf.nimbusprojection.api.plugin.NimbusPlugin;
import dev.vfyjxf.nimbusprojection.api.section.SectionRegister;
import dev.vfyjxf.nimbusprojection.feature.container.section.EnergySectionData;
import dev.vfyjxf.nimbusprojection.feature.container.section.EnergySectionProvider;
import dev.vfyjxf.nimbusprojection.feature.container.section.FluidSectionData;
import dev.vfyjxf.nimbusprojection.feature.container.section.FluidSectionProvider;
import dev.vfyjxf.nimbusprojection.feature.container.section.ItemSectionData;
import dev.vfyjxf.nimbusprojection.feature.container.section.ItemSectionProvider;
import dev.vfyjxf.nimbusprojection.feature.container.section.SectionTypes;

/**
 * Nimbus's common-side registrations — the section kinds' data half
 * (token + codec + provider), needed on the server for snapshots and on
 * the client for panel structure.
 */
@PluginMarker
public final class NimbusCommonPlugin implements NimbusPlugin {

    @Override
    public Namespace pluginId() {
        return NimbusPlugin.builtin;
    }

    @Override
    public void registerContainerSections(SectionRegister register) {
        register.register(SectionTypes.item, ItemSectionData.streamCodec, new ItemSectionProvider());
        register.register(SectionTypes.fluid, FluidSectionData.streamCodec, new FluidSectionProvider());
        register.register(SectionTypes.energy, EnergySectionData.streamCodec, new EnergySectionProvider());
    }
}
