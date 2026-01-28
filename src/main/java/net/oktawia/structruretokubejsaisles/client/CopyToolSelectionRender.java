package net.oktawia.structruretokubejsaisles.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.oktawia.structruretokubejsaisles.Structruretokubejsaisles;
import net.oktawia.structruretokubejsaisles.items.CopyToolItem;

@Mod.EventBusSubscriber(modid = Structruretokubejsaisles.MODID, value = Dist.CLIENT)
public class CopyToolSelectionRender {

    @SubscribeEvent
    public static void onRenderWorld(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        ItemStack stack = mc.player.getMainHandItem();
        if (!(stack.getItem() instanceof CopyToolItem)) return;

        BlockPos p1 = CopyToolItem.getPosFromStack(stack, "copytool_pos1");
        BlockPos p2 = CopyToolItem.getPosFromStack(stack, "copytool_pos2");
        if (p1 == null && p2 == null) return;

        PoseStack pose = event.getPoseStack();

        if (p1 != null && p2 != null) {
            BlockPos min = new BlockPos(
                    Math.min(p1.getX(), p2.getX()),
                    Math.min(p1.getY(), p2.getY()),
                    Math.min(p1.getZ(), p2.getZ())
            );
            BlockPos max = new BlockPos(
                    Math.max(p1.getX(), p2.getX()),
                    Math.max(p1.getY(), p2.getY()),
                    Math.max(p1.getZ(), p2.getZ())
            );

            // inclusive max -> AABB maxExclusive
            RenderUtil.drawSelectionBox(
                    pose, event, min, max.offset(1, 1, 1)
            );
        } else {
            BlockPos single = (p1 != null) ? p1 : p2;
            RenderUtil.drawSelectionBox(
                    pose, event, single, single.offset(1, 1, 1)
            );
        }
    }
}
