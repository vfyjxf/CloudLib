package dev.vfyjxf.cloudlib.api.ui.inworld.render;

import com.mojang.blaze3d.vertex.BufferBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * Small emit helpers for {@link WorldUiPanel.LinesEmitter} — world-space
 * {@code POSITION_COLOR} line primitives baked through the frame's
 * world→view matrix.
 */
public final class WorldLines {

    private WorldLines() {}

    /** One segment between two world points, alpha carried by {@code argb}. */
    public static void line(BufferBuilder b, Matrix4f w2v, Vec3 a, Vec3 c, int argb) {
        float r = ((argb >> 16) & 0xFF) / 255f;
        float g = ((argb >> 8) & 0xFF) / 255f;
        float bl = (argb & 0xFF) / 255f;
        float al = ((argb >>> 24) & 0xFF) / 255f;
        b.addVertex(w2v, (float) a.x, (float) a.y, (float) a.z).setColor(r, g, bl, al);
        b.addVertex(w2v, (float) c.x, (float) c.y, (float) c.z).setColor(r, g, bl, al);
    }

    /** The 12 edges of an AABB. */
    public static void box(BufferBuilder b, Matrix4f w2v, AABB box, int argb) {
        double x0 = box.minX, y0 = box.minY, z0 = box.minZ;
        double x1 = box.maxX, y1 = box.maxY, z1 = box.maxZ;
        // bottom loop
        line(b, w2v, new Vec3(x0, y0, z0), new Vec3(x1, y0, z0), argb);
        line(b, w2v, new Vec3(x1, y0, z0), new Vec3(x1, y0, z1), argb);
        line(b, w2v, new Vec3(x1, y0, z1), new Vec3(x0, y0, z1), argb);
        line(b, w2v, new Vec3(x0, y0, z1), new Vec3(x0, y0, z0), argb);
        // top loop
        line(b, w2v, new Vec3(x0, y1, z0), new Vec3(x1, y1, z0), argb);
        line(b, w2v, new Vec3(x1, y1, z0), new Vec3(x1, y1, z1), argb);
        line(b, w2v, new Vec3(x1, y1, z1), new Vec3(x0, y1, z1), argb);
        line(b, w2v, new Vec3(x0, y1, z1), new Vec3(x0, y1, z0), argb);
        // pillars
        line(b, w2v, new Vec3(x0, y0, z0), new Vec3(x0, y1, z0), argb);
        line(b, w2v, new Vec3(x1, y0, z0), new Vec3(x1, y1, z0), argb);
        line(b, w2v, new Vec3(x1, y0, z1), new Vec3(x1, y1, z1), argb);
        line(b, w2v, new Vec3(x0, y0, z1), new Vec3(x0, y1, z1), argb);
    }

    /** The 12 edges of the unit cube at {@code pos}, slightly inflated. */
    public static void blockFrame(BufferBuilder b, Matrix4f w2v, BlockPos pos, int argb) {
        box(b, w2v, new AABB(pos).inflate(0.004), argb);
    }
}
