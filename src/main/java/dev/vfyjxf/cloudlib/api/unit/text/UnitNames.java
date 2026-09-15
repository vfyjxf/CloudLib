package dev.vfyjxf.cloudlib.api.unit.text;

import dev.vfyjxf.cloudlib.api.annotation.NotNullByDefault;
import dev.vfyjxf.cloudlib.api.unit.Unit;
import dev.vfyjxf.cloudlib.api.util.Namespace;
import dev.vfyjxf.cloudlib.util.Checks;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Provides display names for units.
 */
@NotNullByDefault
public interface UnitNames {

    /**
     * Derives names from unit id paths: {@code minecraft:millibucket} → {@code "millibucket"}.
     */
    static UnitNames defaults() {
        return DerivedNames.INSTANCE;
    }

    static Builder builder() {
        return new Builder();
    }

    private static String derive(Unit<?> unit) {
        String path = unit.id().path();
        int slash = path.lastIndexOf('/');
        if (slash >= 0) path = path.substring(slash + 1);
        return path.replace('_', ' ');
    }

    String name(Unit<?> unit);

    default String name(Unit<?> unit, @Nullable Namespace material) {
        return name(unit);
    }

    final class Builder {

        private final Map<Unit<?>, String> names = new HashMap<>();
        private final Map<Unit<?>, Map<Namespace, String>> qualifiedNames = new HashMap<>();

        private Builder() {
        }

        public Builder name(Unit<?> unit, String name) {
            Checks.checkNotNull(unit, "unit");
            Checks.checkNotNull(name, "name");
            names.put(unit, name);
            return this;
        }

        /**
         * Registers a material qualified name, e.g. iron + ingot → {@code "iron ingot"}.
         */
        public Builder name(Namespace material, Unit<?> unit, String name) {
            Checks.checkNotNull(material, "material");
            Checks.checkNotNull(unit, "unit");
            Checks.checkNotNull(name, "name");
            qualifiedNames.computeIfAbsent(unit, k -> new HashMap<>()).put(material, name);
            return this;
        }

        public UnitNames build() {
            Map<Unit<?>, String> namesCopy = Map.copyOf(names);
            Map<Unit<?>, Map<Namespace, String>> qualifiedCopy = new HashMap<>();
            qualifiedNames.forEach((unit, map) -> qualifiedCopy.put(unit, Map.copyOf(map)));
            return new UnitNames() {
                @Override
                public String name(Unit<?> unit) {
                    return namesCopy.getOrDefault(unit, UnitNames.derive(unit));
                }

                @Override
                public String name(Unit<?> unit, @Nullable Namespace material) {
                    if (material != null) {
                        Map<Namespace, String> map = qualifiedCopy.get(unit);
                        if (map != null) {
                            String qualified = map.get(material);
                            if (qualified != null) return qualified;
                        }
                    }
                    return name(unit);
                }
            };
        }
    }

    final class DerivedNames implements UnitNames {

        private static final DerivedNames INSTANCE = new DerivedNames();

        @Override
        public String name(Unit<?> unit) {
            return derive(unit);
        }
    }
}
