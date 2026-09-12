package dev.vfyjxf.nimbusprojection.api.section;

import org.jetbrains.annotations.ApiStatus;

/**
 * The client-side registration surface handed to
 * {@code NimbusClientPlugin.registerSectionWidgets} — binds a section
 * kind to the factory that renders it.
 */
@ApiStatus.NonExtendable
public interface SectionWidgetRegister {

    <D extends SectionData> void register(SectionType<D> type, SectionWidgetFactory<D> factory);
}
