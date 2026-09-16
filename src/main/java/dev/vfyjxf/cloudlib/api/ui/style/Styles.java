package dev.vfyjxf.cloudlib.api.ui.style;

import dev.vfyjxf.cloudlib.api.ui.style.key.BuiltinKeys;
import dev.vfyjxf.cloudlib.api.ui.style.key.StyleKey;

/**
 * The builtin style vocabulary — every supported css property as a
 * {@link StyleKey} constant ({@code Styles.padding}, {@code Styles.color}, …).
 * <p>
 * The vocabulary is <b>closed</b>: {@link StyleKey} cannot be constructed
 * outside the library, so these constants are the whole property set. Custom
 * styling goes through css custom properties ({@code --name}) — declare them in
 * theme css and read them in java through a {@link StyleVar} lens; no
 * registration is needed on either side.
 * <p>
 * All members are inherited from {@link BuiltinKeys} — this type exists to keep
 * the friendly {@code Styles.*} name in the {@code style} package.
 */
public final class Styles extends BuiltinKeys {

    private Styles() {}

    /** Forces {@code <clinit>} — every constant and table entry is built. */
    public static void init() {
        BuiltinKeys.init();
    }

    // byId / shorthand / all / names are inherited from BuiltinKeys.
}
