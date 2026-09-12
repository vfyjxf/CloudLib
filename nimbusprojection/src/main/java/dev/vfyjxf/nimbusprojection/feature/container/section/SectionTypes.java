package dev.vfyjxf.nimbusprojection.feature.container.section;

import dev.vfyjxf.nimbusprojection.Constants;
import dev.vfyjxf.nimbusprojection.api.section.SectionType;
import dev.vfyjxf.nimbusprojection.feature.machine.section.HiveSectionData;
import dev.vfyjxf.nimbusprojection.feature.machine.section.ProgressSectionData;
import dev.vfyjxf.nimbusprojection.feature.machine.section.TextSectionData;

/**
 * The built-in section kinds — registered by the builtin plugin, kept as
 * tokens here so widgets and ops can address them.
 */
public final class SectionTypes {

    public static final SectionType<ItemSectionData> item = SectionType.of(Constants.namespace, "item");
    public static final SectionType<FluidSectionData> fluid = SectionType.of(Constants.namespace, "fluid");
    public static final SectionType<EnergySectionData> energy = SectionType.of(Constants.namespace, "energy");

    // machine data faces — progress, written text, hive occupancy
    public static final SectionType<ProgressSectionData> progress = SectionType.of(Constants.namespace, "progress");
    public static final SectionType<TextSectionData> text = SectionType.of(Constants.namespace, "text");
    public static final SectionType<HiveSectionData> hive = SectionType.of(Constants.namespace, "hive");

    private SectionTypes() {}
}
