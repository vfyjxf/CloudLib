package dev.vfyjxf.cloudlib.api.unit.text;

import dev.vfyjxf.cloudlib.api.annotation.NotNullByDefault;
import dev.vfyjxf.cloudlib.api.unit.MaterialAmount;
import dev.vfyjxf.cloudlib.api.unit.Quantity;
import dev.vfyjxf.cloudlib.api.unit.Unit;
import dev.vfyjxf.cloudlib.api.unit.UnitConverter;
import dev.vfyjxf.cloudlib.api.util.Namespace;
import dev.vfyjxf.cloudlib.util.Checks;
import net.minecraft.network.chat.Component;

/**
 * Minecraft adapter rendering quantities as {@link Component}s.
 * Unit names resolve through translatable lang keys of the form
 * {@code unit.<root>.<path>} (e.g. {@code unit.minecraft.tick}).
 * Only load this class game-side.
 */
@NotNullByDefault
public final class UnitComponents {

    /**
     * The lang key for a unit, e.g. {@code minecraft:tick} → {@code "unit.minecraft.tick"}.
     */
    public static String langKey(Unit<?> unit) {
        Namespace id = unit.id();
        return "unit." + id.root() + "." + id.path().replace('/', '.').replace('_', '.');
    }

    private final QuantityFormatter formatter;

    public UnitComponents(UnitConverter converter, UnitNames names) {
        this(new QuantityFormatter(converter, names));
    }

    public UnitComponents(QuantityFormatter formatter) {
        Checks.checkNotNull(formatter, "formatter");
        this.formatter = formatter;
    }

    /**
     * {@code <value> <translatable unit name>}, e.g. {@code 20 unit.minecraft.tick}.
     */
    public Component format(Quantity<?> quantity) {
        Checks.checkNotNull(quantity, "quantity");
        String valueText = quantity.value().isIntegral()
                ? Long.toString(quantity.value().toLongExact())
                : quantity.value().toString();
        return Component.literal(valueText + " ").append(Component.translatable(langKey(quantity.unit())));
    }

    /**
     * {@code <value> <translatable base unit name>} of a material amount.
     */
    public Component format(MaterialAmount amount) {
        Checks.checkNotNull(amount, "amount");
        String valueText = amount.value().isIntegral()
                ? Long.toString(amount.value().toLongExact())
                : amount.value().toString();
        return Component.literal(valueText + " ").append(Component.translatable(langKey(amount.baseUnit())));
    }

    /**
     * Short-form mixed-unit breakdown, e.g. {@code 1h 23m 20s}.
     */
    @SafeVarargs
    public final <F> Component decompose(Quantity<F> quantity, Unit<F>... chain) {
        return Component.literal(formatter.formatDecomposed(quantity, chain));
    }
}
