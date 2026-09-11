package dev.vfyjxf.cloudlib.test.inworld;

import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.canvas.SceneCanvas;
import dev.vfyjxf.cloudlib.ui.inworld.InworldTheme;
import dev.vfyjxf.taffy.geometry.FloatSize;
import net.minecraft.world.entity.LivingEntity;

/**
 * Compact entity tag: a name line with the hp count on the right and a 2px
 * health bar underneath — kept deliberately small so tags stay unobtrusive
 * next to their entities (distance shrinks them further via distScale).
 */
public final class EntityTagWidget extends Widget {

    private static final int MIN_W = 26;
    private static final int H = 12;
    private static final int HP_LOW = 0xFFE06666;

    private final LivingEntity entity;
    private final String label;

    public EntityTagWidget(LivingEntity entity) {
        this(entity, null);
    }

    public EntityTagWidget(LivingEntity entity, String label) {
        this.entity = entity;
        this.label = label;
        onMount((scene, context, handle) ->
                scene.layoutTree().setMeasureFunc(nodeId(), (style, space) -> {
                    var font = context.font();
                    //name + room for a two-digit hp suffix
                    int w = font.width("◇ " + name()) + font.width(" 99");
                    return new FloatSize(Math.max(MIN_W, w), H);
                }));
    }

    private String name() {
        return label != null ? label : entity.getName().getString();
    }

    @Override
    protected void renderInternal(SceneCanvas canvas, int mouseX, int mouseY, float partialTicks) {
        var font = context().font();
        int w = width();

        canvas.text("◇ " + name(), 0, 0, InworldTheme.ACCENT);
        String hp = (int) Math.ceil(entity.getHealth()) + "";
        canvas.text(hp, w - font.width(hp), 0, InworldTheme.TEXT_DIM);

        float frac = entity.getMaxHealth() > 0 ? entity.getHealth() / entity.getMaxHealth() : 0;
        int barY = font.lineHeight + 1;
        canvas.fill(0, barY, w, 2, 0x66061012);
        int fill = (int) (w * Math.clamp(frac, 0f, 1f));
        if (fill > 0) {
            canvas.fill(0, barY, fill, 2, frac < 0.3f ? HP_LOW : InworldTheme.ACCENT);
        }
    }
}
