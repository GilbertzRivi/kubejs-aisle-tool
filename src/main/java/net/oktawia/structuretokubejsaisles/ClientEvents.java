package net.oktawia.structuretokubejsaisles;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.oktawia.structuretokubejsaisles.hc.hcCompileMerged1;

/** Client-only hooks: the info text above the hotbar and the selection box in the world. Both draw in Hot Chocolate. */
public final class ClientEvents {
    private ClientEvents() {}

    @EventBusSubscriber(modid = ModContent.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class ModBus {
        @SubscribeEvent
        public static void registerLayers(RegisterGuiLayersEvent event) {
            event.registerAbove(VanillaGuiLayers.HOTBAR,
                    ResourceLocation.fromNamespaceAndPath(ModContent.MOD_ID, "copy_tool_info"),
                    (graphics, deltaTracker) -> hcCompileMerged1.render_hud(graphics));
        }
    }

    @EventBusSubscriber(modid = ModContent.MOD_ID, value = Dist.CLIENT)
    public static final class GameBus {
        @SubscribeEvent
        public static void renderLevel(RenderLevelStageEvent event) {
            if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
                hcCompileMerged1.render_selection(event.getPoseStack());
            }
        }
    }
}
