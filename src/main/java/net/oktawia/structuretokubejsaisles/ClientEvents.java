package net.oktawia.structuretokubejsaisles;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.network.PacketDistributor;
import net.oktawia.structuretokubejsaisles.hc.hcCompileMerged1;
import org.lwjgl.glfw.GLFW;

/** Client-only hooks: the info text above the hotbar, the selection box in the world, and the two Copy Tool keys. */
public final class ClientEvents {
    private ClientEvents() {}

    private static final String CATEGORY = "key.categories." + ModContent.MOD_ID;

    /** Switches right click between marking corners and marking the controller. */
    public static final KeyMapping MODE_KEY = new KeyMapping("key." + ModContent.MOD_ID + ".mode",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_LEFT_BRACKET, CATEGORY);

    /** Forgets both corners and the controller. */
    public static final KeyMapping CLEAR_KEY = new KeyMapping("key." + ModContent.MOD_ID + ".clear",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_BRACKET, CATEGORY);

    @EventBusSubscriber(modid = ModContent.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class ModBus {
        @SubscribeEvent
        public static void registerLayers(RegisterGuiLayersEvent event) {
            event.registerAbove(VanillaGuiLayers.HOTBAR,
                    ResourceLocation.fromNamespaceAndPath(ModContent.MOD_ID, "copy_tool_info"),
                    (graphics, deltaTracker) -> hcCompileMerged1.render_hud(graphics));
        }

        @SubscribeEvent
        public static void registerKeys(RegisterKeyMappingsEvent event) {
            event.register(MODE_KEY);
            event.register(CLEAR_KEY);
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

        @SubscribeEvent
        public static void clientTick(ClientTickEvent.Post event) {
            Minecraft mc = Minecraft.getInstance();
            boolean holding = mc.player != null && mc.player.getMainHandItem().is(ModContent.COPY_TOOL.get());
            while (MODE_KEY.consumeClick()) {
                if (holding) PacketDistributor.sendToServer(new ToolActionPayload(ToolActionPayload.TOGGLE_MODE));
            }
            while (CLEAR_KEY.consumeClick()) {
                if (holding) PacketDistributor.sendToServer(new ToolActionPayload(ToolActionPayload.CLEAR));
            }
        }
    }
}
