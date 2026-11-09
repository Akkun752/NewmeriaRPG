package fr.akkun.newmeriarpg.client;

import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.util.Mth;
import org.joml.Matrix3x2fc;
import org.jspecify.annotations.Nullable;

/**
 * A single filled pie wedge (a triangle fan approximated by {@link #SEGMENTS} thin triangles),
 * built the same way vanilla's own {@code ColoredRectangleRenderState} builds a rectangle: the
 * GUI pipeline draws in 4-vertex "quads" (2 triangles each), so each arc step is submitted as
 * (center, arcPoint, nextArcPoint, center) - the repeated center collapses the quad's second
 * triangle to zero area, leaving a clean filled triangle.
 */
record SpellWheelWedgeRenderState(
        RenderPipeline pipeline,
        TextureSetup textureSetup,
        Matrix3x2fc pose,
        float centerX,
        float centerY,
        float radius,
        float startAngle,
        float endAngle,
        int color,
        @Nullable ScreenRectangle scissorArea,
        @Nullable ScreenRectangle bounds
) implements GuiElementRenderState {
    private static final int SEGMENTS = 10;

    SpellWheelWedgeRenderState(RenderPipeline pipeline, TextureSetup textureSetup, Matrix3x2fc pose,
                                float centerX, float centerY, float radius, float startAngle, float endAngle,
                                int color, @Nullable ScreenRectangle scissorArea) {
        this(pipeline, textureSetup, pose, centerX, centerY, radius, startAngle, endAngle, color, scissorArea,
                bounds(centerX, centerY, radius, pose, scissorArea));
    }

    private static ScreenRectangle bounds(float centerX, float centerY, float radius, Matrix3x2fc pose, @Nullable ScreenRectangle scissorArea) {
        ScreenRectangle full = new ScreenRectangle((int) (centerX - radius), (int) (centerY - radius), (int) (radius * 2), (int) (radius * 2))
                .transformMaxBounds(pose);
        return scissorArea != null ? scissorArea.intersection(full) : full;
    }

    /** Angles are clockwise from straight up: x = centerX + r*sin(a), y = centerY - r*cos(a). */
    private float x(float angle) {
        return centerX + radius * Mth.sin(angle);
    }

    private float y(float angle) {
        return centerY - radius * Mth.cos(angle);
    }

    @Override
    public void buildVertices(VertexConsumer vertexConsumer) {
        float step = (endAngle - startAngle) / SEGMENTS;
        for (int i = 0; i < SEGMENTS; i++) {
            float a0 = startAngle + step * i;
            float a1 = startAngle + step * (i + 1);
            // Order matters: (center, a1, a0) is the winding the GUI pipeline's backface culling
            // expects (matching vanilla's own rectangle quads) - (center, a0, a1) is wound the
            // other way and gets silently culled, which is why the wedges never rendered at all.
            vertexConsumer.addVertexWith2DPose(pose, centerX, centerY).setColor(color);
            vertexConsumer.addVertexWith2DPose(pose, x(a1), y(a1)).setColor(color);
            vertexConsumer.addVertexWith2DPose(pose, x(a0), y(a0)).setColor(color);
            vertexConsumer.addVertexWith2DPose(pose, centerX, centerY).setColor(color);
        }
    }
}
