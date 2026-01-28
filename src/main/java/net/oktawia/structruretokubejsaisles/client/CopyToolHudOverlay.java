package net.oktawia.structruretokubejsaisles.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.oktawia.structruretokubejsaisles.Structruretokubejsaisles;
import net.oktawia.structruretokubejsaisles.items.CopyToolItem;

@Mod.EventBusSubscriber(modid = Structruretokubejsaisles.MODID, value = Dist.CLIENT)
public class CopyToolHudOverlay {

    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiOverlayEvent.Post event) {
        if (event.getOverlay() != VanillaGuiOverlay.HOTBAR.type()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        ItemStack stack = mc.player.getMainHandItem();
        if (!(stack.getItem() instanceof CopyToolItem)) return;

        BlockPos p1 = CopyToolItem.getPosFromStack(stack, "copytool_pos1");
        BlockPos p2 = CopyToolItem.getPosFromStack(stack, "copytool_pos2");

        int screenW = mc.getWindow().getGuiScaledWidth();
        int screenH = mc.getWindow().getGuiScaledHeight();

        int x = screenW / 2 - 90;
        int y = screenH - 60;

        var gg = event.getGuiGraphics();

        if (p1 == null && p2 == null) {
            String line1 = "CopyTool: set corners";
            String line2 = "(Sneak + RightClick = Corner 1)";

            int centerX = screenW / 2;

            int x1 = centerX - mc.font.width(line1) / 2;

            gg.drawString(mc.font, line1, x1, y, 0xFFFFFF, true);
            gg.drawString(mc.font, line2, centerX - mc.font.width(line2) / 2, y + 10, 0xAAAAAA, true);

            return;
        }


        String c1 = (p1 == null)
                ? "Corner1: -"
                : "Corner1: " + p1.getX() + " " + p1.getY() + " " + p1.getZ();

        String c2 = (p2 == null)
                ? "Corner2: -"
                : "Corner2: " + p2.getX() + " " + p2.getY() + " " + p2.getZ();

        gg.drawString(mc.font, c1, x, y, 0xFFFFFF, true);
        gg.drawString(mc.font, c2, x, y + 10, 0xFFFFFF, true);

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

            int sizeX = max.getX() - min.getX() + 1;
            int sizeY = max.getY() - min.getY() + 1;
            int sizeZ = max.getZ() - min.getZ() + 1;

            long blocks = (long) sizeX * (long) sizeY * (long) sizeZ;

            gg.drawString(mc.font,
                    "Size: " + sizeX + "x" + sizeY + "x" + sizeZ + "   Blocks: " + blocks,
                    x, y + 22, 0xFFFFFF, true);
        }
    }
}
