package dev.vfyjxf.cloudlib.test.inworld;

import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.canvas.SceneCanvas;
import dev.vfyjxf.cloudlib.ui.inworld.InworldTheme;
import dev.vfyjxf.taffy.geometry.FloatSize;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;

/**
 * Compact entity tag: a name line plus a 2px health bar underneath — richer
 * than the plain text tag, still small enough for the group-merge and rail
 * layouts. Right side of the name line shows "hp·dist".
 */
public final class EntityTagWidget extends Widget {

    private static final int MIN_W = 40;
    private static final int H = 14;
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
                    //reserve room for the widest "hp·dist" suffix (e.g. " 20·99m")
                    int w = font.width("◇ " + name()) + font.width(" 20·99m");
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

        var player = Minecraft.getInstance().player;
        String right = entity == player
                ? (int) Math.ceil(entity.getHealth()) + ""
                : (int) Math.ceil(entity.getHealth()) + "·"
                + (player == null ? "?" : (int) Math.sqrt(player.distanceToSqr(entity)) + "m");
        canvas.text(right, w - font.width(right), 0, InworldTheme.TEXT_DIM);

        float frac = entity.getMaxHealth() > 0 ? entity.getHealth() / entity.getMaxHealth() : 0;
        int barY = font.lineHeight + 1;
        canvas.fill(0, barY, w, 3, 0x66061012);
        canvas.strokeRect(0, barY, w, 3, 0x5535D6D0);
        int fill = (int) ((w - 2) * Math.clamp(frac, 0f, 1f));
        if (fill > 0) {
            canvas.fill(1, barY + 1, fill, 1, frac < 0.3f ? HP_LOW : InworldTheme.ACCENT);
        }
    }
}
