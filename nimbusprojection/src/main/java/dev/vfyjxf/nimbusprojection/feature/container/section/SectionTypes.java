package dev.vfyjxf.nimbusprojection.feature.container.section;

import dev.vfyjxf.nimbusprojection.Constants;
import dev.vfyjxf.nimbusprojection.api.section.SectionType;

/**
 * The built-in section kinds — registered by the builtin plugin, kept as
 * tokens here so widgets and ops can address them.
 */
public final class SectionTypes {

    public static final SectionType<ItemSectionData> item = SectionType.of(Constants.namespace, "item");
    public static final SectionType<FluidSectionData> fluid = SectionType.of(Constants.namespace, "fluid");
    public static final SectionType<EnergySectionData> energy = SectionType.of(Constants.namespace, "energy");

    private SectionTypes() {}
}
