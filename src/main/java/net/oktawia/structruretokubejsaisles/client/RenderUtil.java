package net.oktawia.structruretokubejsaisles.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.client.event.RenderLevelStageEvent;

public class RenderUtil {

    public static void drawSelectionBox(
            PoseStack pose,
            RenderLevelStageEvent event,
            BlockPos min,
            BlockPos maxExclusive
    ) {
        Minecraft mc = Minecraft.getInstance();

        double camX = mc.gameRenderer.getMainCamera().getPosition().x;
        double camY = mc.gameRenderer.getMainCamera().getPosition().y;
        double camZ = mc.gameRenderer.getMainCamera().getPosition().z;

        AABB box = new AABB(
                min.getX(), min.getY(), min.getZ(),
                maxExclusive.getX(), maxExclusive.getY(), maxExclusive.getZ()
        ).move(-camX, -camY, -camZ);

        VertexConsumer buffer = mc.renderBuffers()
                .bufferSource()
                .getBuffer(RenderType.lines());

        LevelRenderer.renderLineBox(
                pose,
                buffer,
                box,
                0.0f, 1.0f, 0.0f, 1.0f
        );
    }
}
