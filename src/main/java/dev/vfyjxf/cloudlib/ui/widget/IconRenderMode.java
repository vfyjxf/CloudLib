package dev.vfyjxf.cloudlib.ui.widget;

/**
 * How an icon cell draws its item.
 * <ul>
 *   <li>{@link #flat} — the item's atlas sprite through {@code SceneCanvas.renderItemIcon},
 *       the batched path that also rasterizes inside offscreen panel targets;</li>
 *   <li>{@link #model} — the vanilla item renderer through {@code SceneCanvas.renderItem},
 *       the layered forward path that clears depth and lifts the z-offset per draw, so a
 *       block or a shaped item keeps its 3D model.</li>
 * </ul>
 * The forward path costs a depth clear and a batch flush per icon, so a long row pays for
 * every cell; {@link #flat} is the cheap alternative when the panel already knows the
 * models don't read at its size.
 */
public enum IconRenderMode {

    /** The flat atlas sprite — batchable, one quad per icon. */
    flat,

    /** The vanilla 3D item/block model — one layered forwarded draw per icon. */
    model
}
