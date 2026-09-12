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
import dev.vfyjxf.nimbusprojection.feature.entity.EntityItemSectionProvider;
import dev.vfyjxf.nimbusprojection.feature.machine.section.HiveSectionData;
import dev.vfyjxf.nimbusprojection.feature.machine.section.HiveSectionProvider;
import dev.vfyjxf.nimbusprojection.feature.machine.section.ProgressSectionData;
import dev.vfyjxf.nimbusprojection.feature.machine.section.ProgressSectionProvider;
import dev.vfyjxf.nimbusprojection.feature.machine.section.TextSectionData;
import dev.vfyjxf.nimbusprojection.feature.machine.section.TextSectionProvider;

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
        // entity inventories ride the same item data shape — a chest boat's
        // sections serialize identically to a chest's
        register.registerEntity(SectionTypes.item, ItemSectionData.streamCodec, new EntityItemSectionProvider());
        // machine data faces — non-capability BE state the menu would sync
        register.register(SectionTypes.progress, ProgressSectionData.streamCodec, new ProgressSectionProvider());
        register.register(SectionTypes.text, TextSectionData.streamCodec, new TextSectionProvider());
        register.register(SectionTypes.hive, HiveSectionData.streamCodec, new HiveSectionProvider());
    }
}
