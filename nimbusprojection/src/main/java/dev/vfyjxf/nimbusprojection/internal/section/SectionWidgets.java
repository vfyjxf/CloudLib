package dev.vfyjxf.nimbusprojection.internal.section;

import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.inworld.InworldPanelContext;
import dev.vfyjxf.nimbusprojection.api.section.SectionData;
import dev.vfyjxf.nimbusprojection.api.section.SectionInstance;
import dev.vfyjxf.nimbusprojection.api.section.SectionTarget;
import dev.vfyjxf.nimbusprojection.api.section.SectionType;
import dev.vfyjxf.nimbusprojection.api.section.SectionView;
import dev.vfyjxf.nimbusprojection.api.section.SectionWidgetFactory;
import dev.vfyjxf.nimbusprojection.api.section.SectionWidgetRegister;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The client-side widget-factory registry — one factory per section type,
 * bound through {@code NimbusClientPlugin.registerSectionWidgets}.
 * {@link #create} wraps an instance into a typed {@link SectionView}
 * whose data source reads the {@link SectionContents} mirror.
 */
public final class SectionWidgets {

    private static final Map<SectionType<?>, SectionWidgetFactory<?>> factories = new ConcurrentHashMap<>();

    /** The {@link SectionWidgetRegister} handed to client plugins. */
    public static final SectionWidgetRegister register = new SectionWidgetRegister() {
        @Override
        public <D extends SectionData> void register(SectionType<D> type, SectionWidgetFactory<D> factory) {
            factories.put(type, factory);
        }
    };

    private SectionWidgets() {}

    /** Widget for one instance — null when no factory is bound to its type. */
    public static @Nullable Widget create(InworldPanelContext ctx, SectionTarget target, SectionInstance<?> instance) {
        return createTyped(ctx, target, instance);
    }

    @SuppressWarnings("unchecked")
    private static <D extends SectionData> @Nullable Widget createTyped(
            InworldPanelContext ctx, SectionTarget target, SectionInstance<D> instance) {
        SectionWidgetFactory<D> factory = (SectionWidgetFactory<D>) factories.get(instance.type());
        if (factory == null) return null;
        SectionView<D> view = new SectionView<>(ctx, target, instance.id(), instance.type(), instance.data(), () ->
                (D) SectionContents.latest(target, instance.id()));
        return factory.create(view);
    }
}
